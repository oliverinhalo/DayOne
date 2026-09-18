package com.dayone.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.dayone.app.data.AppSettings
import com.dayone.app.data.GhostMode
import com.dayone.app.data.GridMode
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Everything drawn on top of the live camera preview.
 *
 * The key idea is the *frame box*: the saved photo is a square crop, so the overlay
 * draws that exact square, dims everything outside it, and places the reference photo
 * inside it at the same scale the saved image will have. Lining yourself up against the
 * ghost now means lining up against what actually gets stored, instead of against a
 * loose image floating over a full-height preview.
 */
@Composable
fun CaptureOverlay(
    reference: ImageBitmap?,
    outline: ImageBitmap?,
    settings: AppSettings,
    headFraction: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val frame = frameRect(size)

        if (settings.showFrameBox) drawOutsideScrim(frame)

        val ghost = when (settings.ghostMode) {
            GhostMode.OFF -> null
            GhostMode.OUTLINE -> outline ?: reference
            else -> reference
        }

        if (ghost != null) {
            drawGhost(
                image = ghost,
                frame = frame,
                mode = settings.ghostMode,
                alpha = settings.ghostAlpha.coerceIn(0f, 1f),
                scale = settings.ghostScale.coerceIn(0.4f, 3f),
                offsetX = settings.ghostOffsetX,
                offsetY = settings.ghostOffsetY,
                flip = settings.ghostFlip
            )
        }

        if (settings.gridMode != GridMode.NONE) drawGrid(frame, settings.gridMode)
        if (settings.showFaceGuide) drawFaceGuide(frame, headFraction)
        if (settings.showFrameBox) drawFrameCorners(frame)
    }
}

/** The square of preview that ends up in the saved photo. */
private fun frameRect(size: Size): Rect {
    val side = min(size.width, size.height)
    // Sit the square slightly above centre, matching the head-room bias the crop uses.
    val centerY = (size.height * 0.44f).coerceIn(side / 2f, size.height - side / 2f)
    val left = (size.width - side) / 2f
    val top = centerY - side / 2f
    return Rect(left, top, left + side, top + side)
}

private fun DrawScope.drawOutsideScrim(frame: Rect) {
    val scrim = Color.Black.copy(alpha = 0.42f)
    if (frame.top > 0f) {
        drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, frame.top))
    }
    if (frame.bottom < size.height) {
        drawRect(scrim, topLeft = Offset(0f, frame.bottom), size = Size(size.width, size.height - frame.bottom))
    }
    if (frame.left > 0f) {
        drawRect(scrim, topLeft = Offset(0f, frame.top), size = Size(frame.left, frame.height))
    }
    if (frame.right < size.width) {
        drawRect(
            scrim,
            topLeft = Offset(frame.right, frame.top),
            size = Size(size.width - frame.right, frame.height)
        )
    }
}

