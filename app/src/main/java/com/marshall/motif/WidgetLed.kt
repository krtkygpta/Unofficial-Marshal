package com.marshall.motif

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * Nothing-style 5×7 LED percentages for the home-screen widget.
 * Renders Left · Case · Right — same order as the in-app battery row,
 * so the side digits sit under the matching bud.
 */
object WidgetLed {
    private const val COLS = 5
    private const val ROWS = 7

    private val glyphs: Map<Char, IntArray> = mapOf(
        '0' to intArrayOf(0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110),
        '1' to intArrayOf(0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
        '2' to intArrayOf(0b01110, 0b10001, 0b00001, 0b00110, 0b01000, 0b10000, 0b11111),
        '3' to intArrayOf(0b01110, 0b10001, 0b00001, 0b00110, 0b00001, 0b10001, 0b01110),
        '4' to intArrayOf(0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010),
        '5' to intArrayOf(0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110),
        '6' to intArrayOf(0b01110, 0b10000, 0b11110, 0b10001, 0b10001, 0b10001, 0b01110),
        '7' to intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000),
        '8' to intArrayOf(0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110),
        '9' to intArrayOf(0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00001, 0b01110),
        '%' to intArrayOf(0b11001, 0b11010, 0b00100, 0b01000, 0b00100, 0b01011, 0b10011),
        '-' to intArrayOf(0b00000, 0b00000, 0b00000, 0b01110, 0b00000, 0b00000, 0b00000),
    )

    fun drawPercents(
        left: Int,
        right: Int,
        case: Int,
        density: Float,
        onSurface: Int = 0xFFF2F2F2.toInt(),
    ): Bitmap {
        val values = listOf(format(left), format(case), format(right))
        val cell = (2.15f * density).coerceAtLeast(2f)
        val gap = cell * 0.55f
        val charAdvance = COLS * cell + gap
        val columnWidth = 4 * charAdvance
        val padX = cell * 1.6f
        val padY = cell * 1.1f
        val width = (padX * 2f + columnWidth * 3f).toInt().coerceAtLeast(8)
        val height = (padY * 2f + ROWS * cell).toInt().coerceAtLeast(8)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurface
            style = Paint.Style.FILL
        }
        val radius = cell * 0.36f

        values.forEachIndexed { index, text ->
            val blockLeft = padX + index * columnWidth
            val textWidth = text.length * charAdvance - gap
            val startX = blockLeft + (columnWidth - textWidth) / 2f
            drawText(canvas, paint, text, startX, padY, cell, gap, radius)
        }
        return bitmap
    }

    private fun format(value: Int): String = if (value in 0..100) "$value%" else "--"

    private fun drawText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        originX: Float,
        originY: Float,
        cell: Float,
        gap: Float,
        radius: Float,
    ) {
        text.forEachIndexed { i, ch ->
            val glyph = glyphs[ch] ?: return@forEachIndexed
            val x0 = originX + i * (COLS * cell + gap)
            for (row in 0 until ROWS) {
                val bits = glyph[row]
                for (col in 0 until COLS) {
                    val on = (bits shr (COLS - 1 - col)) and 1 == 1
                    if (!on) continue
                    canvas.drawCircle(
                        x0 + col * cell + cell / 2f,
                        originY + row * cell + cell / 2f,
                        radius,
                        paint,
                    )
                }
            }
        }
    }
}
