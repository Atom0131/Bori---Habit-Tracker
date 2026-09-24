package com.apagon.rhythm.data.sync

/** Summary of one sync round, for a real on-screen confirmation rather than silent success. */
data class SyncResult(
    val habitsInserted: Int,
    val habitsUpdated: Int,
    val completionsInserted: Int,
    val completionsUpdated: Int
)
