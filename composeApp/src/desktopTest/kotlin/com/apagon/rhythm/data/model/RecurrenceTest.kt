package com.apagon.rhythm.data.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecurrenceTest {

    companion object {
        /**
         * rule | start | from | to | expected dates. **Android's RecurrenceTest has this exact table**
         * — change both together, so the two apps keep giving the same dates.
         */
        const val TABLE = """
FREQ=DAILY | 2026-11-07 | 2026-11-07 | 2026-11-10 | 2026-11-07,2026-11-08,2026-11-09,2026-11-10
FREQ=DAILY;INTERVAL=3 | 2026-11-01 | 2026-11-05 | 2026-11-12 | 2026-11-07,2026-11-10
FREQ=WEEKLY | 2026-11-03 | 2026-11-01 | 2026-11-30 | 2026-11-03,2026-11-10,2026-11-17,2026-11-24
FREQ=WEEKLY;BYDAY=MO,WE | 2026-11-03 | 2026-11-01 | 2026-11-16 | 2026-11-04,2026-11-09,2026-11-11,2026-11-16
FREQ=WEEKLY;INTERVAL=2;BYDAY=TU,FR | 2026-11-03 | 2026-11-01 | 2026-11-30 | 2026-11-03,2026-11-06,2026-11-17,2026-11-20
FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR | 2026-11-06 | 2026-11-06 | 2026-11-11 | 2026-11-06,2026-11-09,2026-11-10,2026-11-11
FREQ=MONTHLY | 2026-01-31 | 2026-01-01 | 2026-05-31 | 2026-01-31,2026-02-28,2026-03-31,2026-04-30,2026-05-31
FREQ=MONTHLY;INTERVAL=2 | 2026-11-15 | 2026-11-01 | 2027-05-31 | 2026-11-15,2027-01-15,2027-03-15,2027-05-15
FREQ=YEARLY | 2024-02-29 | 2024-01-01 | 2028-12-31 | 2024-02-29,2025-02-28,2026-02-28,2027-02-28,2028-02-29
FREQ=DAILY;UNTIL=20261109 | 2026-11-07 | 2026-11-01 | 2026-11-30 | 2026-11-07,2026-11-08,2026-11-09
FREQ=WEEKLY;BYDAY=MO,WE;COUNT=3 | 2026-11-02 | 2026-11-01 | 2026-12-31 | 2026-11-02,2026-11-04,2026-11-09
FREQ=WEEKLY;COUNT=3 | 2026-11-02 | 2026-11-10 | 2026-12-31 | 2026-11-16
FREQ=DAILY | 2026-11-07 | 2026-11-01 | 2026-11-06 |
"""

        fun rows() = TABLE.trim().lines().map { line ->
            val p = line.split('|').map { it.trim() }
            Row(p[0], LocalDate.parse(p[1]), LocalDate.parse(p[2]), LocalDate.parse(p[3]),
                p[4].split(',').filter { it.isNotBlank() }.map { LocalDate.parse(it) })
        }
    }

    data class Row(val rule: String, val start: LocalDate, val from: LocalDate, val to: LocalDate, val expected: List<LocalDate>)

    @Test
    fun table() {
        rows().forEach { r ->
            val rec = Recurrence.parse(r.rule)!!
            assertEquals(r.expected, rec.occurrences(r.start, r.from, r.to), r.rule)
            assertEquals(Recurrence.parse(rec.format()), rec, "round trip ${r.rule}")
        }
    }

    @Test
    fun skipsNextAndEnd() {
        val rec = Recurrence.parse("FREQ=WEEKLY;BYDAY=MO")!!
        val start = LocalDate.parse("2026-11-02")
        val skip = Recurrence.parseSkips("2026-11-09, junk")
        assertEquals(listOf(LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-16")),
            rec.occurrences(start, start, LocalDate.parse("2026-11-16"), skip))
        assertEquals("2026-11-09", Recurrence.formatSkips(skip))
        assertEquals(LocalDate.parse("2026-11-09"), rec.next(start, LocalDate.parse("2026-11-02")))
        assertEquals(LocalDate.parse("2026-11-09"), rec.next(start, LocalDate.parse("2026-11-05")))
        assertNull(Recurrence.parse("FREQ=DAILY;COUNT=2")!!.next(start, LocalDate.parse("2026-11-03")))
    }

    @Test
    fun parseAndDescribe() {
        assertNull(Recurrence.parse(""))
        assertNull(Recurrence.parse("FREQ=HOURLY"))
        assertNull(Recurrence.parse("nonsense"))
        val d = LocalDate.parse("2026-11-03") // a Tuesday
        assertEquals("Daily", Recurrence.parse("FREQ=DAILY")!!.describe(d))
        assertEquals("Every 3 days", Recurrence.parse("FREQ=DAILY;INTERVAL=3")!!.describe(d))
        assertEquals("Weekly on Tue", Recurrence.parse("FREQ=WEEKLY")!!.describe(d))
        assertEquals("Every 2 weeks on Mon, Wed, until Dec 20, 2026",
            Recurrence.parse("FREQ=WEEKLY;INTERVAL=2;BYDAY=WE,MO;UNTIL=20261220")!!.describe(d))
        assertEquals("Every weekday", Recurrence.parse("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR")!!.describe(d))
        assertEquals("Monthly on day 3, 5 times", Recurrence.parse("FREQ=MONTHLY;COUNT=5")!!.describe(d))
        assertEquals("Yearly on Nov 3", Recurrence.parse("RRULE:FREQ=YEARLY")!!.describe(d))
    }
}
