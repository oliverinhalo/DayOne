package com.dayone.app.video

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.dayone.app.data.db.PhotoEntry
import com.dayone.app.data.db.Project
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class OverlayOptions(
    val showDayNumber: Boolean = true,
    val showDate: Boolean = true,
    val showAge: Boolean = true,
    val showYear: Boolean = true,
    val showNote: Boolean = false,
    val showProjectName: Boolean = false,
    val showProgressBar: Boolean = true
)

class ExportCancelledException : RuntimeException("Export cancelled")

/**
 * Renders a project's day-by-day photos into an MP4 timelapse using the platform's
 * MediaCodec H.264 encoder, an OpenGL ES input surface and MediaMuxer. No third-party
 * video library and no network access - everything happens on-device.
 */
class VideoExporter(
    private val size: Int = 1080,
    private val fps: Int = 30,
    private val millisPerPhoto: Int = 300,
    private val bitRate: Int = 12_000_000,
    private val crossfade: Boolean = true
) {

    private var muxerStarted = false
    private var muxerTrackIndex = -1

    /**
     * @param isCancelled polled once per frame; a true reading throws ExportCancelledException
     * @return the finished file, or null when there was nothing to encode
     */
    fun export(
        project: Project,
        entries: List<PhotoEntry>,
        options: OverlayOptions,
        outFile: File,
        isCancelled: () -> Boolean = { false },
        onProgress: (Float) -> Unit = {}
    ): File? {
        if (entries.isEmpty()) return null

        // Encoders want even dimensions; 16-aligned is safest across chipsets.
        val dimension = (size / 16) * 16
        val framesPerPhoto = max(1, (fps * millisPerPhoto / 1000f).roundToInt())
        val fadeFrames =
            if (crossfade && framesPerPhoto >= 4) min(framesPerPhoto / 3, max(1, fps / 4)) else 0
        val totalFrames = entries.size.toLong() * framesPerPhoto

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, dimension, dimension).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var encoder: MediaCodec? = null
        var inputSurface: InputSurface? = null
        var renderer: BitmapRenderer? = null
        var muxer: MediaMuxer? = null
        var codecSurface: android.view.Surface? = null

        muxerStarted = false
        muxerTrackIndex = -1

        var composedCurrent: Bitmap? = null
        var composedNext: Bitmap? = null
        var blendScratch: Bitmap? = null

        try {
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codecSurface = encoder.createInputSurface()
            inputSurface = InputSurface(codecSurface)
            inputSurface.makeCurrent()
            renderer = BitmapRenderer().apply { setup() }
            encoder.start()

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()

            val birthDate = project.birthDateMillis?.let {
                java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            }

            var frameIndex = 0L
            composedCurrent = composeFrame(project, entries, 0, options, dimension, birthDate)

            entries.indices.forEach { photoIndex ->
                if (isCancelled()) throw ExportCancelledException()

                val isLast = photoIndex == entries.lastIndex
                if (!isLast && fadeFrames > 0) {
                    composedNext?.recycle()
                    composedNext = composeFrame(project, entries, photoIndex + 1, options, dimension, birthDate)
                }

                for (frame in 0 until framesPerPhoto) {
                    if (isCancelled()) throw ExportCancelledException()

                    val fadeStart = framesPerPhoto - fadeFrames
                    val next = composedNext
                    val toDraw: Bitmap = if (!isLast && fadeFrames > 0 && frame >= fadeStart && next != null) {
                        val progress = (frame - fadeStart + 1).toFloat() / (fadeFrames + 1)
                        val scratch = blendScratch
                            ?: Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888)
                                .also { blendScratch = it }
                        blend(composedCurrent!!, next, progress, scratch)
                        scratch
                    } else {
                        composedCurrent!!
                    }

                    renderer.draw(toDraw, dimension, dimension)
                    inputSurface.setPresentationTime(frameIndex * 1_000_000_000L / fps)
                    inputSurface.swapBuffers()

                    drainEncoder(encoder, muxer, bufferInfo, endOfStream = false)
                    frameIndex++
                    onProgress(frameIndex.toFloat() / totalFrames)
                }

                // The next photo becomes the current one; its bitmap is reused, not redecoded.
                if (!isLast) {
                    composedCurrent?.recycle()
                    composedCurrent = if (fadeFrames > 0) composedNext
                    else composeFrame(project, entries, photoIndex + 1, options, dimension, birthDate)
                    composedNext = null
                }
            }

            encoder.signalEndOfInputStream()
            drainEncoder(encoder, muxer, bufferInfo, endOfStream = true)
            return outFile
        } finally {
            composedCurrent?.recycle()
            composedNext?.recycle()
            blendScratch?.recycle()
            runCatching { renderer?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            runCatching { inputSurface?.release() }
            runCatching { codecSurface?.release() }
            runCatching { if (muxerStarted) muxer?.stop() }
            runCatching { muxer?.release() }
        }
    }

    /**
     * Pulls all currently-available encoded buffers out of [encoder] and writes them to
     * [muxer]. MediaMuxer.start() must only ever fire once, the first time the encoder
     * reports its output format, which is why that state lives on the instance.
     */
    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean
    ) {
        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return else continue
                }
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!muxerStarted) { "Encoder changed format twice" }
                    muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                outIndex >= 0 -> {
                    val encodedData = encoder.getOutputBuffer(outIndex)
                        ?: throw RuntimeException("Encoder output buffer $outIndex was null")
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        // Codec config is handed to the muxer via addTrack(), not written.
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(muxerTrackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    // ------------------------------------------------------------------ frame building

    private fun blend(from: Bitmap, to: Bitmap, progress: Float, into: Bitmap) {
        val canvas = Canvas(into)
        canvas.drawColor(Color.BLACK)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(from, 0f, 0f, paint)
        paint.alpha = (progress.coerceIn(0f, 1f) * 255).toInt()
        canvas.drawBitmap(to, 0f, 0f, paint)
    }

    /** One finished frame: the day's photo scaled to fill, with its overlays burned in. */
    private fun composeFrame(
        project: Project,
        entries: List<PhotoEntry>,
        index: Int,
        options: OverlayOptions,
        dimension: Int,
        birthDate: LocalDate?
    ): Bitmap {
        val entry = entries[index]
        val out = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)

        decodeFitted(File(entry.filePath), dimension)?.let { photo ->
            // Centre-crop to a square so mixed aspect ratios (imported photos) still fill.
            val scale = max(dimension.toFloat() / photo.width, dimension.toFloat() / photo.height)
            val drawWidth = photo.width * scale
            val drawHeight = photo.height * scale
            val left = (dimension - drawWidth) / 2f
            val top = (dimension - drawHeight) / 2f
            canvas.drawBitmap(
                photo,
                Rect(0, 0, photo.width, photo.height),
                RectF(left, top, left + drawWidth, top + drawHeight),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            photo.recycle()
        }

        drawOverlay(canvas, dimension, project, entry, index, entries.size, options, birthDate)
        return out
    }

    private fun decodeFitted(file: File, target: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }

    private fun drawOverlay(
        canvas: Canvas,
        dimension: Int,
        project: Project,
        entry: PhotoEntry,
        index: Int,
        total: Int,
        options: OverlayOptions,
        birthDate: LocalDate?
    ) {
        val date = LocalDate.ofEpochDay(entry.dateEpochDay)

        val lines = mutableListOf<String>()
        if (options.showProjectName) lines.add(project.name)
        if (options.showDayNumber) lines.add("Day ${index + 1}")
        if (options.showDate) lines.add(date.format(DATE_FORMAT))

        val secondary = mutableListOf<String>()
        if (options.showAge && birthDate != null) secondary.add("Age ${Period.between(birthDate, date).years}")
        if (options.showYear) secondary.add(date.year.toString())
        if (secondary.isNotEmpty()) lines.add(secondary.joinToString("   •   "))

        val note = entry.note?.takeIf { options.showNote && it.isNotBlank() }
        val hasContent = lines.isNotEmpty() || note != null || options.showProgressBar
        if (!hasContent) return

        // Bottom gradient scrim so text stays legible over any photo
        val scrimHeight = dimension * 0.28f
        val scrimPaint = Paint().apply {
            shader = LinearGradient(
                0f, dimension - scrimHeight, 0f, dimension.toFloat(),
                Color.TRANSPARENT, Color.argb(190, 0, 0, 0), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, dimension - scrimHeight, dimension.toFloat(), dimension.toFloat(), scrimPaint)

        val margin = dimension * 0.055f
        var baseline = dimension - margin

        if (options.showProgressBar) {
            val barHeight = dimension * 0.008f
            val barTop = dimension - margin * 0.55f - barHeight
            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 255, 255, 255) }
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = project.colorArgb }
            val radius = barHeight / 2f
            canvas.drawRoundRect(
                RectF(margin, barTop, dimension - margin, barTop + barHeight), radius, radius, trackPaint
            )
            val fraction = if (total <= 1) 1f else (index + 1).toFloat() / total
            canvas.drawRoundRect(
                RectF(margin, barTop, margin + (dimension - 2 * margin) * fraction, barTop + barHeight),
                radius, radius, fillPaint
            )
            baseline = barTop - dimension * 0.035f
        }

        if (note != null) {
            val notePaint = textPaint(dimension * 0.036f, Typeface.create(Typeface.DEFAULT, Typeface.ITALIC))
            notePaint.color = Color.argb(230, 255, 255, 255)
            canvas.drawText(ellipsize(note, notePaint, dimension - 2 * margin), margin, baseline, notePaint)
            baseline -= dimension * 0.055f
        }

        // Draw the stacked lines from the bottom up, biggest line last (visually first).
        val bodyPaint = textPaint(dimension * 0.042f, Typeface.DEFAULT_BOLD)
        val headlinePaint = textPaint(dimension * 0.062f, Typeface.DEFAULT_BOLD)

        lines.reversed().forEachIndexed { indexFromBottom, line ->
            val paint = if (indexFromBottom == lines.size - 1) headlinePaint else bodyPaint
            canvas.drawText(line, margin, baseline, paint)
            baseline -= paint.textSize * 1.35f
        }
    }

    private fun textPaint(textSize: Float, typeface: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        this.textSize = textSize
        this.typeface = typeface
        setShadowLayer(textSize * 0.25f, 0f, textSize * 0.06f, Color.argb(200, 0, 0, 0))
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "...") > maxWidth) end--
        return text.substring(0, end) + "..."
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    }
}
