package com.dayone.app.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dayone.app.DayOneApp
import com.dayone.app.MainActivity
import com.dayone.app.R
import com.dayone.app.data.db.Project
import kotlinx.coroutines.runBlocking

object ReminderNotifier {

    fun notify(context: Context, project: Project) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_CAPTURE_PROJECT_ID, project.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, project.id.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dayNumber = runBlocking {
            (context.applicationContext as DayOneApp).repository.countForProject(project.id) + 1
        }

        val builder = NotificationCompat.Builder(context, DayOneApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Day $dayNumber - ${project.name}")
            .setContentText("Take today's photo to keep your streak going")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "You haven't taken today's photo for \"${project.name}\" yet. Tap to open the camera - I'll keep reminding you every ${ReminderScheduler.NAG_INTERVAL_MINUTES} minutes until it's done."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Take photo now", contentPendingIntent)

        NotificationManagerCompat.from(context).apply {
            notify(project.id.toInt(), builder.build())
        }
    }
}