private fun DrawScope.drawGhost(
    image: ImageBitmap,
    frame: Rect,
    mode: GhostMode,
    alpha: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    flip: Boolean
) {
    val bands = 8

    clipRect(frame.left, frame.top, frame.right, frame.bottom) {
        withTransform({
            if (flip) scale(-1f, 1f, pivot = frame.center)
            translate(offsetX * frame.width, offsetY * frame.height)
            scale(scale, scale, pivot = frame.center)
        }) {
            when (mode) {
                GhostMode.SPLIT -> {
                    // Reference on the left, live camera on the right, hard edge down the middle.
                    clipRect(frame.left, frame.top, frame.center.x, frame.bottom) {
                        drawImageInFrame(image, frame, 1f)
                    }
                }
                GhostMode.STRIPES -> {
                    val bandWidth = frame.width / bands
                    for (i in 0 until bands step 2) {
                        val left = frame.left + i * bandWidth
                        clipRect(left, frame.top, left + bandWidth, frame.bottom) {
                            drawImageInFrame(image, frame, 1f)
                        }
                    }
                }
                else -> drawImageInFrame(image, frame, alpha)
            }
        }
    }

    if (mode == GhostMode.SPLIT) {
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(frame.center.x, frame.top),
            end = Offset(frame.center.x, frame.bottom),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawImageInFrame(image: ImageBitmap, frame: Rect, alpha: Float) {
    drawImage(
        image = image,
        dstOffset = IntOffset(frame.left.roundToInt(), frame.top.roundToInt()),
        dstSize = IntSize(frame.width.roundToInt(), frame.height.roundToInt()),
        alpha = alpha
    )
}

private fun DrawScope.drawGrid(frame: Rect, mode: GridMode) {
    val line = Color.White.copy(alpha = 0.28f)
    val stroke = 1.2f

    fun vertical(fraction: Float) = drawLine(
        line,
        Offset(frame.left + frame.width * fraction, frame.top),
        Offset(frame.left + frame.width * fraction, frame.bottom),
        stroke
    )

    fun horizontal(fraction: Float) = drawLine(
        line,
        Offset(frame.left, frame.top + frame.height * fraction),
        Offset(frame.right, frame.top + frame.height * fraction),
        stroke
    )

    when (mode) {
        GridMode.THIRDS -> {
            vertical(1f / 3f); vertical(2f / 3f)
            horizontal(1f / 3f); horizontal(2f / 3f)
        }
        GridMode.GRID -> {
            for (i in 1 until 6) {
                vertical(i / 6f); horizontal(i / 6f)
            }
        }
        GridMode.CENTER -> {
            val arm = frame.width * 0.045f
            val center = frame.center
            drawLine(Color.White.copy(alpha = 0.6f), Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), 2f)
            drawLine(Color.White.copy(alpha = 0.6f), Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), 2f)
        }
        GridMode.NONE -> Unit
    }
}

/** Dashed oval showing where the head should sit - the same geometry the crop targets. */
private fun DrawScope.drawFaceGuide(frame: Rect, headFraction: Float) {
    val ovalHeight = frame.height * headFraction.coerceIn(0.15f, 0.9f)
    val ovalWidth = ovalHeight * 0.76f
    val centerX = frame.center.x
    val centerY = frame.top + frame.height * 0.42f

    drawOval(
        color = Color.White.copy(alpha = 0.75f),
        topLeft = Offset(centerX - ovalWidth / 2f, centerY - ovalHeight / 2f),
        size = Size(ovalWidth, ovalHeight),
        style = Stroke(
            width = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f), 0f)
        )
    )
    // Eye line - the single most useful alignment cue day to day.
    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = Offset(centerX - ovalWidth * 0.62f, centerY - ovalHeight * 0.1f),
        end = Offset(centerX + ovalWidth * 0.62f, centerY - ovalHeight * 0.1f),
        strokeWidth = 1.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f)
    )
}

private fun DrawScope.drawFrameCorners(frame: Rect) {
    val corner = frame.width * 0.08f
    val color = Color.White.copy(alpha = 0.9f)
    val stroke = 3f

    // Top-left
    drawLine(color, Offset(frame.left, frame.top), Offset(frame.left + corner, frame.top), stroke)
    drawLine(color, Offset(frame.left, frame.top), Offset(frame.left, frame.top + corner), stroke)
    // Top-right
    drawLine(color, Offset(frame.right, frame.top), Offset(frame.right - corner, frame.top), stroke)
    drawLine(color, Offset(frame.right, frame.top), Offset(frame.right, frame.top + corner), stroke)
    // Bottom-left
    drawLine(color, Offset(frame.left, frame.bottom), Offset(frame.left + corner, frame.bottom), stroke)
    drawLine(color, Offset(frame.left, frame.bottom), Offset(frame.left, frame.bottom - corner), stroke)
    // Bottom-right
    drawLine(color, Offset(frame.right, frame.bottom), Offset(frame.right - corner, frame.bottom), stroke)
    drawLine(color, Offset(frame.right, frame.bottom), Offset(frame.right, frame.bottom - corner), stroke)
}
