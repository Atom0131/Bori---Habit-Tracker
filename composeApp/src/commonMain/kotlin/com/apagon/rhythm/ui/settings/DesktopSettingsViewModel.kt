package com.apagon.rhythm.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.backup.BackupManaging
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.preferences.CrystalBackground
import com.apagon.rhythm.data.preferences.CrystalMesh
import com.apagon.rhythm.data.preferences.CrystalStyle
import com.apagon.rhythm.data.preferences.DarkReadability
import com.apagon.rhythm.data.preferences.ThemeMode
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.preferences.ThemeStyle
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.TodoRepository
import com.apagon.rhythm.platform.FilePicker
import com.apagon.rhythm.platform.PhotoStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Desktop counterpart to androidMain's SettingsViewModel.kt (Stage 11).
// Ported wholesale: every ThemePreferences-forwarding StateFlow/setter, plus
// the archived-habits/todos state. Dropped entirely: all BillingRepository
// calls (desktop is unconditionally Pro, Stage 6) and the Android
// availableCalendars/toggleCalendar/setAllCalendars calendar-list state —
// DesktopDeviceCalendarIntegration always returns an empty list, so there's
// nothing to toggle; DesktopSettingsScreen just shows a static empty state
// instead of wiring dead list plumbing. exportBackup/importBackup call
// FilePicker's dialog methods directly rather than taking a Uri (no
// ContentResolver on desktop — this is a real file path).
sealed class DesktopBackupState {
    object Idle : DesktopBackupState()
    data class Success(val message: String) : DesktopBackupState()
    data class Error(val message: String) : DesktopBackupState()
}

