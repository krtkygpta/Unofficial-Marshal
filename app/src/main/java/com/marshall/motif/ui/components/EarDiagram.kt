package com.marshall.motif.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * A stylised head-and-ears diagram used on the Controls screen.
 * The selected ear is highlighted in the accent colour.
 */
@Composable
fun EarDiagram(
    leftSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val leftGlow by animateFloatAsState(
        targetValue = if (leftSelected) 1f else 0f,
        animationSpec = tween(300),
        label = "leftGlow",
    )
    val rightGlow by animateFloatAsState(
        targetValue = if (leftSelected) 0f else 1f,
        animationSpec = tween(300),
        label = "rightGlow",
    )

    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val head = Path().apply {
                moveTo(w * 0.5f, h * 0.06f)
                cubicTo(w * 0.22f, h * 0.06f, w * 0.12f, h * 0.28f, w * 0.16f, h * 0.52f)
                cubicTo(w * 0.10f, h * 0.60f, w * 0.10f, h * 0.66f, w * 0.16f, h * 0.66f)
                cubicTo(w * 0.16f, h * 0.74f, w * 0.22f, h * 0.78f, w * 0.34f, h * 0.80f)
                cubicTo(w * 0.40f, h * 0.90f, w * 0.44f, h * 0.94f, w * 0.5f, h * 0.94f)
                cubicTo(w * 0.56f, h * 0.94f, w * 0.60f, h * 0.90f, w * 0.66f, h * 0.80f)
                cubicTo(w * 0.78f, h * 0.78f, w * 0.84f, h * 0.74f, w * 0.84f, h * 0.66f)
                cubicTo(w * 0.90f, h * 0.66f, w * 0.90f, h * 0.60f, w * 0.84f, h * 0.52f)
                cubicTo(w * 0.88f, h * 0.28f, w * 0.78f, h * 0.06f, w * 0.5f, h * 0.06f)
                close()
            }
            drawPath(head, brush = Brush.verticalGradient(listOf(Color(0xFF1C1C21), Color(0xFF101014))))

            // Ears (two small ellipses on the sides)
            drawEar(w, h, centerSide = 0.10f, glow = leftGlow, left = true)
            drawEar(w, h, centerSide = 0.90f, glow = rightGlow, left = false)

            // Connecting stems from each bud to the head
            drawLine(
                color = Color(0xFF2A2A30),
                start = Offset(w * 0.20f, h * 0.52f),
                end = Offset(w * 0.32f, h * 0.52f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFF2A2A30),
                start = Offset(w * 0.80f, h * 0.52f),
                end = Offset(w * 0.68f, h * 0.52f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEar(
    w: Float,
    h: Float,
    centerSide: Float,
    glow: Float,
    left: Boolean,
) {
    val cx = w * centerSide
    val cy = h * 0.52f
    val rx = w * 0.065f
    val ry = h * 0.13f

    val accent = Color(0xFFE6C36A)
    val track = Color(0xFF2E2E34)

    if (glow > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.30f * glow), Color.Transparent),
            ),
            radius = rx * 2.2f,
            center = Offset(cx, cy),
        )
    }

    drawOval(
        color = track,
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f),
    )

    drawOval(
        color = lerp(track, accent, glow * 0.6f),
        topLeft = Offset(cx - rx * 0.45f, cy - ry * 0.45f),
        size = Size(rx * 0.9f, ry * 0.9f),
    )

    // Tip
    drawCircle(
        color = lerp(Color(0xFF1A1A1E), accent, glow),
        radius = rx * 0.34f,
        center = Offset(if (left) cx + rx * 0.9f else cx - rx * 0.9f, cy),
    )
}
