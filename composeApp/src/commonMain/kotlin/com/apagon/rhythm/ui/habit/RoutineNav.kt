package com.apagon.rhythm.ui.habit

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * "Open this habit's routine." The sidebar card and the habit rows ask here; Today (switched to its
 * list) answers by showing the player in its right-hand pane. The phone's equivalent is the
 * `routine/{habitId}` route that its notification and rows navigate to.
 */
object RoutineNav {
    val request = MutableStateFlow<Long?>(null)

    fun open(habitId: Long) {
        request.value = habitId
    }
}
