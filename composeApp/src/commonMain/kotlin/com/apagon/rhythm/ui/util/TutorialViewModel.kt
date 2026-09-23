package com.apagon.rhythm.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Thin wrapper so any screen can read/set contextual-tutorial "seen" flags via [androidx.hilt.navigation.compose.koinViewModel]
 * without threading [ThemePreferences] through its own parent ViewModel — same pattern as [PaywallViewModel]. */
class TutorialViewModel constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {
    val hasSeenHabitCreationTutorial: Flow<Boolean> = themePreferences.hasSeenHabitCreationTutorial
    val hasSeenNotesTemplateTutorial: Flow<Boolean> = themePreferences.hasSeenNotesTemplateTutorial
    val hasSeenChecklistHabitTutorial: Flow<Boolean> = themePreferences.hasSeenChecklistHabitTutorial
    val hasSeenSwipeGestureTutorial: Flow<Boolean> = themePreferences.hasSeenSwipeGestureTutorial

    fun setHasSeenHabitCreationTutorial(seen: Boolean) {
        viewModelScope.launch { themePreferences.setHasSeenHabitCreationTutorial(seen) }
    }
    fun setHasSeenNotesTemplateTutorial(seen: Boolean) {
        viewModelScope.launch { themePreferences.setHasSeenNotesTemplateTutorial(seen) }
    }
    fun setHasSeenChecklistHabitTutorial(seen: Boolean) {
        viewModelScope.launch { themePreferences.setHasSeenChecklistHabitTutorial(seen) }
    }
    fun setHasSeenSwipeGestureTutorial(seen: Boolean) {
        viewModelScope.launch { themePreferences.setHasSeenSwipeGestureTutorial(seen) }
    }
}
