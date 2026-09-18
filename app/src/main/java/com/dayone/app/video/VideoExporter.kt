package com.dayone.app.video

import android.graphics.*
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
import java.util.concurrent.TimeUnit

data class OverlayOptions(
    val showDayNumber: Boolean = true,
    val showDate: Boolean = true,
    val showAge: Boolean = true,
    val showYear: Boolean = true
)

/**
 * Renders a project's day-by-day photos into an MP4 timelapse using the platform's
 * MediaCodec H.264 encoder + MediaMuxer directly. No third-party video library and
 * no network access - everything happens on-device.
 */
class VideoExporter(
    private val width: Int = 1080,
    private val height: Int = 1080,
    private val framesPerPhoto: Int = 6,   // how many encoded frames each day's photo holds for, at fps below
    private val fps: Int = 24,
    private val bitRate: Int = 8_000_000
) {

    // Persisted across drainEncoder() calls - MediaMuxer.start() must only ever fire once.
    private var muxerStarted = false
    private var muxerTrackIndex = -1

    fun export(
        project: Project,
        entries: List<PhotoEntry>,
        options: OverlayOptions,
        outFile: File,
        onProgress: (Float) -> Unit = {}
    ) {
        if (entries.isEmpty()) return

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxerStarted = false
        muxerTrackIndex = -1
        val bufferInfo = MediaCodec.BufferInfo()

        val birthDate = project.birthDateMillis?.let {
            java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

        var frameIndex = 0L
        val totalFrames = entries.size.toLong() * framesPerPhoto

        entries.forEachIndexed { dayIdx, entry ->
            val bitmap = decodeAndFit(File(entry.filePath), width, height)
            val date = LocalDate.ofEpochDay(entry.dateEpochDay)
            val overlayBitmap = drawOverlay(
                bitmap, options,
                dayNumber = dayIdx + 1,
                date = date,
                dateText = date.format(dateFormatter),
                age = birthDate?.let { Period.between(it, date).years },
                year = date.year
            )

            repeat(framesPerPhoto) {
                val canvas = inputSurface.lockCanvas(null)
                canvas.drawBitmap(overlayBitmap, 0f, 0f, null)
                inputSurface.unlockCanvasAndPost(canvas)

                drainEncoder(encoder, muxer, bufferInfo, endOfStream = false)
                frameIndex++
                onProgress(frameIndex.toFloat() / totalFrames)
            }
            overlayBitmap.recycle()
            bitmap.recycle()
        }

        // signal end of stream and drain everything that's left
        encoder.signalEndOfInputStream()
        drainEncoder(encoder, muxer, bufferInfo, endOfStream = true)

        encoder.stop()
        encoder.release()
        inputSurface.release()
        if (muxerStarted) {
            muxer.stop()
        }
        muxer.release()
    }

    /**
     * Pulls all currently-available encoded buffers out of [encoder] and writes them to [muxer].
     * [muxerStarted]/[muxerTrackIndex] are instance fields so state correctly carries over
     * between the many calls made per-frame during export() - MediaMuxer.start() must only
     * ever be invoked once, the first time the encoder reports its output format.
     */
    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean
    ) {
        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TimeUnit.SECONDS.toMicros(2))
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

    private fun decodeAndFit(file: File, w: Int, h: Int): Bitmap {
        val bmp = BitmapFactory.decodeFile(file.absolutePath)
        return if (bmp.width == w && bmp.height == h) bmp
        else Bitmap.createScaledBitmap(bmp, w, h, true).also { if (it !== bmp) bmp.recycle() }
    }

    private fun drawOverlay(
        base: Bitmap,
        options: OverlayOptions,
        dayNumber: Int,
        date: LocalDate,
        dateText: String,
        age: Int?,
        year: Int
    ): Bitmap {
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        val textSize = out.width * 0.045f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            setShadowLayer(8f, 0f, 2f, Color.argb(200, 0, 0, 0))
            typeface = Typeface.DEFAULT_BOLD
        }

        // Bottom gradient scrim so text stays legible over any photo
        val scrimHeight = out.height * 0.22f
        val gradient = LinearGradient(
            0f, out.height - scrimHeight, 0f, out.height.toFloat(),
            Color.TRANSPARENT, Color.argb(160, 0, 0, 0), Shader.TileMode.CLAMP
        )
        val scrimPaint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, out.height - scrimHeight, out.width.toFloat(), out.height.toFloat(), scrimPaint)

        val lines = mutableListOf<String>()
        if (options.showDayNumber) lines.add("Day $dayNumber")
        if (options.showDate) lines.add(dateText)
        val secondLineParts = mutableListOf<String>()
        if (options.showAge && age != null) secondLineParts.add("Age $age")
        if (options.showYear) secondLineParts.add("$year")
        if (secondLineParts.isNotEmpty()) lines.add(secondLineParts.joinToString("  •  "))

        var baseline = out.height - scrimHeight * 0.35f
        // Draw from bottom line upward
        for (line in lines.reversed()) {
            canvas.drawText(line, out.width * 0.05f, baseline, paint)
            baseline -= textSize * 1.3f
        }

        return out
    }
}
