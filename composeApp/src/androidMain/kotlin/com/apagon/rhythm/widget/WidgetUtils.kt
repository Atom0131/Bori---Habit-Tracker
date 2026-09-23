package com.apagon.rhythm.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.apagon.rhythm.MainActivity

internal suspend fun <T : GlanceAppWidget> T.updateAll(context: Context) {
    GlanceAppWidgetManager(context)
        .getGlanceIds(javaClass)
        .forEach { update(context, it) }
}

/**
 * Launch intent that opens Rhythm directly on [tab] (one of MainActivity's navTabs routes:
 * "list", "stats", "clock", "notes") instead of whatever screen the app last had open.
 * [habitId], when set, additionally deep-links straight into that habit's detail sheet
 * (only meaningful when [tab] == "list" and the habit still resolves — see MainActivity/HabitListScreen).
 */
internal fun openTabIntent(context: Context, tab: String, habitId: Long? = null): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_OPEN_TAB, tab)
        if (habitId != null) putExtra(MainActivity.EXTRA_OPEN_HABIT_ID, habitId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

internal suspend fun refreshAllWidgets(context: Context) {
    HabitRingWidget().updateAll(context)
    HabitGridWidget().updateAll(context)
    TodayGlanceWidget().updateAll(context)
    StreakSpotlightWidget().updateAll(context)
    ActiveTimerWidget().updateAll(context)
}
