package com.apagon.rhythm.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object TodayGlanceWidgetPrefs {
    val SHOW_HABITS_KEY = booleanPreferencesKey("today_glance_show_habits")
    val SHOW_TODOS_KEY = booleanPreferencesKey("today_glance_show_todos")
    val SHOW_REMINDERS_KEY = booleanPreferencesKey("today_glance_show_reminders")

    /** "default" | "app_theme" | "custom" | "system" — see WidgetColors.STYLE_*. */
    val STYLE_KEY = stringPreferencesKey("today_glance_widget_style")
    /** Null = use the app's global accent color. */
    val CUSTOM_COLOR_KEY = intPreferencesKey("today_glance_custom_color_argb")
    val OPACITY_KEY = floatPreferencesKey("today_glance_opacity")
    const val DEFAULT_OPACITY = 0.50f
}