class DesktopSettingsViewModel(
    val themePreferences: ThemePreferences,
    private val habitRepository: HabitRepository,
    private val todoRepository: TodoRepository,
    private val backupManager: BackupManaging,
    private val filePicker: FilePicker,
    private val photoStorage: PhotoStorage,
) : ViewModel() {

    val userName = themePreferences.userName
    val userNickname = themePreferences.userNickname
    val userAge = themePreferences.userAge
    val userPronouns = themePreferences.userPronouns
    val profilePictureUri = themePreferences.profilePictureUri

    val themeMode = themePreferences.themeMode
    val amoledMode = themePreferences.amoledMode
    val darkReadability = themePreferences.darkReadability
    val accentColorIndex = themePreferences.accentColorIndex
    val accentColorArgb = themePreferences.accentColorArgb
    val swipeSectionsEnabled = themePreferences.swipeSectionsEnabled

    // Crystal theme settings — the data layer (ThemePreferences) and the rendering kit
    // (Crystal.kt/CrystalMeshField.kt) were already fully ported; this screen just never exposed
    // them. See ref_notes for the layout-parity round these were activated in.
    val themeStyle: StateFlow<ThemeStyle> = themePreferences.themeStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeStyle.MATERIAL3)
    val crystalStyle: StateFlow<CrystalStyle> = themePreferences.crystalStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CrystalStyle.TINTED)
    val crystalIntensity: StateFlow<Float> = themePreferences.crystalIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.5f)
    val crystalBackground: StateFlow<CrystalBackground> = themePreferences.crystalBackground
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CrystalBackground.MESH)
    val crystalMesh: StateFlow<CrystalMesh> = themePreferences.crystalMesh
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CrystalMesh.AURORA)
    val crystalBackgroundColorIndex: StateFlow<Int> = themePreferences.crystalBackgroundColorIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val crystalBackgroundColorArgb: StateFlow<Int?> = themePreferences.crystalBackgroundColorArgb
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val crystalMeshCustomArgb: StateFlow<Int?> = themePreferences.crystalMeshCustomArgb
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val crystalMeshCustomChroma: StateFlow<Float> = themePreferences.crystalMeshCustomChroma
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreferences.DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA)

    fun setThemeStyle(style: ThemeStyle) {
        viewModelScope.launch { themePreferences.setThemeStyle(style) }
    }

    fun setCrystalStyle(style: CrystalStyle) {
        viewModelScope.launch { themePreferences.setCrystalStyle(style) }
    }

    fun previewCrystalIntensity(value: Float) {
        themePreferences.previewCrystalIntensity(value)
    }

    fun setCrystalIntensity(value: Float) {
        viewModelScope.launch { themePreferences.setCrystalIntensity(value) }
    }

    fun setCrystalBackground(background: CrystalBackground) {
        viewModelScope.launch { themePreferences.setCrystalBackground(background) }
    }

    fun setCrystalMesh(mesh: CrystalMesh) {
        viewModelScope.launch { themePreferences.setCrystalMesh(mesh) }
    }

    fun setCrystalBackgroundColor(index: Int, argb: Int? = null) {
        viewModelScope.launch { themePreferences.setCrystalBackgroundColor(index, argb) }
    }

    fun previewCrystalBackgroundColor(index: Int, argb: Int?) {
        themePreferences.previewCrystalBackgroundColor(index, argb)
    }

    fun cancelCrystalBackgroundColorPreview() {
        themePreferences.cancelCrystalBackgroundColorPreview()
    }

    fun previewAccentColor(index: Int, argb: Int?) {
        themePreferences.previewAccentColor(index, argb)
    }

    fun cancelAccentColorPreview() {
        themePreferences.cancelAccentColorPreview()
    }

    // Mirrors androidMain's SettingsViewModel.setCrystalMeshCustom: saving a custom field also
    // selects it (CUSTOM) and switches to Mesh mode, so "Save" in the dialog is the one action that
    // both configures and applies the field.
    fun setCrystalMeshCustom(argb: Int, chromaScale: Float) {
        viewModelScope.launch {
            themePreferences.setCrystalMeshCustom(argb, chromaScale)
            themePreferences.setCrystalMesh(CrystalMesh.CUSTOM)
            themePreferences.setCrystalBackground(CrystalBackground.MESH)
        }
    }

    fun previewCrystalMeshCustom(argb: Int, chromaScale: Float) {
        themePreferences.previewCrystalMeshCustom(argb, chromaScale)
    }

    fun cancelCrystalMeshCustomPreview() {
        themePreferences.cancelCrystalMeshCustomPreview()
    }
    val homeViewCalendar: StateFlow<Boolean> = themePreferences.homeViewCalendar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val calendarListMode: StateFlow<Boolean> = themePreferences.calendarListMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val calendarIntegrationEnabled: StateFlow<Boolean> = themePreferences.calendarIntegrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val runInBackground: StateFlow<Boolean> = themePreferences.runInBackground
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val archivedHabits: StateFlow<List<Habit>> = habitRepository.getAllArchivedHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val archivedTodos: StateFlow<List<Todo>> = todoRepository.getArchivedTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unarchiveHabit(habit: Habit) {
        viewModelScope.launch { habitRepository.updateHabit(habit.copy(isActive = true)) }
    }

    fun unarchiveTodo(todo: Todo) {
        viewModelScope.launch { todoRepository.archiveTodo(todo, archive = false) }
    }

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

    fun setSwipeSectionsEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setSwipeSectionsEnabled(enabled) }
    }

    fun setUserName(name: String) { viewModelScope.launch { themePreferences.setUserName(name) } }
    fun setUserNickname(nickname: String) { viewModelScope.launch { themePreferences.setUserNickname(nickname) } }
    fun setUserAge(age: String) { viewModelScope.launch { themePreferences.setUserAge(age) } }
    fun setUserPronouns(pronouns: String) { viewModelScope.launch { themePreferences.setUserPronouns(pronouns) } }
    fun setProfilePictureUri(uri: String?) { viewModelScope.launch { themePreferences.setProfilePictureUri(uri) } }

    fun pickAndSetProfilePicture() {
        viewModelScope.launch {
            val path = filePicker.pickImagePath() ?: return@launch
            val imported = photoStorage.importPhoto(path, "profile") ?: return@launch
            themePreferences.setProfilePictureUri(imported)
        }
    }

    fun setHomeViewCalendar(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setHomeViewCalendar(enabled) }
    }

    fun setCalendarListMode(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setCalendarListMode(enabled) }
    }

    fun setCalendarIntegrationEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setCalendarIntegrationEnabled(enabled) }
    }

    fun setRunInBackground(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setRunInBackground(enabled) }
    }

    private val _backupState = MutableStateFlow<DesktopBackupState>(DesktopBackupState.Idle)
    val backupState: StateFlow<DesktopBackupState> = _backupState

    fun exportBackup() {
        viewModelScope.launch {
            try {
                val path = filePicker.pickBackupExportPath("rhythm-backup.json") ?: return@launch
                backupManager.exportToPath(path)
                _backupState.value = DesktopBackupState.Success("Backup exported successfully")
            } catch (e: Exception) {
                _backupState.value = DesktopBackupState.Error("Export failed: ${e.message}")
            }
        }
    }

    fun importBackup() {
        viewModelScope.launch {
            try {
                val path = filePicker.pickBackupImportPath() ?: return@launch
                backupManager.importFromPath(path)
                _backupState.value = DesktopBackupState.Success("Backup restored successfully!")
            } catch (e: Exception) {
                _backupState.value = DesktopBackupState.Error("Import failed: ${e.message}")
            }
        }
    }

    fun clearBackupState() {
        _backupState.value = DesktopBackupState.Idle
    }
}
