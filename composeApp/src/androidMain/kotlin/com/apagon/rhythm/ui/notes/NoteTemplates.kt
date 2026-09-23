package com.apagon.rhythm.ui.notes

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class NoteTemplate(val label: String) {
    object Blank        : NoteTemplate("Blank")
    object MeetingNotes : NoteTemplate("Meeting Notes")
    object BookNotes    : NoteTemplate("Book Notes")
    object WeeklyPlan   : NoteTemplate("Weekly Plan")
    object LectureNotes : NoteTemplate("Lecture Notes")
    object Brainstorm   : NoteTemplate("Brainstorm")

    companion object {
        val all = listOf(Blank, MeetingNotes, BookNotes, WeeklyPlan, LectureNotes, Brainstorm)
        fun fromName(name: String?) = all.firstOrNull { it.label == name } ?: Blank
    }
}

fun NoteTemplate.buildBlocks(): List<NoteBlock> {
    val today = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
    val cal = Calendar.getInstance()
    // Sunday-anchored week start (matches app-wide day order)
    val daysToSunday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, -daysToSunday)
    val weekStart = SimpleDateFormat("MMMM d", Locale.getDefault()).format(cal.time)

    return when (this) {
        NoteTemplate.Blank -> listOf(NoteBlock())

        // ── MEETING NOTES ─────────────────────────────────────────────────────
        // Purpose: capture decisions and accountability, not just discussion.
        // Structure: logistics → agenda → live notes → outputs (decisions + actions).
        NoteTemplate.MeetingNotes -> listOf(
            NoteBlock(type = BlockType.HEADER, content = "Meeting — $today"),
            NoteBlock(type = BlockType.TEXT,   content = "📋 Subject:"),
            NoteBlock(type = BlockType.TEXT,   content = "📍 Location / Link:"),
            NoteBlock(type = BlockType.TEXT,   content = "⏰ Time:        Duration:"),
            NoteBlock(type = BlockType.TEXT,   content = "👤 Organizer:"),
            NoteBlock(type = BlockType.TEXT,   content = "👥 Attendees:"),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "🎯 Purpose — one sentence on why we're meeting"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.TEXT,   content = "📌 Desired outcome by end of meeting:"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "🗂 Agenda"),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "📝 Discussion Notes"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "✅ Decisions  (what was agreed, not discussed)"),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Decision:   Rationale:"),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Decision:   Rationale:"),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "⚡ Action Items  (owner · due date)"),
            NoteBlock(type = BlockType.CHECKLIST, content = "  ·  owner:  ·  due:"),
            NoteBlock(type = BlockType.CHECKLIST, content = "  ·  owner:  ·  due:"),
            NoteBlock(type = BlockType.CHECKLIST, content = "  ·  owner:  ·  due:"),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "🅿️ Parking Lot  (ideas to revisit later)"),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "🔁 Next meeting:        Agenda preview:")
        )

        // ── BOOK NOTES ────────────────────────────────────────────────────────
        // Purpose: extract durable insight from a book, not just summaries.
        // Structure: book card → chapter-by-chapter → synthesis → so-what.
        NoteTemplate.BookNotes -> listOf(
            NoteBlock(type = BlockType.HEADER, content = "Book Notes"),
            NoteBlock(type = BlockType.TEXT,   content = "📖 Title:"),
            NoteBlock(type = BlockType.TEXT,   content = "✍️ Author:"),
            NoteBlock(type = BlockType.TEXT,   content = "📂 Genre / Category:"),
            NoteBlock(type = BlockType.TEXT,   content = "📅 Started:        Finished:"),
            NoteBlock(type = BlockType.TEXT,   content = "⭐ Rating:  / 5   Would recommend to:"),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "🪞 One-sentence premise of the book:"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.HEADER, content = "Chapter 1 — (title)  ·  pp. "),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Main argument:"),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Key example / story:"),
            NoteBlock(type = BlockType.QUOTE,  content = "Standout quote  — p."),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.HEADER, content = "Chapter 2 — (title)  ·  pp. "),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Main argument:"),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Key example / story:"),
            NoteBlock(type = BlockType.QUOTE,  content = "Standout quote  — p."),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.HEADER, content = "Chapter 3 — (title)  ·  pp. "),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Main argument:"),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Key example / story:"),
            NoteBlock(type = BlockType.QUOTE,  content = "Standout quote  — p."),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "🔑 3 Big Ideas  (book-wide themes)"),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "💭 My take — what I agree / disagree with:"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.TEXT,   content = "🔗 How this connects to something I already know:"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "⚡ What I'll actually do because of this book:"),
            NoteBlock(type = BlockType.CHECKLIST, content = ""),
            NoteBlock(type = BlockType.CHECKLIST, content = "")
        )

        // ── WEEKLY PLAN ───────────────────────────────────────────────────────
        // Purpose: a planner, not just a goal list — tasks live inside each day.
        // Structure: theme + outcomes → day-by-day slots → end-of-week retrospective.
        NoteTemplate.WeeklyPlan -> {
            val dayFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            fun dayLabel(offset: Int) = dayFmt.format(
                (cal.clone() as Calendar).also { it.add(Calendar.DAY_OF_YEAR, offset) }.time
            )
            listOf(
                NoteBlock(type = BlockType.HEADER, content = "Week of $weekStart"),
                NoteBlock(type = BlockType.TEXT,   content = "🧭 Weekly theme / intention:"),
                NoteBlock(type = BlockType.TEXT,   content = ""),
                NoteBlock(type = BlockType.TEXT,   content = "🏆 Top 3 outcomes I need by Friday:"),
                NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
                NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
                NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
                NoteBlock(type = BlockType.TEXT,   content = "🔁 Habit focus this week:"),
                NoteBlock(type = BlockType.BULLET_LIST, content = ""),
                NoteBlock(type = BlockType.DIVIDER),
                NoteBlock(type = BlockType.HEADER, content = dayLabel(0)),  // Sunday
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.HEADER, content = dayLabel(1)),  // Monday
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.HEADER, content = dayLabel(2)),  // Tuesday
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.HEADER, content = dayLabel(3)),  // Wednesday
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.HEADER, content = dayLabel(4)),  // Thursday
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.HEADER, content = dayLabel(5)),  // Friday
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.HEADER, content = dayLabel(6)),  // Saturday
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.CHECKLIST, content = ""),
                NoteBlock(type = BlockType.DIVIDER),
                NoteBlock(type = BlockType.TEXT,   content = "🎉 Wins this week:"),
                NoteBlock(type = BlockType.BULLET_LIST, content = ""),
                NoteBlock(type = BlockType.BULLET_LIST, content = ""),
                NoteBlock(type = BlockType.TEXT,   content = "📚 What I learned:"),
                NoteBlock(type = BlockType.TEXT,   content = ""),
                NoteBlock(type = BlockType.TEXT,   content = "🚀 Carry into next week:"),
                NoteBlock(type = BlockType.CHECKLIST, content = "")
            )
        }

        // ── LECTURE NOTES ─────────────────────────────────────────────────────
        // Purpose: Cornell-style study notes — capture during lecture, review after.
        // Structure: metadata → live notes (fast, messy) → processed recall questions
        //            → key terms → 3-sentence summary → exam prep checklist.
        NoteTemplate.LectureNotes -> listOf(
            NoteBlock(type = BlockType.HEADER, content = "Lecture — $today"),
            NoteBlock(type = BlockType.TEXT,   content = "📚 Course:"),
            NoteBlock(type = BlockType.TEXT,   content = "👨‍🏫 Professor:"),
            NoteBlock(type = BlockType.TEXT,   content = "🔢 Week / Lecture #:"),
            NoteBlock(type = BlockType.TEXT,   content = "📖 Textbook reading:  pp. "),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "🎯 Objectives — what should I know by the end?"),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.HEADER, content = "📝 Notes  (write fast, edit later)"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.HEADER, content = "❓ Recall Questions  (cover notes, answer these)"),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.HEADER, content = "🔤 Key Terms & Definitions"),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Term: — Definition:"),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Term: — Definition:"),
            NoteBlock(type = BlockType.BULLET_LIST, content = "Term: — Definition:"),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "📌 3-Sentence Summary  (in my own words)"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "🗓 Exam / Assignment due:"),
            NoteBlock(type = BlockType.CHECKLIST, content = "Re-read notes within 24 h"),
            NoteBlock(type = BlockType.CHECKLIST, content = "Make flashcards for key terms"),
            NoteBlock(type = BlockType.CHECKLIST, content = "Answer recall questions from memory"),
            NoteBlock(type = BlockType.CHECKLIST, content = "Review before exam")
        )

        // ── BRAINSTORM ────────────────────────────────────────────────────────
        // Purpose: diverge-then-converge — quantity first, quality second.
        // Structure: problem framing + constraints → wild dump → cluster →
        //            score top ideas → choose direction → immediate actions.
        NoteTemplate.Brainstorm -> listOf(
            NoteBlock(type = BlockType.HEADER, content = "Brainstorm — $today"),
            NoteBlock(type = BlockType.TEXT,   content = "❓ Problem / Question to solve:"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.TEXT,   content = "📋 Constraints  (budget, timeline, must-haves):"),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.HEADER, content = "🌊 Diverge — quantity over quality, no filtering"),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.HEADER, content = "🔍 Cluster — group similar ideas"),
            NoteBlock(type = BlockType.TEXT,   content = "Cluster A:"),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.TEXT,   content = "Cluster B:"),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.BULLET_LIST, content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.HEADER, content = "⚡ Converge — top 3 ideas ranked"),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = "  — why: "),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = "  — why: "),
            NoteBlock(type = BlockType.NUMBERED_LIST, content = "  — why: "),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "🎯 Chosen direction:"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.TEXT,   content = "Why this one over the others:"),
            NoteBlock(type = BlockType.TEXT,   content = ""),
            NoteBlock(type = BlockType.DIVIDER),
            NoteBlock(type = BlockType.TEXT,   content = "🚀 Next 3 actions  (do this week)"),
            NoteBlock(type = BlockType.CHECKLIST, content = ""),
            NoteBlock(type = BlockType.CHECKLIST, content = ""),
            NoteBlock(type = BlockType.CHECKLIST, content = "")
        )
    }
}
