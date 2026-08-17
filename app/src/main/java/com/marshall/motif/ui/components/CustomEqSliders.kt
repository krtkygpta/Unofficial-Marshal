package com.marshall.motif.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val EQ_BANDS = listOf("Bass", "Low mid", "Mid", "High mid", "Treble")

@Composable
fun CustomEqSliders(
    values: List<Int>,
    onValueChange: (Int, Int) -> Unit,
    onCommit: (List<Int>) -> Unit = {},
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var local by remember { mutableStateOf(values.take(5).let { it + List(5 - it.size) { 0 } }) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(values) {
        if (!dragging) {
            local = values.take(5).let { it + List(5 - it.size) { 0 } }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EQ_BANDS.forEachIndexed { index, band ->
            val value = local.getOrElse(index) { 0 }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = band,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.8f),
                )
                Slider(
                    value = value.toFloat(),
                    onValueChange = { next ->
                        dragging = true
                        val snapped = next.roundToInt().coerceIn(-6, 6)
                        local = local.toMutableList().apply { this[index] = snapped }
                        onValueChange(index, snapped)
                    },
                    onValueChangeFinished = {
                        dragging = false
                        onCommit(local)
                    },
                    valueRange = -6f..6f,
                    steps = 11,
                    enabled = enabled,
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        activeTickColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    ),
                )
                Text(
                    text = if (value > 0) "+${value} dB" else "$value dB",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
