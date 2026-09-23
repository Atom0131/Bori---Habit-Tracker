package com.apagon.rhythm.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        refreshAllWidgets(ctx)

        // If a timer is active, self-schedule a 30s refresh so the countdown stays live
        val ep = WidgetEntryPoint()
        val now = System.currentTimeMillis()
        val hasActiveTimer = ep.timerRepository().getAllTimers().first()
            .any { it.deletedAt == null && it.endTimeMillis > now }
        if (hasActiveTimer) {
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "timer_widget_refresh",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                    .setInitialDelay(30, TimeUnit.SECONDS)
                    .addTag("timer_widget_refresh")
                    .build()
            )
        }

        return Result.success()
    }
}
