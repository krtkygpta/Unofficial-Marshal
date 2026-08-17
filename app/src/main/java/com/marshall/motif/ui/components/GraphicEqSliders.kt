package com.marshall.motif.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private const val BAND_COUNT = 5
private const val MIN_DB = -6
private const val MAX_DB = 6

/** Same response plot as Basic EQ; the five dots are the ±6 dB handles. */
@Composable
fun GraphicEqSliders(
    values: List<Int>,
    onCommit: (List<Int>) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var local by remember { mutableStateOf(padBands(values)) }
    var dragging by remember { mutableStateOf(false) }
    var active by remember { mutableIntStateOf(-1) }
    val latestLocal = rememberUpdatedState(local)
    val latestCommit = rememberUpdatedState(onCommit)
    LaunchedEffect(values) {
        if (!dragging) local = padBands(values)
    }

    val shape = FloatArray(BAND_COUNT) { i ->
        local.getOrElse(i) { 0 }.coerceIn(MIN_DB, MAX_DB) / MAX_DB.toFloat()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp),
        ) {
            EqVisual(
                shape = shape,
                accent = MaterialTheme.colorScheme.primary,
                highlightIndex = active,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 18.dp)
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        val padH = 18.dp.toPx()
                        val padV = 14.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val plotW = size.width.toFloat()
                            val plotH = size.height.toFloat()
                            val left = padH
                            val right = plotW - padH
                            val top = padV
                            val bottom = plotH - padV
                            val midY = (top + bottom) / 2f
                            val amp = (bottom - top) * 0.42f
                            if (amp <= 0f || right <= left) return@awaitEachGesture

                            fun bandAt(x: Float): Int {
                                var best = 0
                                var bestDist = Float.MAX_VALUE
                                for (i in 0 until BAND_COUNT) {
                                    val bx = left + (right - left) * (i / (BAND_COUNT - 1f))
                                    val d = abs(x - bx)
                                    if (d < bestDist) {
                                        bestDist = d
                                        best = i
                                    }
                                }
                                return best
                            }

                            fun dbAt(y: Float): Int {
                                val v = ((midY - y) / amp).coerceIn(-1f, 1f)
                                return (v * MAX_DB).roundToInt().coerceIn(MIN_DB, MAX_DB)
                            }

                            val band = bandAt(down.position.x)
                            dragging = true
                            active = band
                            fun apply(y: Float) {
                                val next = latestLocal.value.toMutableList()
                                next[band] = dbAt(y)
                                local = next
                            }
                            apply(down.position.y)
                            drag(down.id) { change ->
                                change.consume()
                                apply(change.position.y)
                            }
                            dragging = false
                            active = -1
                            latestCommit.value(latestLocal.value)
                        }
                    },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            local.forEachIndexed { index, db ->
                Text(
                    text = if (db > 0) "+$db" else "$db",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (index == active) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (index == active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

private fun padBands(values: List<Int>): List<Int> =
    (0 until BAND_COUNT).map { values.getOrElse(it) { 0 }.coerceIn(MIN_DB, MAX_DB) }
