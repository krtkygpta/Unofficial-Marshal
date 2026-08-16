package com.marshall.motif.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NoiseAware
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marshall.motif.R
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ble.MarshallGatt
import com.marshall.motif.ble.TouchAction
import com.marshall.motif.ble.TouchMap
import com.marshall.motif.ui.components.AppTopBar
import com.marshall.motif.ui.components.GroupedList
import com.marshall.motif.ui.components.HintText
import com.marshall.motif.ui.components.ListDivider
import com.marshall.motif.ui.components.ScreenScaffold
import com.marshall.motif.ui.components.SectionBlock
import com.marshall.motif.ui.components.StableBottomSheet
import com.marshall.motif.ui.components.ToggleRow
import com.marshall.motif.ui.theme.Radius
import com.marshall.motif.ui.theme.Space

/** Editing one gesture mapping. */
private data class GestureEdit(
    val leftEar: Boolean,
    val gestureIndex: Int,
)

private data class ActionGroup(
    val title: String,
    val actions: List<TouchAction>,
)

private val ActionGroups = listOf(
    ActionGroup(
        title = "Playback",
        actions = listOf(
            TouchAction.PLAY_PAUSE,
            TouchAction.PLAY_CALL,
            TouchAction.NEXT,
            TouchAction.PREVIOUS,
            TouchAction.SPOTIFY,
        ),
    ),
    ActionGroup(
        title = "Volume",
        actions = listOf(
            TouchAction.VOLUME_UP,
            TouchAction.VOLUME_DOWN,
        ),
    ),
    ActionGroup(
        title = "Noise control",
        actions = listOf(
            TouchAction.ANC_ALL,
            TouchAction.ANC_TRA,
            TouchAction.ANC_OFF,
            TouchAction.ANC_OFF_TRA,
        ),
    ),
    ActionGroup(
        title = "Other",
        actions = listOf(
            TouchAction.ASSISTANT,
            TouchAction.NOTHING,
        ),
    ),
)

