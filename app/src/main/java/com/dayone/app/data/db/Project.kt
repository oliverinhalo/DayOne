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

    // Whether the daily reminder is enabled for this project
    val reminderEnabled: Boolean = true,

    // Accent color for this project's UI card, as an ARGB int
    val colorArgb: Int = 0xFF00E5A0.toInt(),

    val createdAtMillis: Long = System.currentTimeMillis(),

    /**
     * Which days of the week this project is "on", as a bitmask built by [Weekdays].
     * Bit 0 = Monday ... bit 6 = Sunday. Days outside the mask never fire a reminder
     * and never break a streak - they simply aren't part of the schedule.
     * Default = all seven days.
     */
    val activeDaysMask: Int = Weekdays.ALL,

    /** Repeat the reminder while the day's photo is still missing. */
    val nagEnabled: Boolean = true,

    /** Minutes between repeats when [nagEnabled]. */
    val nagIntervalMinutes: Int = 45,

    /** Minute-of-day after which repeats stop for the day (default 22:00). */
    val nagUntilMinuteOfDay: Int = 1320,

    /** Optional second reminder later in the day; null = none. */
    val secondReminderMinuteOfDay: Int? = null,

    /** Archived projects stay on disk but drop out of the main list and reminders. */
    val archived: Boolean = false,

    /** Manual ordering in the project list; lower sorts first. */
    val sortIndex: Int = 0,

    /** Face-centred auto-crop on capture. Off = keep the framing exactly as shot. */
    val autoCropFace: Boolean = true,

    /**
     * How much of the frame height the head should fill when auto-cropping, 0.2f..0.8f.
     * Larger = tighter crop. Mirrors the guide oval drawn on the capture screen.
     */
    val headFraction: Float = 0.42f
)

/** Bit helpers for [Project.activeDaysMask]. Bit 0 = Monday (matches DayOfWeek.MONDAY.value - 1). */
object Weekdays {
    const val ALL = 0b1111111
    const val WEEKDAYS = 0b0011111   // Mon-Fri
    const val WEEKENDS = 0b1100000   // Sat-Sun

    /** @param dayOfWeekValue java.time.DayOfWeek.value, 1 (Mon) .. 7 (Sun) */
    fun contains(mask: Int, dayOfWeekValue: Int): Boolean =
        mask and (1 shl (dayOfWeekValue - 1)) != 0

    fun toggle(mask: Int, dayOfWeekValue: Int): Int =
        mask xor (1 shl (dayOfWeekValue - 1))

    fun count(mask: Int): Int = (1..7).count { contains(mask, it) }

    /** Short human label, e.g. "Every day", "Mon-Fri", "Mon, Wed, Sat". */
    fun label(mask: Int): String = when {
        mask and ALL == ALL -> "Every day"
        mask and ALL == WEEKDAYS -> "Weekdays (Mon-Fri)"
        mask and ALL == WEEKENDS -> "Weekends"
        count(mask) == 0 -> "No days selected"
        else -> (1..7).filter { contains(mask, it) }.joinToString(", ") { NAMES[it - 1] }
    }

    val NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
}
