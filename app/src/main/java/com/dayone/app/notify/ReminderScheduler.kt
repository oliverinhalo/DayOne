package com.dayone.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dayone.app.DayOneApp
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules, per project, a daily alarm at that project's reminder time. When the alarm fires
 * and the day's photo hasn't been taken yet, ReminderAlarmReceiver posts a notification and
 * re-arms a follow-up "nag" alarm every NAG_INTERVAL_MINUTES so you can't just dismiss and forget.
 * The nag stops the moment a photo is captured (see markDoneToday) or at end of day.
 */
object ReminderScheduler {

    const val NAG_INTERVAL_MINUTES = 45L
    const val EXTRA_PROJECT_ID = "project_id"
    const val EXTRA_IS_NAG = "is_nag"

    fun rescheduleAll(context: Context) {
        val repo = (context.applicationContext as DayOneApp).repository
        runBlocking {
            val projects = repo.getReminderEnabledProjects()
            projects.forEach { scheduleDaily(context, it.id, it.reminderMinuteOfDay) }
        }
    }

    fun scheduleDaily(context: Context, projectId: Long, minuteOfDay: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60

        var trigger = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, minute))
        if (trigger.isBefore(LocalDateTime.now())) {
            trigger = trigger.plusDays(1)
        }
        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = baseIntent(context, projectId, isNag = false)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode(projectId, false), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    /** Arms the next nag alarm, NAG_INTERVAL_MINUTES from now, only fired while undone for today. */
    fun scheduleNag(context: Context, projectId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerMillis = System.currentTimeMillis() + NAG_INTERVAL_MINUTES * 60_000L

        val intent = baseIntent(context, projectId, isNag = true)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode(projectId, true), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    /** Call once the photo is captured: cancels any pending nag and clears the notification. */
    fun markDoneToday(context: Context, projectId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = baseIntent(context, projectId, isNag = true)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode(projectId, true), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.cancel(projectId.toInt())

        // Re-arm tomorrow's first reminder
        runBlocking {
            val repo = (context.applicationContext as DayOneApp).repository
            val project = repo.getProject(projectId) ?: return@runBlocking
            scheduleDaily(context, projectId, project.reminderMinuteOfDay)
        }
    }

    private fun baseIntent(context: Context, projectId: Long, isNag: Boolean): Intent =
        Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PROJECT_ID, projectId)
            putExtra(EXTRA_IS_NAG, isNag)
        }

    private fun requestCode(projectId: Long, isNag: Boolean): Int =
        (projectId.toInt() * 2) + if (isNag) 1 else 0
}
