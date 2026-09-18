package com.dayone.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dayone.app.DayOneApp
import com.dayone.app.data.StreakCalculator
import com.dayone.app.data.db.Project
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Per-project reminder alarms.
 *
 * Each project has its own time of day, its own set of weekdays, and its own repeat
 * ("nag") behaviour. Alarms are one-shot and re-armed as they fire, which is what lets
 * a Mon/Wed/Sat schedule skip straight over Tuesday instead of firing and doing nothing.
 */
object ReminderScheduler {

    const val EXTRA_PROJECT_ID = "project_id"
    const val EXTRA_KIND = "kind"

    const val KIND_DAILY = 0
    const val KIND_NAG = 1
    const val KIND_SECOND = 2
    const val KIND_SNOOZE = 3

    /** Legacy default, still used when a project has no explicit interval. */
    const val NAG_INTERVAL_MINUTES = 45L

    // ---------------------------------------------------------------- scheduling

    fun rescheduleAll(context: Context) {
        val repo = (context.applicationContext as DayOneApp).repository
        runBlocking {
            repo.getAllProjects().forEach { project ->
                cancelAll(context, project.id)
                if (project.reminderEnabled && !project.archived) schedule(context, project)
            }
        }
    }

    fun schedule(context: Context, project: Project) {
        if (!project.reminderEnabled || project.archived) {
            cancelAll(context, project.id)
            return
        }
        scheduleNextOccurrence(context, project, project.reminderMinuteOfDay, KIND_DAILY)
        project.secondReminderMinuteOfDay?.let {
            scheduleNextOccurrence(context, project, it, KIND_SECOND)
        }
    }

    /** Convenience for callers that only have an id (e.g. after a notification action). */
    fun scheduleById(context: Context, projectId: Long) {
        val repo = (context.applicationContext as DayOneApp).repository
        runBlocking { repo.getProject(projectId)?.let { schedule(context, it) } }
    }

    /**
     * Arms the next firing of [minuteOfDay] on a day this project is actually scheduled
     * for - today if that time hasn't passed yet, otherwise the next active weekday.
     */
    private fun scheduleNextOccurrence(context: Context, project: Project, minuteOfDay: Int, kind: Int) {
        val now = LocalDateTime.now()
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

        var candidateDate = LocalDate.now()
        if (!LocalDateTime.of(candidateDate, time).isAfter(now)) {
            candidateDate = candidateDate.plusDays(1)
        }
        val target = StreakCalculator.nextActiveDay(candidateDate, project.activeDaysMask) ?: return
        val triggerMillis = LocalDateTime.of(target, time)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        setAlarm(context, triggerMillis, pendingIntent(context, project.id, kind))
    }

    /** Arms the next repeat while today's photo is still outstanding. */
    fun scheduleNag(context: Context, project: Project) {
        if (!project.nagEnabled) return
        val interval = project.nagIntervalMinutes.coerceAtLeast(5).toLong()
        val next = LocalDateTime.now().plusMinutes(interval)
        val cutoff = LocalDateTime.of(
            LocalDate.now(),
            LocalTime.of(project.nagUntilMinuteOfDay / 60, project.nagUntilMinuteOfDay % 60)
        )
        if (next.isAfter(cutoff)) return   // done pestering for today

        val triggerMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setAlarm(context, triggerMillis, pendingIntent(context, project.id, KIND_NAG))
    }

    fun snooze(context: Context, projectId: Long, minutes: Int) {
        val triggerMillis = System.currentTimeMillis() + minutes * 60_000L
        setAlarm(context, triggerMillis, pendingIntent(context, projectId, KIND_SNOOZE))
    }

    /** Photo captured (or day skipped): stop repeating today and line up the next day. */
    fun markDoneToday(context: Context, projectId: Long) {
        cancel(context, projectId, KIND_NAG)
        cancel(context, projectId, KIND_SNOOZE)
        ReminderNotifier.cancel(context, projectId)
        scheduleById(context, projectId)
    }

    fun cancelAll(context: Context, projectId: Long) {
        listOf(KIND_DAILY, KIND_NAG, KIND_SECOND, KIND_SNOOZE).forEach { cancel(context, projectId, it) }
        ReminderNotifier.cancel(context, projectId)
    }

    private fun cancel(context: Context, projectId: Long, kind: Int) {
        alarmManager(context).cancel(pendingIntent(context, projectId, kind))
    }

    // ---------------------------------------------------------------- plumbing

    private fun setAlarm(context: Context, triggerMillis: Long, pendingIntent: PendingIntent) {
        val am = alarmManager(context)
        val canBeExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        if (canBeExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            // Without the exact-alarm permission this still fires, just with some slack.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(context: Context, projectId: Long, kind: Int): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            // A distinct action per project+kind keeps PendingIntents from colliding,
            // since extras alone are not part of PendingIntent identity.
            action = "com.dayone.app.REMINDER_${projectId}_$kind"
            putExtra(EXTRA_PROJECT_ID, projectId)
            putExtra(EXTRA_KIND, kind)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(projectId, kind),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(projectId: Long, kind: Int): Int = (projectId.toInt() * 10) + kind
}
