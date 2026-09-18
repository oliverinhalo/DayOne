package com.dayone.app.camera

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.min

/**
 * Turns the reference photo into a transparent outline for the capture overlay.
 *
 * A translucent ghost is hard to align against once both images are busy - the two
 * pictures just smear together. Outlines of the head, shoulders and eye line sit on top
 * of the live preview without hiding it, so lining yesterday up with today is exact.
 *
 * A plain Sobel gradient over a downscaled grayscale copy is enough here and takes a
 * couple of milliseconds; the result is cached by the view model per reference photo.
 */
object EdgeMap {

    /**
     * @param strength 0f..1f - raises or lowers the gradient threshold for a line to show
     * @param color line colour (alpha is derived from edge strength)
     */
    fun build(source: Bitmap, maxDim: Int = 512, strength: Float = 0.5f, color: Int = Color.WHITE): Bitmap {
        val scale = min(1f, maxDim.toFloat() / maxOf(source.width, source.height))
        val w = (source.width * scale).toInt().coerceAtLeast(16)
        val h = (source.height * scale).toInt().coerceAtLeast(16)
        val small = Bitmap.createScaledBitmap(source, w, h, true)

        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        if (small !== source) small.recycle()

        // Grayscale (integer luma) once, so the Sobel pass is a pure array walk.
        val luma = IntArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            luma[i] = (((p shr 16 and 0xFF) * 77) + ((p shr 8 and 0xFF) * 150) + ((p and 0xFF) * 29)) shr 8
        }

        val out = IntArray(w * h)
        val threshold = (12 + (1f - strength.coerceIn(0f, 1f)) * 60).toInt()
        val baseColor = color and 0x00FFFFFF

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val tl = luma[i - w - 1]; val t = luma[i - w]; val tr = luma[i - w + 1]
                val l = luma[i - 1];                            val r = luma[i + 1]
                val bl = luma[i + w - 1]; val b = luma[i + w]; val br = luma[i + w + 1]

                val gx = (tr + 2 * r + br) - (tl + 2 * l + bl)
                val gy = (bl + 2 * b + br) - (tl + 2 * t + tr)
                val magnitude = abs(gx) + abs(gy)

                out[i] = if (magnitude > threshold) {
                    val alpha = ((magnitude - threshold) * 3).coerceIn(0, 255)
                    (alpha shl 24) or baseColor
                } else 0
            }
        }

        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }
}
