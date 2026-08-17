package com.marshall.motif.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BAND_LABELS = listOf("160", "400", "1k", "2.5k", "6k")

/**
 * Frequency-response style plot. [shape] is −1..1 per Motif band.
 */
@Composable
fun EqVisual(
    shape: FloatArray,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    @Suppress("UNUSED_PARAMETER") track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    @Suppress("UNUSED_PARAMETER") animate: Boolean = false,
    showLabels: Boolean = true,
    highlightIndex: Int = -1,
) {
    val zero = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = labelColor,
        fontSize = 10.sp,
    )

    Box(modifier) {
        Canvas(Modifier.fillMaxSize().padding(bottom = if (showLabels) 18.dp else 0.dp)) {
            val padH = 18.dp.toPx()
            val padV = 14.dp.toPx()
            val left = padH
            val right = size.width - padH
            val top = padV
            val bottom = size.height - padV
            val midY = (top + bottom) / 2f
            val amp = (bottom - top) * 0.42f
            val n = 5
            val pts = List(n) { i ->
                val x = left + (right - left) * (i / (n - 1f))
                val v = shape.getOrElse(i) { 0f }.coerceIn(-1f, 1f)
                Offset(x, midY - v * amp)
            }

            drawLine(zero, Offset(left, midY), Offset(right, midY), 1.2.dp.toPx(), StrokeCap.Round)

            val stroke = smoothPath(pts)
            val fill = Path().apply {
                addPath(stroke)
                lineTo(pts.last().x, midY)
                lineTo(pts.first().x, midY)
                close()
            }
            drawPath(
                fill,
                brush = Brush.verticalGradient(
                    0f to accent.copy(alpha = 0.32f),
                    1f to accent.copy(alpha = 0.02f),
                    startY = top,
                    endY = bottom,
                ),
                style = Fill,
            )
            drawPath(
                stroke,
                color = accent,
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            pts.forEachIndexed { i, p ->
                val hot = i == highlightIndex
                drawCircle(accent.copy(alpha = if (hot) 0.30f else 0.22f), (if (hot) 11.dp else 7.dp).toPx(), p)
                drawCircle(accent, (if (hot) 5.dp else 3.4.dp).toPx(), p)
            }
        }
        if (showLabels) {
            Canvas(Modifier.fillMaxSize()) {
                val padH = 18.dp.toPx()
                val n = 5
                for (i in 0 until n) {
                    val x = padH + (size.width - 2 * padH) * (i / (n - 1f))
                    val layout = measurer.measure(BAND_LABELS[i], labelStyle)
                    drawText(
                        layout,
                        topLeft = Offset(x - layout.size.width / 2f, size.height - layout.size.height),
                    )
                }
            }
        }
    }
}

@Composable
fun EqSparkline(
    shape: FloatArray,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val zero = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    Canvas(modifier) {
        val left = 2.dp.toPx()
        val right = size.width - 2.dp.toPx()
        val midY = size.height / 2f
        val amp = size.height * 0.38f
        val n = 5
        val pts = List(n) { i ->
            val x = left + (right - left) * (i / (n - 1f))
            val v = shape.getOrElse(i) { 0f }.coerceIn(-1f, 1f)
            Offset(x, midY - v * amp)
        }
        drawLine(zero, Offset(left, midY), Offset(right, midY), 1.dp.toPx())
        drawPath(
            smoothPath(pts),
            color = accent,
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private fun DrawScope.smoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts.first().x, pts.first().y)
    if (pts.size == 1) return path
    for (i in 0 until pts.lastIndex) {
        val p0 = pts[i]
        val p1 = pts[i + 1]
        val dx = (p1.x - p0.x) * 0.45f
        path.cubicTo(p0.x + dx, p0.y, p1.x - dx, p1.y, p1.x, p1.y)
    }
    return path
}
