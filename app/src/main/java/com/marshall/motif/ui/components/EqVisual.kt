package com.marshall.motif.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Static 5-band equaliser illustration for a sound profile.
 * `shape` values are in -1..1 (cut / boost) and map to bar heights.
 * No animation — the graphic is the profile itself.
 */
@Composable
fun EqVisual(
    shape: FloatArray,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    @Suppress("UNUSED_PARAMETER") animate: Boolean = false,
) {
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val zeroLine = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padH = 20.dp.toPx()
        val padV = 16.dp.toPx()
        val chartL = padH
        val chartR = w - padH
        val chartT = padV
        val chartB = h - padV
        val chartW = chartR - chartL
        val chartH = chartB - chartT
        val midY = chartT + chartH / 2f
        val bands = 5
        val bandW = chartW / bands

        // Center (flat) reference line
        drawLine(
            color = zeroLine,
            start = Offset(chartL, midY),
            end = Offset(chartR, midY),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Soft horizontal guides
        for (g in 1..2) {
            val yUp = midY - chartH * 0.22f * g
            val yDn = midY + chartH * 0.22f * g
            drawLine(grid, Offset(chartL, yUp), Offset(chartR, yUp), 1.dp.toPx())
            drawLine(grid, Offset(chartL, yDn), Offset(chartR, yDn), 1.dp.toPx())
        }

        // Smooth curve through band peaks
        val curve = Path()
        val samples = 48
        for (i in 0..samples) {
            val t = i / samples.toFloat()
            val x = chartL + t * chartW
            // Interpolate between band centers
            val pos = t * (bands - 1)
            val i0 = pos.toInt().coerceIn(0, bands - 1)
            val i1 = (i0 + 1).coerceAtMost(bands - 1)
            val f = pos - i0
            val v0 = shape.getOrElse(i0) { 0f }
            val v1 = shape.getOrElse(i1) { 0f }
            val v = v0 + (v1 - v0) * f
            val y = midY - v * chartH * 0.38f
            if (i == 0) curve.moveTo(x, y) else curve.lineTo(x, y)
        }
        drawPath(
            curve,
            color = accent.copy(alpha = 0.35f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )

        // Bars for each band (static profile)
        for (i in 0 until bands) {
            val v = shape.getOrElse(i) { 0f }.coerceIn(-1f, 1f)
            val mag = abs(v)
            val barH = (0.08f + mag * 0.72f) * chartH * 0.5f
            val cx = chartL + bandW * i + bandW * 0.5f
            val barWidth = bandW * 0.42f
            val top = if (v >= 0f) midY - barH else midY
            val height = barH.coerceAtLeast(4.dp.toPx())
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (v >= 0f) {
                        listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.45f))
                    } else {
                        listOf(accent.copy(alpha = 0.45f), accent.copy(alpha = 0.85f))
                    },
                ),
                topLeft = Offset(cx - barWidth / 2f, top),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
            // Band baseline tick
            drawCircle(
                color = accent.copy(alpha = 0.5f),
                radius = 2.dp.toPx(),
                center = Offset(cx, midY),
            )
        }
    }
}

/**
 * Small equalizer bars (optional mini widget).
 */
@Composable
fun MiniEqualizer(
    modifier: Modifier = Modifier,
    color: Color,
    active: Boolean,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barCount = 4
        val gap = w / (barCount * 2)
        val barWidth = gap
        val heights = floatArrayOf(0.35f, 0.7f, 0.5f, 0.85f)

        for (i in 0 until barCount) {
            val x = i * gap * 2
            val height = if (active) heights[i] * h else h * 0.35f
            drawRoundRect(
                color = color.copy(alpha = if (active) 1f else 0.3f),
                topLeft = Offset(x, (h - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
