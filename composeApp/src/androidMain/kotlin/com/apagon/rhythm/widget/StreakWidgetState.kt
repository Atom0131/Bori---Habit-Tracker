package com.apagon.rhythm.widget

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object StreakWidgetPrefs {
    val DRUM_ICON_KEY = stringPreferencesKey("streak_drum_icon")
    val HABIT_ID_KEY = longPreferencesKey("streak_habit_id")

    /** "default" | "app_theme" | "custom" | "system" — see WidgetColors.STYLE_*. */
    val STYLE_KEY = stringPreferencesKey("streak_widget_style")
    /** Null = use the app's global accent color. */
    val CUSTOM_COLOR_KEY = intPreferencesKey("streak_custom_color_argb")
    val OPACITY_KEY = floatPreferencesKey("streak_opacity")
    const val DEFAULT_OPACITY = 0.50f

    val DRUM_OPTIONS = listOf(
        "fire", "bomba", "taiko", "djembe", "snare", "tabla", "steelpan",
        "conga", "bongo", "cajon", "timbales", "talkingdrum", "bassdrum", "framedrum", "darbuka"
    )

    val DRUM_LABELS = mapOf(
        "fire" to "Fire",
        "bomba" to "Bomba",
        "taiko" to "Taiko",
        "djembe" to "Djembe",
        "snare" to "Snare",
        "tabla" to "Tabla",
        "steelpan" to "Steel Pan",
        "conga" to "Conga",
        "bongo" to "Bongo",
        "cajon" to "Cajón",
        "timbales" to "Timbales",
        "talkingdrum" to "Talking",
        "bassdrum" to "Bass",
        "framedrum" to "Frame",
        "darbuka" to "Darbuka"
    )
}
