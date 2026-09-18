package com.dayone.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.dayone.app.data.PhotoRepository
import com.dayone.app.data.SettingsRepository
import com.dayone.app.notify.ReminderScheduler

class DayOneApp : Application() {

    lateinit var repository: PhotoRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = PhotoRepository(this)
        settingsRepository = SettingsRepository(this)
        createNotificationChannel()
        // Re-arm every project's daily reminder alarm on process start
        ReminderScheduler.rescheduleAll(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_daily),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notif_channel_daily_desc)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_photo_reminder"
    }
}
