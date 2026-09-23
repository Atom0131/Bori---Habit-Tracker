package com.apagon.rhythm.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class ColorPickerViewModel constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val customColors: StateFlow<List<Int?>> = themePreferences.customColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(5) { null })

    fun saveCustomColor(index: Int, argb: Int) {
        viewModelScope.launch {
            themePreferences.setCustomColor(index, argb)
        }
    }

    fun removeCustomColor(index: Int) {
        viewModelScope.launch {
            themePreferences.setCustomColor(index, null)
        }
    }
}
