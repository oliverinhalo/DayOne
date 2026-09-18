package com.dayone.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.dayone.app.data.PhotoRepository
import com.dayone.app.data.SettingsRepository
import com.dayone.app.data.StreakCalculator
import com.dayone.app.data.db.Project
import com.dayone.app.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DayOneApp : Application() {

    lateinit var repository: PhotoRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = PhotoRepository(this)
        settingsRepository = SettingsRepository(this)
        createNotificationChannel()

        // Re-arm every project's alarms off the main thread - this touches the database.
        appScope.launch {
            runCatching { ReminderScheduler.rescheduleAll(this@DayOneApp) }
        }
    }

    /** Current streak for [project], honouring its weekday schedule and skipped days. */
    suspend fun streakFor(project: Project): Int {
        val days = repository.getEntries(project.id).map { it.dateEpochDay }.toSet()
        return StreakCalculator.compute(
            capturedDays = days,
            activeDaysMask = project.activeDaysMask,
            skippedDays = settingsRepository.skippedDays(project.id)
        ).current
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
                setShowBadge(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_photo_reminder"
    }
}
