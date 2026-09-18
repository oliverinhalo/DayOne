package com.dayone.app.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Runs on-device ML Kit face detection on a captured photo, then produces a square,
 * face-centered crop (matching the head-alignment oval shown in the capture UI),
 * so every day's photo lines up the same way in the timeline/overlay/video.
 *
 * Entirely offline - ML Kit's bundled face-detection model runs on-device, no network call.
 */
object FaceCropper {

    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build()
        )
    }

    data class Result(val face: RectF?, val usedFaceCrop: Boolean)

    /**
     * @param outputSize final square output resolution in pixels
     * @param headFraction how much of the output height the head should occupy (0..1),
     *   matching the on-screen guide oval
     * @param autoCrop when false the frame is only squared off and scaled, never re-centred
     */
    fun processAndSave(
        srcFile: File,
        outFile: File,
        outputSize: Int = 1440,
        headFraction: Float = 0.42f,
        autoCrop: Boolean = true
    ): Result {
        val decoded = decodeSampledBitmap(srcFile, 2560)
        val rotated = applyExifTransform(decoded, srcFile)

        val faceRect = if (autoCrop) detectFaceSync(rotated) else null

        val cropRect: Rect = if (faceRect != null) {
            computeCenteredCropAroundFace(rotated.width, rotated.height, faceRect, headFraction)
        } else {
            // No face found (or auto-crop off) - fall back to a centered square crop so
            // overlay alignment still roughly works.
            centeredSquareFallback(rotated.width, rotated.height)
        }

        val left = cropRect.left.coerceIn(0, max(0, rotated.width - 1))
        val top = cropRect.top.coerceIn(0, max(0, rotated.height - 1))
        val width = cropRect.width().coerceIn(1, rotated.width - left)
        val height = cropRect.height().coerceIn(1, rotated.height - top)

        val cropped = Bitmap.createBitmap(rotated, left, top, width, height)
        val finalBmp = Bitmap.createScaledBitmap(cropped, outputSize, outputSize, true)

        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { fos ->
            finalBmp.compress(Bitmap.CompressFormat.JPEG, 94, fos)
        }

        if (cropped !== finalBmp) cropped.recycle()
        finalBmp.recycle()
        if (rotated !== decoded) decoded.recycle()
        rotated.recycle()

        return Result(faceRect, faceRect != null)
    }

    private fun detectFaceSync(bitmap: Bitmap): RectF? {
        return try {
            val input = InputImage.fromBitmap(bitmap, 0)
            val faces = Tasks.await(detector.process(input))
            val best = faces.maxByOrNull { it.boundingBox.width().toLong() * it.boundingBox.height() }
            best?.boundingBox?.let { RectF(it) }
        } catch (t: Throwable) {
            null
        }
    }

    private fun computeCenteredCropAroundFace(
        imgW: Int, imgH: Int, face: RectF, headFraction: Float
    ): Rect {
        // Desired crop height so that the face height occupies `headFraction` of it
        val faceH = face.height()
        val fraction = headFraction.coerceIn(0.15f, 0.9f)
        val desiredCropSize = (faceH / fraction).roundToInt().coerceAtLeast(1)
        val cropSize = min(desiredCropSize, min(imgW, imgH))

        val faceCenterX = face.centerX()
        // Bias slightly above face center so hair/shoulders fit nicely (passport style)
        val faceCenterY = face.centerY() - faceH * 0.08f

        var left = (faceCenterX - cropSize / 2f).roundToInt()
        var top = (faceCenterY - cropSize / 2f).roundToInt()
        left = left.coerceIn(0, max(0, imgW - cropSize))
        top = top.coerceIn(0, max(0, imgH - cropSize))

        return Rect(left, top, left + cropSize, top + cropSize)
    }

    private fun centeredSquareFallback(imgW: Int, imgH: Int): Rect {
        val size = min(imgW, imgH)
        val left = (imgW - size) / 2
        val top = (imgH - size) / 3 // bias up a bit, most selfies have headroom above
        return Rect(left, top, left + size, top + size)
    }

    private fun decodeSampledBitmap(file: File, maxDim: Int): Bitmap {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        var inSample = 1
        while (opts.outWidth / inSample > maxDim || opts.outHeight / inSample > maxDim) {
            inSample *= 2
        }
        val realOpts = BitmapFactory.Options().apply {
            inSampleSize = inSample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, realOpts)
            ?: throw IllegalStateException("Could not decode ${file.name}")
    }

    /**
     * Applies the full EXIF orientation, including the mirrored variants CameraX writes
     * for a front-camera shot when horizontal reversal is requested.
     */
    private fun applyExifTransform(bitmap: Bitmap, file: File): Bitmap {
        val orientation = runCatching {
            ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
