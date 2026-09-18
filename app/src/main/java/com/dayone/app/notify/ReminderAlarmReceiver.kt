package com.dayone.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayone.app.DayOneApp
import com.dayone.app.data.StreakCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Fires for the daily reminder, the optional second reminder, a repeat, or a snooze.
 * It re-arms the following day's alarm as it goes, so a one-shot exact alarm chain
 * keeps running indefinitely without a repeating alarm (which Android delays heavily).
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val projectId = intent.getLongExtra(ReminderScheduler.EXTRA_PROJECT_ID, -1L)
        val kind = intent.getIntExtra(ReminderScheduler.EXTRA_KIND, ReminderScheduler.KIND_DAILY)
        if (projectId == -1L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as DayOneApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val project = app.repository.getProject(projectId) ?: return@launch

                // Chain the next day's alarm first, so an early return below can never
                // leave the project without a future reminder.
                if (kind == ReminderScheduler.KIND_DAILY || kind == ReminderScheduler.KIND_SECOND) {
                    ReminderScheduler.schedule(context, project)
                }

                if (!project.reminderEnabled || project.archived) return@launch

                val today = LocalDate.now()
                if (!StreakCalculator.isActive(today, project.activeDaysMask)) return@launch
                if (app.settingsRepository.isSkipped(projectId, today.toEpochDay())) return@launch
                if (app.repository.getEntryForDate(projectId, today) != null) {
                    // Already shot today - nothing to nag about.
                    ReminderNotifier.cancel(context, projectId)
                    return@launch
                }

                val streak = app.streakFor(project)
                ReminderNotifier.notify(
                    context = context,
                    project = project,
                    dayNumber = app.repository.countForProject(projectId) + 1,
                    currentStreak = streak
                )

                ReminderScheduler.scheduleNag(context, project)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
