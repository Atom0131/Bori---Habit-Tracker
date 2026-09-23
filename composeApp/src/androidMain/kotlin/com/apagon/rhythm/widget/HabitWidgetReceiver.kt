package com.apagon.rhythm.widget
import com.apagon.rhythm.core.time.*

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class HabitWidgetReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.apagon.rhythm.WIDGET_COMPLETE_HABIT"
        const val EXTRA_HABIT_ID = "habit_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE) return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId == -1L) return
        val today = LocalDate.now().toString()
        CoroutineScope(Dispatchers.IO).launch {
            val ep = WidgetEntryPoint()
            ep.habitRepository().markComplete(habitId, today)
            HabitRingWidget().updateAll(context)
            HabitGridWidget().updateAll(context)
        }
    }
}
