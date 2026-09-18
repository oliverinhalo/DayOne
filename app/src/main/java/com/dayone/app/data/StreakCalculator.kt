package com.dayone.app.data

import com.dayone.app.data.db.Weekdays
import java.time.LocalDate

data class StreakStats(
    val current: Int = 0,
    val best: Int = 0,
    val total: Int = 0,
    /** Scheduled days so far that were neither shot nor deliberately skipped. */
    val missed: Int = 0,
    /** 0f..1f of scheduled days that were actually shot. */
    val completion: Float = 0f,
    val firstDay: LocalDate? = null,
    val lastDay: LocalDate? = null,
    /** Scheduled, not shot, not skipped - most recent first. */
    val missedDays: List<LocalDate> = emptyList(),
    val doneToday: Boolean = false,
    /** True when today isn't part of this project's schedule (a rest day). */
    val restDayToday: Boolean = false
)

/**
 * Streak maths that understands part-time schedules.
 *
 * A project can be scheduled for any subset of weekdays (e.g. Mon/Wed/Sat). Days that
 * aren't scheduled are invisible to the streak: they neither count towards it nor break
 * it. Days the user deliberately skipped behave the same way, so an honest "not today"
 * doesn't cost a 200-day run.
 */
object StreakCalculator {

    fun compute(
        capturedDays: Set<Long>,
        activeDaysMask: Int,
        skippedDays: Set<Long> = emptySet(),
        today: LocalDate = LocalDate.now()
    ): StreakStats {
        val restDayToday = !isActive(today, activeDaysMask)
        if (capturedDays.isEmpty()) return StreakStats(restDayToday = restDayToday)

        val firstDay = LocalDate.ofEpochDay(capturedDays.min())
        val lastDay = LocalDate.ofEpochDay(capturedDays.max())
        val doneToday = capturedDays.contains(today.toEpochDay())

        // --- current streak: walk backwards from today --------------------------------
        // Today only breaks a streak once it's over, so while it's still outstanding
        // start counting from yesterday instead.
        var cursor = if (doneToday) today else today.minusDays(1)
        var current = 0
        var walking = true
        while (walking && !cursor.isBefore(firstDay)) {
            val day = cursor.toEpochDay()
            when {
                !isActive(cursor, activeDaysMask) -> Unit       // rest day: pass through
                skippedDays.contains(day) -> Unit               // deliberate skip: pass through
                capturedDays.contains(day) -> current++
                else -> walking = false
            }
            if (walking) cursor = cursor.minusDays(1)
        }

        // --- history: best streak, completion, missed days ----------------------------
        var best = 0
        var run = 0
        var scheduled = 0
        val missedDays = mutableListOf<LocalDate>()

        var d = firstDay
        while (!d.isAfter(today)) {
            val day = d.toEpochDay()
            if (isActive(d, activeDaysMask) && !skippedDays.contains(day)) {
                when {
                    capturedDays.contains(day) -> {
                        run++
                        if (run > best) best = run
                        scheduled++
                    }
                    d.isEqual(today) -> Unit    // today is still open: not a miss yet
                    else -> {
                        run = 0
                        scheduled++
                        missedDays.add(d)
                    }
                }
            }
            d = d.plusDays(1)
        }

        return StreakStats(
            current = current,
            best = maxOf(best, current),
            total = capturedDays.size,
            missed = missedDays.size,
            completion = if (scheduled == 0) 0f else (scheduled - missedDays.size).toFloat() / scheduled,
            firstDay = firstDay,
            lastDay = lastDay,
            missedDays = missedDays.asReversed(),
            doneToday = doneToday,
            restDayToday = restDayToday
        )
    }

    fun isActive(date: LocalDate, mask: Int): Boolean =
        Weekdays.contains(mask, date.dayOfWeek.value)

    /** The next date on or after [from] that the project is scheduled for, or null if it has no days. */
    fun nextActiveDay(from: LocalDate, mask: Int): LocalDate? {
        if (Weekdays.count(mask) == 0) return null
        var d = from
        repeat(8) {
            if (isActive(d, mask)) return d
            d = d.plusDays(1)
        }
        return null
    }
}
