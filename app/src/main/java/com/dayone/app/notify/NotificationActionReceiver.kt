package com.dayone.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayone.app.DayOneApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Handles the "Snooze" and "Skip today" buttons on a reminder notification. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val projectId = intent.getLongExtra(ReminderScheduler.EXTRA_PROJECT_ID, -1L)
        if (projectId == -1L) return
        val app = context.applicationContext as DayOneApp

        when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 60)
                ReminderNotifier.cancel(context, projectId)
                ReminderScheduler.snooze(context, projectId, minutes)
            }
            ACTION_SKIP -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        app.settingsRepository.setSkipped(projectId, LocalDate.now().toEpochDay(), true)
                        ReminderScheduler.markDoneToday(context, projectId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = "action"
        const val EXTRA_MINUTES = "minutes"
        const val ACTION_SNOOZE = "snooze"
        const val ACTION_SKIP = "skip"
    }
}