@Composable
fun ControlsScreen(
    ble: BleManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ble.state
    var leftSelected by rememberSaveable { mutableStateOf(true) }
    var editing by remember { mutableStateOf<GestureEdit?>(null) }
    val currentMap = if (leftSelected) state.touchLeft else state.touchRight
    val gesturesEnabled = state.touchEnabled && state.connected

    ScreenScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "Controls",
                onBack = onBack,
            )
        },
    ) {
        // Ear switcher — single segmented control
        EarSegmentedControl(
            leftSelected = leftSelected,
            onSelectLeft = { leftSelected = true },
            onSelectRight = { leftSelected = false },
        )

        if (state.has(MarshallGatt.TOUCH_LOCK) || !state.connected) {
            ToggleRow(
                title = "Touch controls",
                subtitle = "Enable the touch surfaces",
                checked = state.touchEnabled,
                onCheckedChange = ble::setTouchEnabled,
                icon = Icons.Rounded.TouchApp,
                enabled = state.connected,
            )
        }

        SectionBlock(
            title = if (leftSelected) "Left earbud gestures" else "Right earbud gestures",
        ) {
            GroupedList {
                TouchMap.GESTURES.forEachIndexed { index, gesture ->
                    val action = TouchAction.fromByte(currentMap[index])
                    GestureMappingRow(
                        gesture = gesture,
                        action = action,
                        enabled = gesturesEnabled,
                        onClick = {
                            editing = GestureEdit(
                                leftEar = leftSelected,
                                gestureIndex = index,
                            )
                        },
                    )
                    if (index < TouchMap.GESTURES.lastIndex) {
                        ListDivider()
                    }
                }
            }
            HintText(
                if (gesturesEnabled) {
                    "Tap a gesture to choose what it does."
                } else if (!state.connected) {
                    "Connect your earbuds to edit gestures."
                } else {
                    "Turn on touch controls to edit gestures."
                },
            )
        }
    }

    editing?.let { edit ->
        val map = if (edit.leftEar) state.touchLeft else state.touchRight
        val current = TouchAction.fromByte(map[edit.gestureIndex])
        ActionPickerSheet(
            earLabel = if (edit.leftEar) "Left" else "Right",
            gestureName = TouchMap.GESTURES[edit.gestureIndex],
            current = current,
            onSelect = { action ->
                ble.setTouchAction(edit.leftEar, edit.gestureIndex, action)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun EarSegmentedControl(
    leftSelected: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Radius.Shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space.Xs),
            horizontalArrangement = Arrangement.spacedBy(Space.Xs),
        ) {
            EarChip(
                label = "Left",
                isLeft = true,
                selected = leftSelected,
                onClick = onSelectLeft,
                modifier = Modifier.weight(1f),
            )
            EarChip(
                label = "Right",
                isLeft = false,
                selected = !leftSelected,
                onClick = onSelectRight,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EarChip(
    label: String,
    isLeft: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = Radius.ShapeSmall,
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        tonalElevation = if (selected) 1.dp else 0.dp,
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(
                    if (isLeft) R.drawable.motif_earbud_left else R.drawable.motif_earbud_right,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(if (selected) 1f else 0.55f),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(Space.Xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GestureMappingRow(
    gesture: String,
    action: TouchAction,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.Md, vertical = Space.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.Md),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon(),
                    contentDescription = null,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gesture,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = action.displayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Change",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ActionPickerSheet(
    earLabel: String,
    gestureName: String,
    current: TouchAction,
    onSelect: (TouchAction) -> Unit,
    onDismiss: () -> Unit,
) {
    // Custom stable sheet — no Material ModalBottomSheet settle/shake loop.
    StableBottomSheet(
        visible = true,
        onDismiss = onDismiss,
        maxHeightFraction = 0.90f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.Lg),
        ) {
            Text(
                text = "Choose action",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Space.Xxs))
            Text(
                text = "$earLabel · $gestureName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Space.Md))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .selectableGroup(),
                contentPadding = PaddingValues(bottom = Space.Lg),
                verticalArrangement = Arrangement.spacedBy(Space.Md),
            ) {
                ActionGroups.forEach { group ->
                    item(key = "header_${group.title}") {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Space.Xxs),
                        )
                    }
                    item(key = "card_${group.title}") {
                        Surface(
                            shape = Radius.Shape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                group.actions.forEachIndexed { i, action ->
                                    ActionOptionRow(
                                        action = action,
                                        selected = action == current,
                                        onClick = { onSelect(action) },
                                    )
                                    if (i < group.actions.lastIndex) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(start = 56.dp)
                                                .height(0.5.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionOptionRow(
    action: TouchAction,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = Space.Md, vertical = Space.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.Md),
    ) {
        Icon(
            imageVector = action.icon(),
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.displayLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = action.displaySubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RadioButton(
            selected = selected,
            onClick = null, // row handles selection
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

/** Shorter, readable labels for the UI. */
private val TouchAction.displayLabel: String
    get() = when (this) {
        TouchAction.NOTHING -> "Do nothing"
        TouchAction.ASSISTANT -> "Voice assistant"
        TouchAction.ANC_OFF_TRA -> "Off ↔ Transparency"
        TouchAction.ANC_ALL -> "Cycle all ANC modes"
        TouchAction.ANC_TRA -> "ANC ↔ Transparency"
        TouchAction.ANC_OFF -> "ANC ↔ Off"
        TouchAction.SPOTIFY -> "Spotify Tap"
        TouchAction.VOLUME_UP -> "Volume up"
        TouchAction.VOLUME_DOWN -> "Volume down"
        TouchAction.PLAY_CALL -> "Play / pause & calls"
        TouchAction.PREVIOUS -> "Previous track"
        TouchAction.PLAY_PAUSE -> "Play / pause"
        TouchAction.NEXT -> "Next track"
    }

private val TouchAction.displaySubtitle: String
    get() = when (this) {
        TouchAction.NOTHING -> "Gesture does nothing"
        TouchAction.ASSISTANT -> "Siri, Google Assistant, etc."
        TouchAction.ANC_OFF_TRA -> "Toggle ambient and off"
        TouchAction.ANC_ALL -> "Off → ANC → Transparency"
        TouchAction.ANC_TRA -> "Switch noise cancel and aware"
        TouchAction.ANC_OFF -> "Switch noise cancel and off"
        TouchAction.SPOTIFY -> "Resume or open Spotify"
        TouchAction.VOLUME_UP -> "Raise volume one step"
        TouchAction.VOLUME_DOWN -> "Lower volume one step"
        TouchAction.PLAY_CALL -> "Media and answer / end calls"
        TouchAction.PREVIOUS -> "Go to previous track"
        TouchAction.PLAY_PAUSE -> "Toggle music playback"
        TouchAction.NEXT -> "Skip to next track"
    }

private fun TouchAction.icon(): ImageVector = when (this) {
    TouchAction.NOTHING -> Icons.Rounded.Block
    TouchAction.ASSISTANT -> Icons.Rounded.Mic
    TouchAction.ANC_OFF_TRA -> Icons.Rounded.NoiseAware
    TouchAction.ANC_ALL -> Icons.Rounded.NoiseControlOff
    TouchAction.ANC_TRA -> Icons.Rounded.NoiseAware
    TouchAction.ANC_OFF -> Icons.Rounded.NoiseControlOff
    TouchAction.SPOTIFY -> Icons.Rounded.MusicNote
    TouchAction.VOLUME_UP -> Icons.AutoMirrored.Rounded.VolumeUp
    TouchAction.VOLUME_DOWN -> Icons.AutoMirrored.Rounded.VolumeDown
    TouchAction.PLAY_CALL -> Icons.Rounded.PlayArrow
    TouchAction.PREVIOUS -> Icons.Rounded.SkipPrevious
    TouchAction.PLAY_PAUSE -> Icons.Rounded.PlayArrow
    TouchAction.NEXT -> Icons.Rounded.SkipNext
}
