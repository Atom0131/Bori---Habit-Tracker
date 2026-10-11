package com.apagon.rhythm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A day on which at least one habit was done — the permanent record behind the Today card's day
 * counter, which never resets. Mirrors the Android app's `ActiveDay` (DB v50 there).
 *
 * `habit_completions` is not permanent: purging a deleted habit cascades away its completions, which
 * would lower the count. This table has no foreign key, so nothing cascades into it. SQLite triggers
 * on `habit_completions` fill it ([com.apagon.rhythm.data.db.ActiveDayTriggers]), so every write
 * path feeds it. Backed up as `activeDays`; carried on a full sync.
 *
 * Only `DesktopHabitDatabase` declares it — see [com.apagon.rhythm.data.db.ActiveDayDao].
 */
@Entity(tableName = "active_days")
data class ActiveDay(
    /** ISO date, "yyyy-MM-dd" — the same format as `HabitCompletion.dateCompleted`. */
    @PrimaryKey val date: String
)
