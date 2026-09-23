package com.apagon.rhythm.widget

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object HabitGridWidgetPrefs {
    val SELECTED_HABITS_KEY = stringPreferencesKey("habit_grid_selected_habits")

    fun parseIds(raw: String?): List<Long> =
        raw?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

    fun joinIds(ids: List<Long>): String = ids.joinToString(",")

    /** "default" | "app_theme" | "custom" | "system" — see WidgetColors.STYLE_*. */
    val STYLE_KEY = stringPreferencesKey("habit_grid_widget_style")
    /** Null = use the app's global accent color. */
    val CUSTOM_COLOR_KEY = intPreferencesKey("habit_grid_custom_color_argb")
    val OPACITY_KEY = floatPreferencesKey("habit_grid_opacity")
    const val DEFAULT_OPACITY = 0.50f
}
