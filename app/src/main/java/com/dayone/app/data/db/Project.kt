package com.dayone.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single independent daily-photo project. The app supports any number of these
 * running side by side (e.g. "Me", "Group photo", "Mum") - each with its own
 * folder on disk, its own reminder schedule, and its own streak.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    // Folder name under DayOne/ on external app storage, e.g. "Me", "Group_Photo"
    val folderName: String,

    // Epoch millis of the very first entry - used for "day number" / "year" overlays
    val startDateMillis: Long,

    // Optional date of birth in epoch millis, used to compute "age" overlay. Null = skip.
    val birthDateMillis: Long? = null,

    // Reminder time of day, minutes since midnight (default 8:00am = 480)
    val reminderMinuteOfDay: Int = 480,

    // Whether the repeating "don't let me miss a day" nag is enabled for this project
    val reminderEnabled: Boolean = true,

    // Accent color for this project's UI card, as an ARGB int
    val colorArgb: Int = 0xFF00E5A0.toInt(),

    val createdAtMillis: Long = System.currentTimeMillis()
)
