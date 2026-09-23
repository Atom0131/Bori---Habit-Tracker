package com.apagon.rhythm.widget

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object HabitRingWidgetPrefs {
    val HABIT_ID_KEY = longPreferencesKey("habit_ring_habit_id")
    const val ALL_HABITS_ID = -1L

    /** "default" | "app_theme" | "custom" | "system" — see WidgetColors.STYLE_*. */
    val STYLE_KEY = stringPreferencesKey("habit_ring_widget_style")
    /** Null = use the app's global accent color. */
    val CUSTOM_COLOR_KEY = intPreferencesKey("habit_ring_custom_color_argb")
    val OPACITY_KEY = floatPreferencesKey("habit_ring_opacity")
    const val DEFAULT_OPACITY = 0.50f
}
