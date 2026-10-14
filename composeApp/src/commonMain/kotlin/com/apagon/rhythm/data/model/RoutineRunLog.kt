package com.apagon.rhythm.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One finished run of a routine, for its history. Written when a run leaves its last step (see
 * `RoutineRunner`, the same table as the phone's), updated if the summary's "Mark routine done anyway" is used afterwards. A run is
 * identified by its habit and start instant, which is what makes that update an upsert.
 */
@Entity(
    tableName = "routine_runs",
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["habitId", "startedAt"], unique = true)]
)
data class RoutineRunLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    /** The day the run ticked steps on (yyyy-MM-dd). */
    val date: String,
    val startedAt: Long,
    val finishedAt: Long,
    val stepsDone: Int,
    val stepsSkipped: Int,
    val stepsTotal: Int,
    /** The sum of the steps' planned times, to set against finishedAt - startedAt. */
    val plannedSeconds: Int
)
