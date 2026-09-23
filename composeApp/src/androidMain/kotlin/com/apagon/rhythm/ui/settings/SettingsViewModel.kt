package com.apagon.rhythm.ui.settings

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.backup.BackupManager
import com.apagon.rhythm.data.billing.BillingRepository
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.preferences.DarkReadability
import com.apagon.rhythm.data.preferences.ThemeMode
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.repository.DeviceCalendarIntegration
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.TodoRepository
import com.apagon.rhythm.data.model.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


sealed class BackupState {
    object Idle : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}
class SettingsViewModel constructor(
    private val context: Context,
    val themePreferences: ThemePreferences,
    private val backupManager: BackupManager,
    private val billingRepository: BillingRepository,
    private val habitRepository: HabitRepository,
    private val todoRepository: TodoRepository,
    private val calendarIntegrationRepository: DeviceCalendarIntegration,
) : ViewModel() {

    val enabledCalendars: StateFlow<Set<String>> = themePreferences.enabledCalendars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _availableCalendars = MutableStateFlow<List<com.apagon.rhythm.data.repository.DeviceCalendar>>(emptyList())
    val availableCalendars: StateFlow<List<com.apagon.rhythm.data.repository.DeviceCalendar>> = _availableCalendars

    fun refreshAvailableCalendars() {
        viewModelScope.launch {
            _availableCalendars.value = calendarIntegrationRepository.fetchAvailableCalendars()
        }
    }

    fun toggleCalendar(id: String) {
        viewModelScope.launch {
            val current = themePreferences.enabledCalendars.first().toMutableSet()
            if (current.contains(id)) current.remove(id)
            else current.add(id)
            themePreferences.setEnabledCalendars(current)
        }
    }

    fun setAllCalendars(ids: Set<String>) {
        viewModelScope.launch {
            themePreferences.setEnabledCalendars(ids)
        }
    }

    fun startBillingFlow(activity: Activity, productId: String = BillingRepository.PRO_MONTHLY_ID) {
        billingRepository.launchBillingFlow(activity, productId)
    }

    fun startVipPromoFlow(activity: Activity) {
        billingRepository.launchVipPromoFlow(activity)
    }

    fun manageSubscriptionsIntent(productId: String = BillingRepository.PRO_ANNUAL_ID) =
        billingRepository.manageSubscriptionsIntent(productId)

    fun queryPurchases() {
        billingRepository.queryPurchases()
    }

    val archivedHabits: StateFlow<List<Habit>> = habitRepository.getAllArchivedHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedTodos: StateFlow<List<Todo>> = todoRepository.getArchivedTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unarchiveHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.updateHabit(habit.copy(isActive = true))
        }
    }

    fun unarchiveTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.archiveTodo(todo, archive = false)
        }
    }

    val userName     = themePreferences.userName
    val userNickname = themePreferences.userNickname
    val userAge      = themePreferences.userAge
    val userPronouns = themePreferences.userPronouns

    val themeMode = themePreferences.themeMode
    val amoledMode = themePreferences.amoledMode
    val darkReadability = themePreferences.darkReadability
    val accentColorIndex = themePreferences.accentColorIndex
    val accentColorArgb = themePreferences.accentColorArgb
    val isPro = themePreferences.isPro
    val profilePictureUri = themePreferences.profilePictureUri
    val swipeSectionsEnabled = themePreferences.swipeSectionsEnabled
    val homeViewCalendar: StateFlow<Boolean> = themePreferences.homeViewCalendar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val calendarListMode: StateFlow<Boolean> = themePreferences.calendarListMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val calendarIntegrationEnabled: StateFlow<Boolean> = themePreferences.calendarIntegrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreferences.setThemeMode(mode) }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setAmoledMode(enabled) }
    }

    fun setDarkReadability(mode: DarkReadability) {
        viewModelScope.launch { themePreferences.setDarkReadability(mode) }
    }

    fun setAccentColor(index: Int, argb: Int? = null) {
        viewModelScope.launch { themePreferences.setAccentColor(index, argb) }
    }

    fun setAccentColorIndex(index: Int) {
        setAccentColor(index, null)
    }

    fun setSwipeSectionsEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setSwipeSectionsEnabled(enabled) }
    }

    fun setUserName(name: String)         { viewModelScope.launch { themePreferences.setUserName(name) } }
    fun setUserNickname(nickname: String) { viewModelScope.launch { themePreferences.setUserNickname(nickname) } }
    fun setUserAge(age: String)           { viewModelScope.launch { themePreferences.setUserAge(age) } }
    fun setUserPronouns(pronouns: String) { viewModelScope.launch { themePreferences.setUserPronouns(pronouns) } }
    fun setProfilePictureUri(uri: String?) { viewModelScope.launch { themePreferences.setProfilePictureUri(uri) } }

    fun setHomeViewCalendar(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setHomeViewCalendar(enabled) }
    }

    fun setCalendarListMode(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setCalendarListMode(enabled) }
    }

    fun setCalendarIntegrationEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setCalendarIntegrationEnabled(enabled) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = backupManager.exportToJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                _backupState.value = BackupState.Success("Backup exported successfully")
            } catch (e: Exception) {
                _backupState.value = BackupState.Error("Export failed: ${e.message}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: throw IllegalStateException("Could not read backup file")
                }
                withContext(Dispatchers.IO) {
                    backupManager.importFromJson(json)
                }
                _backupState.value = BackupState.Success("Backup restored successfully!")
                android.widget.Toast.makeText(
                    context, "Backup restored successfully!", android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                _backupState.value = BackupState.Error("Import failed: ${e.message}")
            }
        }
    }

    fun clearBackupState() {
        _backupState.value = BackupState.Idle
    }
}
