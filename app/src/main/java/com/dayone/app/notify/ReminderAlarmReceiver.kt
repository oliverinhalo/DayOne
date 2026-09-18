package com.dayone.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.dayone.app.DayOneApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val projectId = intent.getLongExtra(ReminderScheduler.EXTRA_PROJECT_ID, -1L)
        val isNag = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_NAG, false)
        if (projectId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = (context.applicationContext as DayOneApp).repository
                val project = repo.getProject(projectId)
                if (project == null || !project.reminderEnabled) return@launch

                val already = repo.hasCapturedToday(project)
                if (already) {
                    // Nothing to nag about - safety net in case markDoneToday's cancel raced this alarm.
                    return@launch
                }

                ReminderNotifier.notify(context, project)

                // Keep nagging every NAG_INTERVAL_MINUTES until midnight, then tomorrow's
                // first daily alarm (already scheduled) takes over.
                val stillToday = LocalDateTime.now().toLocalDate() == LocalDate.now()
                val minutesLeftToday = java.time.Duration.between(
                    LocalDateTime.now(), LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                ).toMinutes()
                if (stillToday && minutesLeftToday > ReminderScheduler.NAG_INTERVAL_MINUTES) {
                    ReminderScheduler.scheduleNag(context, projectId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
