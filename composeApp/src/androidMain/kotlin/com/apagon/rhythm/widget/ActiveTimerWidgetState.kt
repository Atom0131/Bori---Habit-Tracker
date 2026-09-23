package com.apagon.rhythm.widget

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object ActiveTimerWidgetPrefs {
    /** "default" | "app_theme" | "custom" | "system" — see WidgetColors.STYLE_*. */
    val STYLE_KEY = stringPreferencesKey("active_timer_widget_style")
    /** Null = use the app's global accent color. */
    val CUSTOM_COLOR_KEY = intPreferencesKey("active_timer_custom_color_argb")
    val OPACITY_KEY = floatPreferencesKey("active_timer_opacity")
    const val DEFAULT_OPACITY = 0.50f
}
