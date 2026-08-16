package com.marshall.motif.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marshall.motif.ui.theme.M3EMotion

/**
 * Floating capsule bottom nav (ported from Finance Tracker).
 * Sliding selection pill, animated weights, optional side FAB.
 */
@Composable
fun FloatingBottomNav(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showFab: Boolean = false,
    fabIcon: ImageVector? = null,
    fabContentDescription: String = "",
    onFabClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val dockInset = 4.dp
    val selectedWeightTarget = 1.4f
    val unselectedWeightTarget = 0.85f
    val totalWeight = selectedWeightTarget + unselectedWeightTarget * (items.size - 1).coerceAtLeast(0)
    val safeSelected = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    Row(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.32f),
                    spotColor = Color.Black.copy(alpha = 0.40f),
                ),
            shape = CircleShape,
            color = scheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f)),
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(dockInset),
            ) {
                val targetPillWidth = maxWidth * (selectedWeightTarget / totalWeight)
                val targetOffset = maxWidth * (safeSelected.toFloat() * unselectedWeightTarget / totalWeight)
                val pillWidth by animateDpAsState(
                    targetValue = targetPillWidth,
                    animationSpec = M3EMotion.spatialDefault(),
                    label = "pillWidth",
                )
                val indicatorOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = M3EMotion.spatialDefault(),
                    label = "navIndicatorX",
                )
                Box(
                    Modifier
                        .offset(x = indicatorOffset)
                        .width(pillWidth)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(scheme.primaryContainer),
                )
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == safeSelected
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                scheme.onPrimaryContainer
                            } else {
                                scheme.onSurfaceVariant
                            },
                            animationSpec = M3EMotion.effectsDefault(),
                            label = "navColor$index",
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.12f else 1f,
                            animationSpec = M3EMotion.spatialFast(),
                            label = "navIconScale$index",
                        )
                        val animatedItemWeight by animateFloatAsState(
                            targetValue = if (isSelected) selectedWeightTarget else unselectedWeightTarget,
                            animationSpec = M3EMotion.spatialDefault(),
                            label = "navWeight$index",
                        )
                        Column(
                            modifier = Modifier
                                .weight(animatedItemWeight)
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSelect(index) },
                                )
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                            )
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = contentColor,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFab && fabIcon != null && onFabClick != null) {
            Surface(
                onClick = onFabClick,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.30f),
                        spotColor = Color.Black.copy(alpha = 0.38f),
                    ),
                shape = CircleShape,
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                shadowElevation = 0.dp,
                tonalElevation = 6.dp,
            ) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = fabIcon,
                        contentDescription = fabContentDescription,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

data class NavItem(
    val label: String,
    val icon: ImageVector,
)
