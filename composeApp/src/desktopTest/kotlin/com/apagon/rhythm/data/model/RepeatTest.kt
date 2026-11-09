package com.apagon.rhythm.data.model

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.apagon.rhythm.data.db.DesktopHabitDatabase
import com.apagon.rhythm.data.repository.CalendarEventRepository
import com.apagon.rhythm.data.repository.ReminderRepository
import com.apagon.rhythm.data.repository.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepeatTest {

    private lateinit var db: DesktopHabitDatabase

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<DesktopHabitDatabase>()
            .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
    }

    @AfterTest
    fun tearDown() = db.close()

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun todoMovesForwardAndKeepsItsAnchor() {
        val t = Todo(title = "Rent", dueDate = "2026-01-31 09:00", recurrence = "FREQ=MONTHLY")
        val feb = t.nextRepeat(d("2026-01-31"))!!
        assertEquals("2026-02-28 09:00", feb.dueDate)
        // The anchor keeps it on the 31st's series: after February comes March 31, not March 28.
        assertEquals("2026-03-31 09:00", feb.nextRepeat(d("2026-02-28"))!!.dueDate)
        // Overdue: a daily to-do done three days late comes back tomorrow, once.
        val daily = Todo(title = "Meds", dueDate = "2026-11-02", recurrence = "FREQ=DAILY")
        assertEquals("2026-11-06", daily.nextRepeat(d("2026-11-05"))!!.dueDate)
        // Series over: completes normally.
        assertNull(Todo(title = "x", dueDate = "2026-11-02", recurrence = "FREQ=DAILY;COUNT=1").nextRepeat(d("2026-11-02")))
    }

    @Test
    fun reminderSkipsMissedDates() {
        val r = Reminder(title = "Water", dateTime = "2026-11-02 08:00", recurrence = "FREQ=DAILY")
        assertEquals("2026-11-06 08:00", r.nextRepeat(LocalDateTime.parse("2026-11-05T09:00"))!!.dateTime)
        assertEquals("2026-11-05 08:00", r.nextRepeat(LocalDateTime.parse("2026-11-05T07:00"))!!.dateTime)
    }

    @Test
    fun ringingKeepsTheDateAndTickingMovesItOnce() {
        val r = Reminder(title = "Water", dateTime = "2026-11-05 09:00", recurrence = "FREQ=DAILY")
        // Rang at 9:00, still today: stays put (overdue until ticked).
        assertNull(r.caughtUp(LocalDateTime.parse("2026-11-05T09:05")))
        // Ticked at 9:05: on to tomorrow, not the day after.
        assertEquals("2026-11-06 09:00", r.nextRepeat(LocalDateTime.parse("2026-11-05T09:05"))!!.dateTime)
        // Never ticked, phone/computer off for days: catches up to the latest due date and rings for it.
        assertEquals("2026-11-08 09:00", r.caughtUp(LocalDateTime.parse("2026-11-08T10:00"))!!.dateTime)
        assertEquals("2026-11-07 09:00", r.caughtUp(LocalDateTime.parse("2026-11-08T08:00"))!!.dateTime)
        // Series over: nothing to catch up to beyond its last date.
        val two = r.copy(recurrence = "FREQ=DAILY;COUNT=2")
        assertEquals("2026-11-06 09:00", two.caughtUp(LocalDateTime.parse("2026-11-20T10:00"))!!.dateTime)
    }

    @Test
    fun completingARepeatingTodoLeavesADoneCopy() = runBlocking {
        val repo = TodoRepository(db.todoDao(), db.todoSubtaskDao())
        val id = repo.addTodo(Todo(title = "Trash", dueDate = "2099-01-06", recurrence = "FREQ=WEEKLY"))
        repo.setCompleted(db.todoDao().getTodoByIdRaw(id)!!, true)
        val all = db.todoDao().getAllActiveTodos().first()
        val open = all.single { !it.isCompleted }
        val done = all.single { it.isCompleted }
        assertEquals(id, open.id)
        assertEquals("2099-01-13", open.dueDate)
        assertEquals("", done.recurrence)
        assertTrue(done.syncId != open.syncId)
    }

    @Test
    fun tickingARepeatingReminderMovesIt() = runBlocking {
        val repo = ReminderRepository(db.reminderDao())
        val id = repo.addReminder(Reminder(title = "Call", dateTime = "2099-01-06 18:00", recurrence = "FREQ=DAILY"))
        repo.updateReminder(db.reminderDao().getReminderByIdRaw(id)!!.copy(isCompleted = true))
        val now = db.reminderDao().getReminderByIdRaw(id)!!
        assertEquals("2099-01-07 18:00", now.dateTime)
        assertEquals(false, now.isCompleted)
    }

    @Test
    fun repeatingEventsShowOnEveryDate() = runBlocking {
        val repo = CalendarEventRepository(db.calendarEventDao(), db.eventReminderDao())
        repo.addEvent(CalendarEvent(title = "Class", startDate = "2026-11-02", endDate = "2026-11-02", startTime = "10:00",
            endTime = "11:00", recurrence = "FREQ=WEEKLY;BYDAY=MO,WE", recurrenceExceptions = "2026-11-11"))
        repo.addEvent(CalendarEvent(title = "Trip", startDate = "2026-11-20", endDate = "2026-11-22"))
        val nov = repo.getEventsInRange("2026-11-09", "2026-11-22").first()
        assertEquals(listOf("2026-11-09", "2026-11-16", "2026-11-18", "2026-11-20"), nov.map { it.startDate })
        assertEquals("2026-11-16", nov[1].occurrenceDate)
        assertNull(nov.last().occurrenceDate)
        // A multi-day weekly series overlapping the window from before it.
        val weekend = CalendarEvent(title = "Away", startDate = "2026-11-06", endDate = "2026-11-08", recurrence = "FREQ=WEEKLY")
        assertEquals(listOf("2026-11-06"), listOf(weekend).expandForRange("2026-11-08", "2026-11-12").map { it.startDate })
        assertEquals(d("2026-11-13"), weekend.nextOccurrence(d("2026-11-09")))
    }
}
