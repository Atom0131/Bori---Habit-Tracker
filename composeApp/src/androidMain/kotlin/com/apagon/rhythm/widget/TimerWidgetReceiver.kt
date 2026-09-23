package com.apagon.rhythm.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apagon.rhythm.notifications.TimerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerWidgetReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PAUSE = "com.apagon.rhythm.WIDGET_PAUSE_TIMER"
        const val ACTION_CANCEL = "com.apagon.rhythm.WIDGET_CANCEL_TIMER"
        const val EXTRA_TIMER_ID = "timer_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
        if (timerId == -1L) return
        when (intent.action) {
            ACTION_PAUSE -> context.startForegroundService(
                Intent(context, TimerForegroundService::class.java).apply {
                    action = TimerForegroundService.ACTION_PAUSE_TIMER
                    putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
                }
            )
            ACTION_CANCEL -> context.startForegroundService(
                Intent(context, TimerForegroundService::class.java).apply {
                    action = TimerForegroundService.ACTION_CANCEL_TIMER
                    putExtra(TimerForegroundService.EXTRA_TIMER_ID, timerId)
                }
            )
        }
        CoroutineScope(Dispatchers.IO).launch {
            ActiveTimerWidget().updateAll(context)
        }
    }
}
