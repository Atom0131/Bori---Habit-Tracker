package com.apagon.rhythm.ui.journal

import com.apagon.rhythm.platform.PhotoStorage

import com.apagon.rhythm.platform.PurchaseLauncher
import com.apagon.rhythm.core.time.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.JournalRepository
import com.apagon.rhythm.data.repository.LockType
import com.apagon.rhythm.data.repository.SecurityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.apagon.rhythm.core.json.JSONArray
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel constructor(
    private val photoStorage: PhotoStorage,
    private val journalRepository: JournalRepository,
    private val habitRepository: HabitRepository,
    private val securityRepository: SecurityRepository,
    private val themePreferences: ThemePreferences,
    private val purchaseLauncher: PurchaseLauncher
) : ViewModel() {

    val isPro: StateFlow<Boolean> = themePreferences.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun startBillingFlow(productId: String = PurchaseLauncher.PRO_MONTHLY_ID) {
        purchaseLauncher.launchPurchase(productId)
    }

    private val today = LocalDate.now()
    private val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Security states
    val lockType: StateFlow<LockType> = securityRepository.lockType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockType.NONE)

    val journalPin: StateFlow<String?> = securityRepository.journalPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val journalPassword: StateFlow<String?> = securityRepository.journalPassword
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val biometricEnabled: StateFlow<Boolean> = securityRepository.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = combine(_isLocked, lockType) { locked, type ->
        locked && type != LockType.NONE
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // Automatically lock if any lock type is configured
        viewModelScope.launch {
            securityRepository.lockType.collect { type ->
                if (type != LockType.NONE) _isLocked.value = true
            }
        }
    }

    fun unlock(credential: String): Boolean {
        val type = lockType.value
        val correctCredential = when (type) {
            LockType.PIN -> journalPin.value
            LockType.PASSWORD -> journalPassword.value
            LockType.NONE -> null
        }
        
        return if (correctCredential != null && correctCredential == credential) {
            _isLocked.value = false
            true
        } else {
            false
        }
    }

    fun unlockWithBiometrics() {
        if (lockType.value != LockType.NONE) {
            _isLocked.value = false
        }
    }

    fun lock() { 
        if (lockType.value != LockType.NONE) {
            _isLocked.value = true 
        }
    }

    fun setPin(pin: String?) {
        viewModelScope.launch { securityRepository.setJournalPin(pin) }
    }

    fun setPassword(password: String?) {
        viewModelScope.launch { securityRepository.setJournalPassword(password) }
    }

    fun clearAllLocks() {
        viewModelScope.launch { securityRepository.clearAllLocks() }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { securityRepository.setBiometricEnabled(enabled) }
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _searchQuery.value = "" // Clear search when date changes
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    // Display entries (either for date or search results)
    val displayEntries: StateFlow<List<JournalEntry>> = combine(
        _selectedDate,
        _searchQuery
    ) { date, query ->
        date to query
    }.flatMapLatest { (date, query) ->
        if (query.isBlank()) {
            journalRepository.getEntriesForDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
        } else {
            journalRepository.searchEntries(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // For backward compatibility or specific usage, keep entriesForDate if needed,
    // but we'll primarily use displayEntries in the UI.
    val entriesForDate: StateFlow<List<JournalEntry>> = _selectedDate
        .flatMapLatest { date ->
            journalRepository.getEntriesForDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // The general daily entry (habitId = null) for selected date
    val dailyEntry: StateFlow<JournalEntry?> = entriesForDate
        .map { entries -> entries.firstOrNull { it.habitId == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // All active habits (for habit-link dropdown in entry sheet)
    val activeHabits: StateFlow<List<Habit>> = habitRepository.getAllActiveHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Active habits paired with their habit-specific note for the selected date
    val habitNotes: StateFlow<List<Pair<Habit, JournalEntry?>>> =
        combine(
            habitRepository.getAllActiveHabits(),
            entriesForDate
        ) { habits, entries ->
            habits.map { habit ->
                habit to entries.firstOrNull { it.habitId == habit.id }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveGeneralEntry(content: String, existingEntry: JournalEntry?) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            if (existingEntry != null) {
                journalRepository.updateEntry(
                    existingEntry.copy(
                        content = content,
                        mood = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else if (content.isNotBlank()) {
                journalRepository.addEntry(
                    JournalEntry(
                        date = dateStr,
                        content = content,
                        mood = 0
                    )
                )
            }
        }
    }

    fun saveHabitNote(habit: Habit, content: String, existingEntry: JournalEntry?) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            if (existingEntry != null) {
                if (content.isBlank()) {
                    journalRepository.deleteEntry(existingEntry)
                } else {
                    journalRepository.updateEntry(
                        existingEntry.copy(
                            content = content,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } else if (content.isNotBlank()) {
                journalRepository.addEntry(
                    JournalEntry(
                        date = dateStr,
                        content = content,
                        habitId = habit.id
                    )
                )
            }
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch { journalRepository.deleteEntry(entry) }
    }

    private suspend fun copyPhotoToInternal(uriString: String): String? =
        photoStorage.importPhoto(uriString, "journal/images")

    fun saveEntry(
        date: String,
        title: String,
        content: String,
        mood: Int,
        feelings: List<String>,
        tags: List<String>,
        photoUris: List<String>,
        habitId: Long?,
        existingEntry: JournalEntry? = null,
        onSuccess: () -> Unit = {},
        onLimitExceeded: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (existingEntry == null && !isPro.value) {
                val entries = journalRepository.getEntriesForDate(date).first()
                if (entries.size >= 3) {
                    onLimitExceeded("Upgrade to Pro to add more than 3 journal entries per day!")
                    return@launch
                }
            }

            val processedUris = photoUris.map { uri ->
                if (uri.startsWith("content://")) copyPhotoToInternal(uri) ?: uri
                else uri
            }
            val tagsJson = JSONArray(tags).toString()
            val photosJson = JSONArray(processedUris).toString()
            val feelingsJson = JSONArray(feelings).toString()
            if (existingEntry != null) {
                journalRepository.updateEntry(
                    existingEntry.copy(
                        title = title,
                        content = content,
                        mood = mood,
                        tags = tagsJson,
                        photoUris = photosJson,
                        feelings = feelingsJson,
                        habitId = habitId,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else if (title.isNotBlank() || content.isNotBlank() || mood != 0 || feelings.isNotEmpty()) {
                journalRepository.addEntry(
                    JournalEntry(
                        date = date,
                        title = title,
                        content = content,
                        mood = mood,
                        tags = tagsJson,
                        photoUris = photosJson,
                        feelings = feelingsJson,
                        habitId = habitId
                    )
                )
            }
            onSuccess()
        }
    }
}
