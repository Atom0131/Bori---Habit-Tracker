package com.apagon.rhythm.data.sync

/** Summary of one sync round, for a real on-screen confirmation rather than silent success. */
data class SyncResult(
    val habitsInserted: Int,
    val habitsUpdated: Int,
    val completionsInserted: Int,
    val completionsUpdated: Int,
    /** Aggregate insert/update count across every other Stage 2 entity (todos, calendar events,
     * alarms, timers, reminders, notebooks, notes, journal entries, and their full-replace
     * children) — not broken out per-entity, matching the plan's "don't over-engineer v1" steer. */
    val otherInserted: Int = 0,
    val otherUpdated: Int = 0
)
