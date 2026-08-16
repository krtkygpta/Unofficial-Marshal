package com.marshall.motif.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object M3EMotion {
    fun <T> spatialDefault(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = 700f,
    )

    fun <T> spatialFast(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.9f,
        stiffness = 1400f,
    )

    fun <T> effectsDefault(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1600f,
    )
}
