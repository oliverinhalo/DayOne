package com.dayone.app

import com.dayone.app.data.StreakCalculator
import com.dayone.app.data.db.Weekdays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The streak rules are the part of the app a user would notice being wrong immediately,
 * and they now have to understand part-time schedules, so they are pinned down here.
 */
class StreakCalculatorTest {

    private val monday = LocalDate.of(2025, 9, 15)      // a known Monday
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val thursday = monday.plusDays(3)
    private val friday = monday.plusDays(4)
    private val saturday = monday.plusDays(5)
    private val sunday = monday.plusDays(6)

    private fun days(vararg dates: LocalDate) = dates.map { it.toEpochDay() }.toSet()

    @Test
    fun `consecutive days count as a streak`() {
        val stats = StreakCalculator.compute(
            capturedDays = days(monday, tuesday, wednesday),
            activeDaysMask = Weekdays.ALL,
            today = wednesday
        )
        assertEquals(3, stats.current)
        assertEquals(3, stats.best)
        assertEquals(3, stats.total)
        assertTrue(stats.doneToday)
    }

    @Test
    fun `today still being outstanding does not break the streak`() {
        val stats = StreakCalculator.compute(
            capturedDays = days(monday, tuesday),
            activeDaysMask = Weekdays.ALL,
            today = wednesday
        )
        assertEquals(2, stats.current)
        assertFalse(stats.doneToday)
        assertEquals(0, stats.missed)
    }

    @Test
    fun `a missed day resets the streak`() {
        val stats = StreakCalculator.compute(
            capturedDays = days(monday, wednesday, thursday),
            activeDaysMask = Weekdays.ALL,
            today = thursday
        )
        assertEquals(2, stats.current)
        assertEquals(2, stats.best)
        assertEquals(1, stats.missed)
        assertEquals(listOf(tuesday), stats.missedDays)
    }

    @Test
    fun `days outside a weekday-only schedule neither count nor break`() {
        // Mon-Fri project: shooting Fri then Mon is an unbroken run across the weekend.
        val stats = StreakCalculator.compute(
            capturedDays = days(thursday, friday, monday.plusDays(7)),
            activeDaysMask = Weekdays.WEEKDAYS,
            today = monday.plusDays(7)
        )
        assertEquals(3, stats.current)
        assertEquals(0, stats.missed)
        assertEquals(1f, stats.completion, 0.001f)
    }

    @Test
    fun `a Mon-Wed-Sat schedule ignores every other day`() {
        val monWedSat = (1 shl 0) or (1 shl 2) or (1 shl 5)
        val stats = StreakCalculator.compute(
            capturedDays = days(monday, wednesday, saturday),
            activeDaysMask = monWedSat,
            today = sunday
        )
        assertEquals(3, stats.current)
        assertEquals(0, stats.missed)
        assertTrue(stats.restDayToday)      // Sunday isn't part of this schedule
        assertFalse(stats.doneToday)
    }

    @Test
    fun `missing a scheduled day on a part-time schedule does break the streak`() {
        val monWedSat = (1 shl 0) or (1 shl 2) or (1 shl 5)
        val stats = StreakCalculator.compute(
            capturedDays = days(monday, saturday),   // Wednesday skipped without marking it
            activeDaysMask = monWedSat,
            today = saturday
        )
        assertEquals(1, stats.current)
        assertEquals(1, stats.missed)
        assertEquals(listOf(wednesday), stats.missedDays)
    }

    @Test
    fun `a deliberately skipped day keeps the streak alive`() {
        val stats = StreakCalculator.compute(
            capturedDays = days(monday, wednesday),
            activeDaysMask = Weekdays.ALL,
            skippedDays = days(tuesday),
            today = wednesday
        )
        assertEquals(2, stats.current)
        assertEquals(0, stats.missed)
    }

    @Test
    fun `an empty project reports zeroes rather than throwing`() {
        val stats = StreakCalculator.compute(emptySet(), Weekdays.ALL, today = monday)
        assertEquals(0, stats.current)
        assertEquals(0, stats.total)
        assertEquals(null, stats.firstDay)
    }

    @Test
    fun `completion is the share of scheduled days actually shot`() {
        val stats = StreakCalculator.compute(
            capturedDays = days(monday, tuesday, thursday),
            activeDaysMask = Weekdays.ALL,
            today = friday                       // Friday is still open, so 4 days count
        )
        assertEquals(3f / 4f, stats.completion, 0.001f)
    }

    @Test
    fun `next active day skips forward to the next scheduled weekday`() {
        assertEquals(monday.plusDays(7), StreakCalculator.nextActiveDay(saturday, Weekdays.WEEKDAYS))
        assertEquals(saturday, StreakCalculator.nextActiveDay(saturday, Weekdays.WEEKENDS))
        assertEquals(null, StreakCalculator.nextActiveDay(saturday, 0))
    }

    @Test
    fun `weekday labels read the way people describe a schedule`() {
        assertEquals("Every day", Weekdays.label(Weekdays.ALL))
        assertEquals("Weekdays (Mon-Fri)", Weekdays.label(Weekdays.WEEKDAYS))
        assertEquals("Weekends", Weekdays.label(Weekdays.WEEKENDS))
        assertEquals("Mon, Wed, Sat", Weekdays.label((1 shl 0) or (1 shl 2) or (1 shl 5)))
    }
}
