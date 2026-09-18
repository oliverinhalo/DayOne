package com.dayone.app.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dayone.app.DayOneApp
import com.dayone.app.MainActivity
import com.dayone.app.R
import com.dayone.app.data.db.Project

/** Builds and posts the "today's photo is still missing" notification. */
object ReminderNotifier {

    fun notify(context: Context, project: Project, dayNumber: Int, currentStreak: Int) {
        // On Android 13+ posting without the runtime permission is a silent no-op, so
        // bail out early rather than building a notification nobody will ever see.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            project.id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_CAPTURE_PROJECT_ID, project.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = buildString {
            append("Today's photo for \"${project.name}\" isn't taken yet.")
            if (currentStreak > 0) append(" You're on a $currentStreak-day streak - don't drop it now.")
        }

        val builder = NotificationCompat.Builder(context, DayOneApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(project.colorArgb)
            .setContentTitle(
                if (currentStreak > 0) "Day $dayNumber - ${project.name} ($currentStreak day streak)"
                else "Day $dayNumber - ${project.name}"
            )
            .setContentText("Tap to take today's photo")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Take photo", openPendingIntent)
            .addAction(0, "Snooze 1h", actionIntent(context, project.id, NotificationActionReceiver.ACTION_SNOOZE, 60))
            .addAction(0, "Skip today", actionIntent(context, project.id, NotificationActionReceiver.ACTION_SKIP, 0))

        runCatching {
            NotificationManagerCompat.from(context).notify(project.id.toInt(), builder.build())
        }
    }

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun cancel(context: Context, projectId: Long) {
        runCatching { NotificationManagerCompat.from(context).cancel(projectId.toInt()) }
    }

    private fun actionIntent(context: Context, projectId: Long, action: String, minutes: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = "com.dayone.app.$action.$projectId"
            putExtra(ReminderScheduler.EXTRA_PROJECT_ID, projectId)
            putExtra(NotificationActionReceiver.EXTRA_ACTION, action)
            putExtra(NotificationActionReceiver.EXTRA_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            (projectId.toInt() * 10) + action.hashCode().and(7),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
