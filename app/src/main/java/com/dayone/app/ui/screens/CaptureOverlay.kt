package com.dayone.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale

/**
 * Draws two aids on top of the live camera preview:
 *  1) A translucent ghost of yesterday's photo, so you can line up your position exactly.
 *  2) A dashed oval "head zone" guide showing where your face should sit, matching
 *     what FaceCropper will crop to.
 */
@Composable
fun CaptureOverlay(
    previousPhoto: Bitmap?,
    ghostAlpha: Float,
    showGuideOval: Boolean,
    modifier: Modifier = Modifier,
    zoomScalar: Float = 1.0f
) {
    if (previousPhoto != null && ghostAlpha > 0f) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = previousPhoto.asImageBitmap(),
                contentDescription = "Previous day photo overlay",
                contentScale = ContentScale.Inside,
                modifier = Modifier.graphicsLayer(
                    scaleX = zoomScalar,
                    scaleY = zoomScalar
                ),
                alpha = ghostAlpha
            )
        }
    }

    if (showGuideOval) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val ovalWidth = w * 0.55f
            val ovalHeight = h * 0.40f
            val centerX = w / 2f
            // Slightly above vertical center, matching FaceCropper's headroom bias
            val centerY = h * 0.42f

            drawOval(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(centerX - ovalWidth / 2f, centerY - ovalHeight / 2f),
                size = androidx.compose.ui.geometry.Size(ovalWidth, ovalHeight),
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f), 0f)
                )
            )
            // Center crosshair to help align eye-line day over day
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(centerX - 14f, centerY),
                end = Offset(centerX + 14f, centerY),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(centerX, centerY - 14f),
                end = Offset(centerX, centerY + 14f),
                strokeWidth = 3f
            )
        }
    }
}
