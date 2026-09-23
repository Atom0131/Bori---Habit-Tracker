package com.apagon.rhythm.ui.notes

import com.apagon.rhythm.platform.PurchaseLauncher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.repository.NotesRepository
import com.apagon.rhythm.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotebookWithCount(val notebook: Notebook, val count: Int)
class NotesViewModel constructor(
    private val repository: NotesRepository,
    themePreferences: ThemePreferences,
    private val purchaseLauncher: PurchaseLauncher
) : ViewModel() {

    val notebooks: StateFlow<List<Notebook>> = repository.getAllNotebooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notebooksWithCount: StateFlow<List<NotebookWithCount>> = combine(
        repository.getAllNotebooks(),
        repository.getNoteCounts()
    ) { notebooks, counts ->
        val countMap = counts.associate { it.notebookId to it.count }
        notebooks.map { nb -> NotebookWithCount(nb, countMap[nb.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPro: StateFlow<Boolean> = themePreferences.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun startBillingFlow(productId: String = PurchaseLauncher.PRO_MONTHLY_ID) {
        purchaseLauncher.launchPurchase(productId)
    }

    fun addNotebook(name: String, colorIndex: Int, colorArgb: Int? = null) {
        viewModelScope.launch {
            repository.insertNotebook(
                Notebook(
                    name = name,
                    colorIndex = colorIndex,
                    colorArgb = colorArgb
                )
            )
        }
    }

    fun updateNotebook(notebook: Notebook) {
        viewModelScope.launch {
            repository.updateNotebook(notebook)
        }
    }

    fun deleteNotebook(notebook: Notebook) {
        viewModelScope.launch {
            repository.deleteNotebook(notebook)
        }
    }
}
