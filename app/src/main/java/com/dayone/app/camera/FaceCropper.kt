package com.dayone.app.camera

import android.graphics.*
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

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
    )

    /**
     * @param outputSize final square output resolution in pixels (e.g. 1080)
     * @param headFraction how much of the output height the head should occupy (0..1),
     *   matching the on-screen guide oval
     * @return the crop that was applied, in the *original* image's coordinate space,
     *   plus the RectF of the detected face for reference/re-crop later.
     */
    fun processAndSave(srcFile: File, outFile: File, outputSize: Int = 1080, headFraction: Float = 0.42f): RectF? {
        val bitmap = decodeSampledBitmap(srcFile, 2048)
        val rotated = rotateIfNeeded(bitmap, srcFile)

        val faceRect = detectFaceSync(rotated)

        val cropRect: Rect = if (faceRect != null) {
            computeCenteredCropAroundFace(rotated.width, rotated.height, faceRect, headFraction)
        } else {
            // No face found - fall back to a centered square crop so overlay alignment
            // still roughly works; user can still see the guide oval live in the viewfinder.
            centeredSquareFallback(rotated.width, rotated.height)
        }

        val cropped = Bitmap.createBitmap(
            rotated,
            cropRect.left.coerceIn(0, rotated.width - 1),
            cropRect.top.coerceIn(0, rotated.height - 1),
            cropRect.width().coerceIn(1, rotated.width),
            cropRect.height().coerceIn(1, rotated.height)
        )
        val finalBmp = Bitmap.createScaledBitmap(cropped, outputSize, outputSize, true)

        FileOutputStream(outFile).use { fos ->
            finalBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }

        if (cropped !== finalBmp) cropped.recycle()
        finalBmp.recycle()
        rotated.recycle()

        return faceRect
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
        val desiredCropSize = (faceH / headFraction).roundToInt().coerceAtLeast(1)
        val cropSize = min(desiredCropSize, min(imgW, imgH))

        val faceCenterX = face.centerX()
        // Bias slightly above face center so hair/shoulders fit nicely (like a passport-style headshot)
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
        val realOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
        return BitmapFactory.decodeFile(file.absolutePath, realOpts)
            ?: throw IllegalStateException("Could not decode $file")
    }

    private fun rotateIfNeeded(bitmap: Bitmap, file: File): Bitmap {
        val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
