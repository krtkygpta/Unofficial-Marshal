package com.marshall.motif

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.marshall.motif.ui.theme.CustomThemeMode
import com.marshall.motif.ui.theme.ThemeMode

/** Persisted, Compose-observable app preferences. */
class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("marshall_settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(
        runCatching {
            val raw = preferences.getString("theme_mode", ThemeMode.DYNAMIC.name) ?: ThemeMode.DYNAMIC.name
            when (raw) {
                // Migrate the removed legacy choices to the closest new option.
                "PIXEL", "MARSHALL" -> ThemeMode.MONOCHROMATIC
                else -> ThemeMode.valueOf(raw)
            }
        }.getOrDefault(ThemeMode.DYNAMIC),
    )
        private set

    var customThemeMode by mutableStateOf(
        runCatching {
            CustomThemeMode.valueOf(
                preferences.getString("custom_theme_mode", CustomThemeMode.SYSTEM.name)
                    ?: CustomThemeMode.SYSTEM.name,
            )
        }.getOrDefault(CustomThemeMode.SYSTEM),
    )
        private set

    var accentColor by mutableStateOf(
        preferences.getInt("accent_color", 0xFFD4AF5A.toInt()),
    )
        private set

    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        preferences.edit().putString("theme_mode", mode.name).apply()
    }

    fun updateCustomThemeMode(mode: CustomThemeMode) {
        customThemeMode = mode
        preferences.edit().putString("custom_theme_mode", mode.name).apply()
    }

    fun updateAccentColor(color: Int) {
        accentColor = color
        preferences.edit().putInt("accent_color", color).apply()
    }
}
