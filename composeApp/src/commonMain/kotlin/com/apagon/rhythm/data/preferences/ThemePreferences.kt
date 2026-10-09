package com.apagon.rhythm.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.apagon.rhythm.core.json.JSONObject
import com.apagon.rhythm.core.time.System


enum class ThemeMode { SYSTEM, LIGHT, DARK }

// Controls dark-mode surface elevation / contrast via materialkolor contrastLevel parameter.
// Standard=0.0 (M3 spec), Comfortable=0.3 (lifted surfaces), High=0.65 (max readability).
enum class DarkReadability { STANDARD, COMFORTABLE, HIGH }

// Mirrors the Android original's ThemeStyle/CrystalStyle/CrystalBackground/CrystalMesh —
// see that file's KDoc for the full design rationale. Kept here verbatim so a backup exported
// from either platform imports cleanly on the other.
enum class ThemeStyle { MATERIAL3, EXPRESSIVE, CRYSTAL }
enum class CrystalStyle { SHEER, TINTED }
enum class CrystalBackground { MESH, SOLID }
enum class CrystalMesh {
    AURORA, EMBER, VERDANT, MIST, ROSE,
    TIDE, INDIGO, ORCHID, DUNE, GRAPHITE,
    CUSTOM
}

class ThemePreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val AMOLED_MODE_KEY = booleanPreferencesKey("amoled_mode")
        val ACCENT_COLOR_KEY = intPreferencesKey("accent_color_index")
        val ACCENT_COLOR_ARGB_KEY = intPreferencesKey("accent_color_argb")
        val SWIPE_SECTIONS_KEY = booleanPreferencesKey("swipe_sections_enabled")
        val HOME_VIEW_CALENDAR_KEY = booleanPreferencesKey("home_view_calendar")
        // Desktop-only (no Android equivalent — the OS already handles background
        // scheduling there) — deliberately NOT read/written by exportPreferences/
        // importPreferences below, same reasoning SecurityRepository's PIN is kept
        // outside the backup: a value meaningless on the other platform shouldn't
        // round-trip through a cross-device backup file.
        val RUN_IN_BACKGROUND_KEY = booleanPreferencesKey("run_in_background")
        val CALENDAR_LIST_MODE_KEY = booleanPreferencesKey("calendar_list_mode")
        val USER_NAME_KEY      = stringPreferencesKey("user_name")
        val USER_NICKNAME_KEY  = stringPreferencesKey("user_nickname")
        val USER_AGE_KEY       = stringPreferencesKey("user_age")
        val USER_PRONOUNS_KEY  = stringPreferencesKey("user_pronouns")
        val IS_PRO_KEY         = booleanPreferencesKey("is_pro")
        val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
        val HAS_COMPLETED_INTERACTIVE_TUTORIAL_KEY = booleanPreferencesKey("has_completed_interactive_tutorial")
        val HAS_SEEN_HABIT_CREATION_TUTORIAL_KEY = booleanPreferencesKey("has_seen_habit_creation_tutorial")
        val HAS_SEEN_NOTES_TEMPLATE_TUTORIAL_KEY = booleanPreferencesKey("has_seen_notes_template_tutorial")
        val HAS_SEEN_CHECKLIST_HABIT_TUTORIAL_KEY = booleanPreferencesKey("has_seen_checklist_habit_tutorial")
        val HAS_SEEN_SWIPE_GESTURE_TUTORIAL_KEY = booleanPreferencesKey("has_seen_swipe_gesture_tutorial")
        val CUSTOM_COLORS_KEY = stringPreferencesKey("custom_colors")
        val PROFILE_PICTURE_URI_KEY = stringPreferencesKey("profile_picture_uri")
        val CALENDAR_INTEGRATION_KEY = booleanPreferencesKey("calendar_integration_enabled")
        val ENABLED_CALENDARS_KEY = stringSetPreferencesKey("enabled_calendar_ids")
        val NOTES_TOOLBAR_PINNED = booleanPreferencesKey("notes_toolbar_pinned")
        val DARK_READABILITY_KEY = stringPreferencesKey("dark_readability")
        val THEME_STYLE_KEY = stringPreferencesKey("theme_style")
        val CRYSTAL_STYLE_KEY = stringPreferencesKey("crystal_style")
        val CRYSTAL_INTENSITY_KEY = floatPreferencesKey("crystal_intensity")
        val CRYSTAL_BACKGROUND_KEY = stringPreferencesKey("crystal_background")
        val CRYSTAL_MESH_KEY = stringPreferencesKey("crystal_mesh")
        val CRYSTAL_BG_COLOR_KEY = intPreferencesKey("crystal_bg_color_index")
        val CRYSTAL_BG_COLOR_ARGB_KEY = intPreferencesKey("crystal_bg_color_argb")
        val CRYSTAL_MESH_CUSTOM_ARGB_KEY = intPreferencesKey("crystal_mesh_custom_argb")
        val CRYSTAL_MESH_CUSTOM_CHROMA_KEY = floatPreferencesKey("crystal_mesh_custom_chroma")

        /** Bumped by every setter whose key `exportPreferences()` includes (i.e. everything that
         * would cross devices in a sync blob) — see [touchPreferences]. Device-local-only setters
         * such as [setRunInBackground] deliberately do not bump this: the sync engine uses it to
         * decide whether the preferences blob changed since the last sync, and a key that is
         * stripped from that blob anyway (see `exportPreferencesForSync`) shouldn't make the engine
         * think otherwise. */
        val PREFERENCES_UPDATED_AT_KEY = longPreferencesKey("preferences_updated_at")

        /** Stage 3.5 — optional, default-OFF Markdown vault *file* sync (separate from Stage 2's
         * Note/Notebook *row* sync, which is always on). A local sync-behavior setting, not an
         * appearance/content setting — deliberately NOT included in [exportPreferences]/
         * [importPreferences] (so it never rides along in a portable backup) and therefore
         * automatically excluded from [exportPreferencesForSync] too, which is built from
         * [exportPreferences]. Each device opts in independently — this is never flipped on one
         * device by a sync from the other. See [setVaultFileSyncEnabled] for why its setter skips
         * [touchPreferences]. */
        val VAULT_FILE_SYNC_ENABLED_KEY = booleanPreferencesKey("vault_file_sync_enabled")

        /** Never part of the synced settings, so changing one must not stamp the timestamp. */
        private val DEVICE_LOCAL_KEYS: Set<Preferences.Key<*>> by lazy {
            setOf(IS_PRO_KEY, PROFILE_PICTURE_URI_KEY, RUN_IN_BACKGROUND_KEY,
                VAULT_FILE_SYNC_ENABLED_KEY, PREFERENCES_UPDATED_AT_KEY)
        }

        /** Index meaning "no colour — the cool neutral room". Distinct from 0, a real palette entry. */
        const val CRYSTAL_BG_NEUTRAL = -1

        /** Dead centre of whichever Crystal band is active. */
        const val DEFAULT_CRYSTAL_INTENSITY = 0.5f
        const val DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA = 1.0f
        const val MIN_CRYSTAL_MESH_CUSTOM_CHROMA = 0.18f
    }

    /** Last time any setter whose key `exportPreferences()` includes ran — see
     * [PREFERENCES_UPDATED_AT_KEY]. Used by the sync engine to decide whether the whole
     * preferences blob needs to go out this round. */
    val preferencesUpdatedAt: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PREFERENCES_UPDATED_AT_KEY] ?: 0L
    }

    /** Every setter below whose key is part of `exportPreferences()`'s set calls this instead of
     * `dataStore.edit` directly, so [PREFERENCES_UPDATED_AT_KEY] can never drift out of sync with
     * an actual exportable change. Device-local-only setters (e.g. [setRunInBackground]) call
     * `dataStore.edit` directly and deliberately skip this. */
    //
    // Stamps only when a synced value actually changes (2026-10-09). It used to stamp on every call,
    // so a brand-new desktop's untouched defaults carried a fresh timestamp and won its first sync
    // against a phone that had been set up for weeks (the phone's Crystal theme was replaced with
    // Material). Re-writing the same value, or touching only a [DEVICE_LOCAL_KEYS] key, now leaves
    // the timestamp alone, so an install nobody has customised stays at 0 and never wins.
    private suspend fun touchPreferences(forceStamp: Boolean = false, block: (MutablePreferences) -> Unit) {
        dataStore.edit { prefs ->
            val before = syncedValues(prefs)
            block(prefs)
            if (forceStamp || syncedValues(prefs) != before) {
                prefs[PREFERENCES_UPDATED_AT_KEY] = System.currentTimeMillis()
            }
        }
    }

    private fun syncedValues(prefs: Preferences): Map<Preferences.Key<*>, Any> =
        prefs.asMap().filterKeys { it !in DEVICE_LOCAL_KEYS }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name  -> ThemeMode.DARK
            else                 -> ThemeMode.SYSTEM
        }
    }

    val amoledMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AMOLED_MODE_KEY] ?: false
    }

    // Transient in-memory override while the custom accent picker is under the thumb — same
    // shape as crystalIntensityPreview below. `null` means "no preview in progress."
    private val accentPreview = MutableStateFlow<Pair<Int, Int?>?>(null)

    val accentColorIndex: Flow<Int> = combine(
        dataStore.data.map { prefs -> prefs[ACCENT_COLOR_KEY] ?: 0 },
        accentPreview
    ) { stored, preview -> preview?.first ?: stored }

    val accentColorArgb: Flow<Int?> = combine(
        dataStore.data.map { prefs -> prefs[ACCENT_COLOR_ARGB_KEY] },
        accentPreview
    ) { stored, preview -> if (preview != null) preview.second else stored }

    /** Called every drag frame from the custom accent picker — repaints the whole app live
     * without a DataStore write per frame. */
    fun previewAccentColor(index: Int, argb: Int?) {
        accentPreview.value = index to argb
    }

    /** Cancel/dismiss-without-selecting: revert to whatever was actually committed. */
    fun cancelAccentColorPreview() {
        accentPreview.value = null
    }

    /** Key-absence, not equality-with-default — otherwise someone deliberately choosing the same
     * value the default would have picked is indistinguishable from someone who never looked. Used
     * by `accentSeed` (Theme.kt) to decide whether a Crystal mesh field may supply the seed. */
    val accentColorIsDefault: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_KEY] == null && prefs[ACCENT_COLOR_ARGB_KEY] == null
    }

    val swipeSectionsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SWIPE_SECTIONS_KEY] ?: false
    }

    val homeViewCalendar: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[HOME_VIEW_CALENDAR_KEY] ?: false
    }

    val runInBackground: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[RUN_IN_BACKGROUND_KEY] ?: false
    }

    val calendarListMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CALENDAR_LIST_MODE_KEY] ?: false
    }

    suspend fun setHomeViewCalendar(enabled: Boolean) {
        touchPreferences { prefs ->
            prefs[HOME_VIEW_CALENDAR_KEY] = enabled
        }
    }

    // Desktop-only, deliberately NOT routed through touchPreferences (see RUN_IN_BACKGROUND_KEY's
    // own KDoc) — it is excluded from exportPreferences/importPreferences and from the sync blob,
    // so bumping the tracking key here would make the sync engine think something syncable changed.
    suspend fun setRunInBackground(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[RUN_IN_BACKGROUND_KEY] = enabled
        }
    }

    /** Off by default — a file-tree sync (partial writes, conflicting concurrent file edits,
     * orphaned assets) is a materially higher-risk mechanism than the row-level entity sync, so it
     * stays opt-in until proven. See [VAULT_FILE_SYNC_ENABLED_KEY]'s own KDoc for why it's excluded
     * from backup and from the preferences sync blob. */
    val vaultFileSyncEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[VAULT_FILE_SYNC_ENABLED_KEY] ?: false
    }

    /** Deliberately NOT routed through [touchPreferences] — same reasoning as
     * [setRunInBackground]: this key is never part of [exportPreferences]'s set, so bumping
     * [PREFERENCES_UPDATED_AT_KEY] here would make the sync engine think an exportable setting
     * changed when none did, triggering a needless preferences-blob resync. */
    suspend fun setVaultFileSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[VAULT_FILE_SYNC_ENABLED_KEY] = enabled
        }
    }

    suspend fun setCalendarListMode(enabled: Boolean) {
        touchPreferences { prefs ->
            prefs[CALENDAR_LIST_MODE_KEY] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        touchPreferences { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        touchPreferences { prefs ->
            prefs[AMOLED_MODE_KEY] = enabled
        }
    }

    suspend fun setAccentColor(index: Int, argb: Int? = null) {
        touchPreferences { prefs ->
            prefs[ACCENT_COLOR_KEY] = index
            if (argb != null) prefs[ACCENT_COLOR_ARGB_KEY] = argb
            else prefs.remove(ACCENT_COLOR_ARGB_KEY)
        }
        accentPreview.value = null
    }

    suspend fun setAccentColorIndex(index: Int) {
        setAccentColor(index, null)
    }

    suspend fun setSwipeSectionsEnabled(enabled: Boolean) {
        touchPreferences { prefs ->
            prefs[SWIPE_SECTIONS_KEY] = enabled
        }
    }

    val userName: Flow<String> = dataStore.data.map { it[USER_NAME_KEY] ?: "" }
    val userNickname: Flow<String> = dataStore.data.map { it[USER_NICKNAME_KEY] ?: "" }
    val userAge: Flow<String> = dataStore.data.map { it[USER_AGE_KEY] ?: "" }
    val userPronouns: Flow<String> = dataStore.data.map { it[USER_PRONOUNS_KEY] ?: "" }
    val isPro: Flow<Boolean> = dataStore.data.map { it[IS_PRO_KEY] ?: false }
    val hasSeenOnboarding: Flow<Boolean> = dataStore.data.map { it[HAS_SEEN_ONBOARDING_KEY] ?: false }
    val hasCompletedInteractiveTutorial: Flow<Boolean> = dataStore.data.map { it[HAS_COMPLETED_INTERACTIVE_TUTORIAL_KEY] ?: false }
    val hasSeenHabitCreationTutorial: Flow<Boolean> = dataStore.data.map { it[HAS_SEEN_HABIT_CREATION_TUTORIAL_KEY] ?: false }
    val hasSeenNotesTemplateTutorial: Flow<Boolean> = dataStore.data.map { it[HAS_SEEN_NOTES_TEMPLATE_TUTORIAL_KEY] ?: false }
    val hasSeenChecklistHabitTutorial: Flow<Boolean> = dataStore.data.map { it[HAS_SEEN_CHECKLIST_HABIT_TUTORIAL_KEY] ?: false }
    val hasSeenSwipeGestureTutorial: Flow<Boolean> = dataStore.data.map { it[HAS_SEEN_SWIPE_GESTURE_TUTORIAL_KEY] ?: false }
    val profilePictureUri: Flow<String?> = dataStore.data.map { it[PROFILE_PICTURE_URI_KEY] }

    val notesToolbarPinned: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOTES_TOOLBAR_PINNED] ?: false
    }

    suspend fun setNotesToolbarPinned(pinned: Boolean) {
        touchPreferences { it[NOTES_TOOLBAR_PINNED] = pinned }
    }

    val darkReadability: Flow<DarkReadability> = dataStore.data.map { prefs ->
        when (prefs[DARK_READABILITY_KEY]) {
            DarkReadability.COMFORTABLE.name -> DarkReadability.COMFORTABLE
            DarkReadability.HIGH.name        -> DarkReadability.HIGH
            else                             -> DarkReadability.STANDARD
        }
    }

    suspend fun setDarkReadability(mode: DarkReadability) {
        touchPreferences { it[DARK_READABILITY_KEY] = mode.name }
    }

    // Defaults to MATERIAL3 — an unknown or absent value must land on today's look.
    val themeStyle: Flow<ThemeStyle> = dataStore.data.map { prefs ->
        when (prefs[THEME_STYLE_KEY]) {
            ThemeStyle.EXPRESSIVE.name -> ThemeStyle.EXPRESSIVE
            ThemeStyle.CRYSTAL.name    -> ThemeStyle.CRYSTAL
            else                       -> ThemeStyle.MATERIAL3
        }
    }

    suspend fun setThemeStyle(style: ThemeStyle) {
        touchPreferences { it[THEME_STYLE_KEY] = style.name }
    }

    val crystalStyle: Flow<CrystalStyle> = dataStore.data.map { prefs ->
        when (prefs[CRYSTAL_STYLE_KEY]) {
            CrystalStyle.SHEER.name -> CrystalStyle.SHEER
            else                    -> CrystalStyle.TINTED
        }
    }

    suspend fun setCrystalStyle(style: CrystalStyle) {
        touchPreferences { it[CRYSTAL_STYLE_KEY] = style.name }
    }

    // Transient in-memory override used only while the Settings slider is under a thumb —
    // avoids a DataStore disk write (and a full dataStore.data re-emit) on every drag frame.
    // See the Android original's ThemePreferences.kt for the full rationale.
    private val crystalIntensityPreview = MutableStateFlow<Float?>(null)

    val crystalIntensity: Flow<Float> = combine(
        dataStore.data.map { prefs -> prefs[CRYSTAL_INTENSITY_KEY] ?: DEFAULT_CRYSTAL_INTENSITY },
        crystalIntensityPreview
    ) { stored, preview -> (preview ?: stored).coerceIn(0f, 1f) }

    fun previewCrystalIntensity(value: Float?) {
        crystalIntensityPreview.value = value
    }

    suspend fun setCrystalIntensity(value: Float) {
        touchPreferences { it[CRYSTAL_INTENSITY_KEY] = value.coerceIn(0f, 1f) }
        crystalIntensityPreview.value = null
    }

    // Preview overrides for the mesh Custom field dialog. Previewing the field also means
    // previewing *selection* of it (mesh -> CUSTOM, background -> MESH) even before Save, since
    // these two enums are what every reader (including this screen's own swatch highlighting)
    // uses to decide what's actually showing.
    private val crystalBackgroundPreview = MutableStateFlow<CrystalBackground?>(null)
    private val crystalMeshPreview = MutableStateFlow<CrystalMesh?>(null)

    val crystalBackground: Flow<CrystalBackground> = combine(
        dataStore.data.map { prefs ->
            when (prefs[CRYSTAL_BACKGROUND_KEY]) {
                CrystalBackground.SOLID.name -> CrystalBackground.SOLID
                else                         -> CrystalBackground.MESH
            }
        },
        crystalBackgroundPreview
    ) { stored, preview -> preview ?: stored }

    suspend fun setCrystalBackground(background: CrystalBackground) {
        touchPreferences { it[CRYSTAL_BACKGROUND_KEY] = background.name }
    }

    val crystalMesh: Flow<CrystalMesh> = combine(
        dataStore.data.map { prefs ->
            when (prefs[CRYSTAL_MESH_KEY]) {
                CrystalMesh.EMBER.name    -> CrystalMesh.EMBER
                CrystalMesh.VERDANT.name  -> CrystalMesh.VERDANT
                CrystalMesh.MIST.name     -> CrystalMesh.MIST
                CrystalMesh.ROSE.name     -> CrystalMesh.ROSE
                CrystalMesh.TIDE.name     -> CrystalMesh.TIDE
                CrystalMesh.INDIGO.name   -> CrystalMesh.INDIGO
                CrystalMesh.ORCHID.name   -> CrystalMesh.ORCHID
                CrystalMesh.DUNE.name     -> CrystalMesh.DUNE
                CrystalMesh.GRAPHITE.name -> CrystalMesh.GRAPHITE
                CrystalMesh.CUSTOM.name   -> CrystalMesh.CUSTOM
                else                      -> CrystalMesh.AURORA
            }
        },
        crystalMeshPreview
    ) { stored, preview -> preview ?: stored }

    suspend fun setCrystalMesh(mesh: CrystalMesh) {
        touchPreferences { it[CRYSTAL_MESH_KEY] = mesh.name }
    }

    // Transient in-memory override for the mesh Custom field's own seed/chroma, same shape as
    // crystalIntensityPreview above.
    private val crystalMeshCustomPreview = MutableStateFlow<Pair<Int, Float>?>(null)

    val crystalMeshCustomArgb: Flow<Int?> = combine(
        dataStore.data.map { it[CRYSTAL_MESH_CUSTOM_ARGB_KEY] },
        crystalMeshCustomPreview
    ) { stored, preview -> if (preview != null) preview.first else stored }

    val crystalMeshCustomChroma: Flow<Float> = combine(
        dataStore.data.map { prefs ->
            (prefs[CRYSTAL_MESH_CUSTOM_CHROMA_KEY] ?: DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA)
                .coerceIn(MIN_CRYSTAL_MESH_CUSTOM_CHROMA, 1f)
        },
        crystalMeshCustomPreview
    ) { stored, preview -> (preview?.second ?: stored).coerceIn(MIN_CRYSTAL_MESH_CUSTOM_CHROMA, 1f) }

    /** Called every drag frame from the mesh Custom field dialog — previews the seed/chroma and,
     * by also previewing the mesh/background enums above, the field actually showing as selected. */
    fun previewCrystalMeshCustom(argb: Int, chromaScale: Float) {
        crystalMeshCustomPreview.value = argb to chromaScale
        crystalMeshPreview.value = CrystalMesh.CUSTOM
        crystalBackgroundPreview.value = CrystalBackground.MESH
    }

    /** Cancel/dismiss-without-saving: revert all three previewed values to whatever was actually
     * committed. */
    fun cancelCrystalMeshCustomPreview() {
        crystalMeshCustomPreview.value = null
        crystalMeshPreview.value = null
        crystalBackgroundPreview.value = null
    }

    suspend fun setCrystalMeshCustom(argb: Int, chromaScale: Float) {
        touchPreferences {
            it[CRYSTAL_MESH_CUSTOM_ARGB_KEY] = argb
            it[CRYSTAL_MESH_CUSTOM_CHROMA_KEY] = chromaScale.coerceIn(MIN_CRYSTAL_MESH_CUSTOM_CHROMA, 1f)
        }
        crystalMeshCustomPreview.value = null
        crystalMeshPreview.value = null
        crystalBackgroundPreview.value = null
    }

    // Transient in-memory override while the custom Crystal-room picker is under the thumb —
    // same shape as accentPreview above.
    private val crystalRoomPreview = MutableStateFlow<Pair<Int, Int?>?>(null)

    val crystalBackgroundColorIndex: Flow<Int> = combine(
        dataStore.data.map { prefs -> prefs[CRYSTAL_BG_COLOR_KEY] ?: CRYSTAL_BG_NEUTRAL },
        crystalRoomPreview
    ) { stored, preview -> preview?.first ?: stored }

    val crystalBackgroundColorArgb: Flow<Int?> = combine(
        dataStore.data.map { prefs -> prefs[CRYSTAL_BG_COLOR_ARGB_KEY] },
        crystalRoomPreview
    ) { stored, preview -> if (preview != null) preview.second else stored }

    /** Called every drag frame from the custom Crystal-room picker. */
    fun previewCrystalBackgroundColor(index: Int, argb: Int?) {
        crystalRoomPreview.value = index to argb
    }

    /** Cancel/dismiss-without-selecting: revert to whatever was actually committed. */
    fun cancelCrystalBackgroundColorPreview() {
        crystalRoomPreview.value = null
    }

    suspend fun setCrystalBackgroundColor(index: Int, argb: Int? = null) {
        touchPreferences { prefs ->
            prefs[CRYSTAL_BG_COLOR_KEY] = index
            if (argb != null) prefs[CRYSTAL_BG_COLOR_ARGB_KEY] = argb
            else prefs.remove(CRYSTAL_BG_COLOR_ARGB_KEY)
        }
        crystalRoomPreview.value = null
    }

    val calendarIntegrationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CALENDAR_INTEGRATION_KEY] ?: false
    }

    suspend fun setCalendarIntegrationEnabled(enabled: Boolean) {
        touchPreferences { prefs ->
            prefs[CALENDAR_INTEGRATION_KEY] = enabled
        }
    }

    val enabledCalendars: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[ENABLED_CALENDARS_KEY] ?: emptySet()
    }

    suspend fun setEnabledCalendars(ids: Set<String>) {
        touchPreferences { prefs ->
            prefs[ENABLED_CALENDARS_KEY] = ids
        }
    }

    val customColors: Flow<List<Int?>> = dataStore.data.map { prefs ->
        val raw = prefs[CUSTOM_COLORS_KEY] ?: ""
        val parts = raw.split(",")
        (0..5).map { i ->
            parts.getOrNull(i)?.takeIf { it.isNotBlank() }?.toIntOrNull()
        }
    }

    suspend fun setCustomColor(index: Int, argb: Int?) {
        touchPreferences { prefs ->
            val raw = prefs[CUSTOM_COLORS_KEY] ?: ""
            val parts = raw.split(",").toMutableList()
            while (parts.size < 6) parts.add("")
            if (index in 0..5) {
                parts[index] = argb?.toString() ?: ""
                prefs[CUSTOM_COLORS_KEY] = parts.joinToString(",")
            }
        }
    }

    suspend fun setUserName(name: String) { touchPreferences { it[USER_NAME_KEY] = name } }
    suspend fun setUserNickname(nickname: String) { touchPreferences { it[USER_NICKNAME_KEY] = nickname } }
    suspend fun setUserAge(age: String) { touchPreferences { it[USER_AGE_KEY] = age } }
    suspend fun setUserPronouns(pronouns: String) { touchPreferences { it[USER_PRONOUNS_KEY] = pronouns } }
    suspend fun setIsPro(isPro: Boolean) { touchPreferences { it[IS_PRO_KEY] = isPro } }
    suspend fun setHasSeenOnboarding(seen: Boolean) { touchPreferences { it[HAS_SEEN_ONBOARDING_KEY] = seen } }
    suspend fun setHasCompletedInteractiveTutorial(completed: Boolean) { touchPreferences { it[HAS_COMPLETED_INTERACTIVE_TUTORIAL_KEY] = completed } }
    suspend fun setHasSeenHabitCreationTutorial(seen: Boolean) { touchPreferences { it[HAS_SEEN_HABIT_CREATION_TUTORIAL_KEY] = seen } }
    suspend fun setHasSeenNotesTemplateTutorial(seen: Boolean) { touchPreferences { it[HAS_SEEN_NOTES_TEMPLATE_TUTORIAL_KEY] = seen } }
    suspend fun setHasSeenChecklistHabitTutorial(seen: Boolean) { touchPreferences { it[HAS_SEEN_CHECKLIST_HABIT_TUTORIAL_KEY] = seen } }
    suspend fun setHasSeenSwipeGestureTutorial(seen: Boolean) { touchPreferences { it[HAS_SEEN_SWIPE_GESTURE_TUTORIAL_KEY] = seen } }
    /** The user picked or removed a picture: counts as a settings change, since the picture syncs. */
    suspend fun setProfilePictureUri(uri: String?) {
        touchPreferences(forceStamp = true) { prefs ->
            if (uri != null) prefs[PROFILE_PICTURE_URI_KEY] = uri
            else prefs.remove(PROFILE_PICTURE_URI_KEY)
        }
    }

    /** Same, without counting as a settings change — a picture received from a peer. */
    suspend fun setProfilePictureUriQuietly(uri: String?) {
        touchPreferences { prefs ->
            if (uri != null) prefs[PROFILE_PICTURE_URI_KEY] = uri
            else prefs.remove(PROFILE_PICTURE_URI_KEY)
        }
    }

    suspend fun exportPreferences(): JSONObject {
        val prefs = dataStore.data.first()
        return JSONObject().apply {
            put("themeMode", prefs[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name)
            put("darkReadability", prefs[DARK_READABILITY_KEY] ?: DarkReadability.STANDARD.name)
            put("themeStyle", prefs[THEME_STYLE_KEY] ?: ThemeStyle.MATERIAL3.name)
            put("crystalStyle", prefs[CRYSTAL_STYLE_KEY] ?: CrystalStyle.TINTED.name)
            // Stored as a string — this port's JSONObject (kotlinx.serialization-backed,
            // see core/json/OrgJsonCompat.kt) has no getDouble/optDouble, unlike org.json.
            put("crystalIntensity", (prefs[CRYSTAL_INTENSITY_KEY] ?: DEFAULT_CRYSTAL_INTENSITY).toString())
            put("crystalBackground", prefs[CRYSTAL_BACKGROUND_KEY] ?: CrystalBackground.MESH.name)
            put("crystalMesh", prefs[CRYSTAL_MESH_KEY] ?: CrystalMesh.AURORA.name)
            put("crystalBgColorIndex", prefs[CRYSTAL_BG_COLOR_KEY] ?: CRYSTAL_BG_NEUTRAL)
            prefs[CRYSTAL_BG_COLOR_ARGB_KEY]?.let { put("crystalBgColorArgb", it) }
            prefs[CRYSTAL_MESH_CUSTOM_ARGB_KEY]?.let { put("crystalMeshCustomArgb", it) }
            put("crystalMeshCustomChroma", (prefs[CRYSTAL_MESH_CUSTOM_CHROMA_KEY] ?: DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA).toString())
            put("amoledMode", prefs[AMOLED_MODE_KEY] ?: false)
            put("accentColorIndex", prefs[ACCENT_COLOR_KEY] ?: 0)
            prefs[ACCENT_COLOR_ARGB_KEY]?.let { put("accentColorArgb", it) }
            put("swipeSectionsEnabled", prefs[SWIPE_SECTIONS_KEY] ?: false)
            put("homeViewCalendar", prefs[HOME_VIEW_CALENDAR_KEY] ?: false)
            put("calendarListMode", prefs[CALENDAR_LIST_MODE_KEY] ?: false)
            put("notesToolbarPinned", prefs[NOTES_TOOLBAR_PINNED] ?: false)
            put("customColors", prefs[CUSTOM_COLORS_KEY] ?: "")
            put("userName", prefs[USER_NAME_KEY] ?: "")
            put("userNickname", prefs[USER_NICKNAME_KEY] ?: "")
            put("userAge", prefs[USER_AGE_KEY] ?: "")
            put("userPronouns", prefs[USER_PRONOUNS_KEY] ?: "")
            put("profilePictureUri", prefs[PROFILE_PICTURE_URI_KEY] ?: JSONObject.NULL)
            put("isPro", prefs[IS_PRO_KEY] ?: false)
            put("hasSeenOnboarding", prefs[HAS_SEEN_ONBOARDING_KEY] ?: false)
            put("hasCompletedInteractiveTutorial", prefs[HAS_COMPLETED_INTERACTIVE_TUTORIAL_KEY] ?: false)
            put("hasSeenHabitCreationTutorial", prefs[HAS_SEEN_HABIT_CREATION_TUTORIAL_KEY] ?: false)
            put("hasSeenNotesTemplateTutorial", prefs[HAS_SEEN_NOTES_TEMPLATE_TUTORIAL_KEY] ?: false)
            put("hasSeenChecklistHabitTutorial", prefs[HAS_SEEN_CHECKLIST_HABIT_TUTORIAL_KEY] ?: false)
            put("hasSeenSwipeGestureTutorial", prefs[HAS_SEEN_SWIPE_GESTURE_TUTORIAL_KEY] ?: false)
            put("calendarIntegrationEnabled", prefs[CALENDAR_INTEGRATION_KEY] ?: false)
            prefs[ENABLED_CALENDARS_KEY]?.let { put("enabledCalendars", it.joinToString(",")) }
        }
    }

    suspend fun importPreferences(json: JSONObject) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = json.optString("themeMode", ThemeMode.SYSTEM.name)
            prefs[DARK_READABILITY_KEY] = json.optString("darkReadability", DarkReadability.STANDARD.name)
            prefs[THEME_STYLE_KEY] = json.optString("themeStyle", ThemeStyle.MATERIAL3.name)
            prefs[CRYSTAL_STYLE_KEY] = json.optString("crystalStyle", CrystalStyle.TINTED.name)
            prefs[CRYSTAL_INTENSITY_KEY] =
                (json.optString("crystalIntensity", DEFAULT_CRYSTAL_INTENSITY.toString()).toFloatOrNull()
                    ?: DEFAULT_CRYSTAL_INTENSITY).coerceIn(0f, 1f)
            prefs[CRYSTAL_BACKGROUND_KEY] = json.optString("crystalBackground", CrystalBackground.MESH.name)
            prefs[CRYSTAL_MESH_KEY] = json.optString("crystalMesh", CrystalMesh.AURORA.name)
            prefs[CRYSTAL_BG_COLOR_KEY] = json.optInt("crystalBgColorIndex", CRYSTAL_BG_NEUTRAL)
            if (json.has("crystalBgColorArgb")) {
                prefs[CRYSTAL_BG_COLOR_ARGB_KEY] = json.getInt("crystalBgColorArgb")
            } else {
                prefs.remove(CRYSTAL_BG_COLOR_ARGB_KEY)
            }
            if (json.has("crystalMeshCustomArgb")) {
                prefs[CRYSTAL_MESH_CUSTOM_ARGB_KEY] = json.getInt("crystalMeshCustomArgb")
            } else {
                prefs.remove(CRYSTAL_MESH_CUSTOM_ARGB_KEY)
            }
            prefs[CRYSTAL_MESH_CUSTOM_CHROMA_KEY] =
                (json.optString("crystalMeshCustomChroma", DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA.toString()).toFloatOrNull()
                    ?: DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA).coerceIn(MIN_CRYSTAL_MESH_CUSTOM_CHROMA, 1f)
            prefs[AMOLED_MODE_KEY] = json.optBoolean("amoledMode", false)
            prefs[ACCENT_COLOR_KEY] = json.optInt("accentColorIndex", 0)
            if (json.has("accentColorArgb")) {
                prefs[ACCENT_COLOR_ARGB_KEY] = json.getInt("accentColorArgb")
            } else {
                prefs.remove(ACCENT_COLOR_ARGB_KEY)
            }
            prefs[SWIPE_SECTIONS_KEY] = json.optBoolean("swipeSectionsEnabled", false)
            prefs[HOME_VIEW_CALENDAR_KEY] = json.optBoolean("homeViewCalendar", false)
            prefs[CALENDAR_LIST_MODE_KEY] = json.optBoolean("calendarListMode", false)
            prefs[NOTES_TOOLBAR_PINNED] = json.optBoolean("notesToolbarPinned", false)

            val customColors = json.optString("customColors", "")
            if (customColors.isNotEmpty()) prefs[CUSTOM_COLORS_KEY] = customColors

            val userName = json.optString("userName", "")
            if (userName.isNotEmpty()) prefs[USER_NAME_KEY] = userName

            val userNickname = json.optString("userNickname", "")
            if (userNickname.isNotEmpty()) prefs[USER_NICKNAME_KEY] = userNickname

            val userAge = json.optString("userAge", "")
            if (userAge.isNotEmpty()) prefs[USER_AGE_KEY] = userAge

            val userPronouns = json.optString("userPronouns", "")
            if (userPronouns.isNotEmpty()) prefs[USER_PRONOUNS_KEY] = userPronouns

            if (json.has("profilePictureUri") && !json.isNull("profilePictureUri")) {
                prefs[PROFILE_PICTURE_URI_KEY] = json.getString("profilePictureUri")
            }

            // Restore tutorial/onboarding state to avoid re-triggering for long-time users
            if (json.has("hasSeenOnboarding")) {
                prefs[HAS_SEEN_ONBOARDING_KEY] = json.optBoolean("hasSeenOnboarding", false)
            }
            if (json.has("hasCompletedInteractiveTutorial")) {
                prefs[HAS_COMPLETED_INTERACTIVE_TUTORIAL_KEY] = json.optBoolean("hasCompletedInteractiveTutorial", false)
            }
            if (json.has("hasSeenHabitCreationTutorial")) {
                prefs[HAS_SEEN_HABIT_CREATION_TUTORIAL_KEY] = json.optBoolean("hasSeenHabitCreationTutorial", false)
            }
            if (json.has("hasSeenNotesTemplateTutorial")) {
                prefs[HAS_SEEN_NOTES_TEMPLATE_TUTORIAL_KEY] = json.optBoolean("hasSeenNotesTemplateTutorial", false)
            }
            if (json.has("hasSeenChecklistHabitTutorial")) {
                prefs[HAS_SEEN_CHECKLIST_HABIT_TUTORIAL_KEY] = json.optBoolean("hasSeenChecklistHabitTutorial", false)
            }
            if (json.has("hasSeenSwipeGestureTutorial")) {
                prefs[HAS_SEEN_SWIPE_GESTURE_TUTORIAL_KEY] = json.optBoolean("hasSeenSwipeGestureTutorial", false)
            }

            // Restore Pro status
            if (json.has("isPro")) {
                prefs[IS_PRO_KEY] = json.optBoolean("isPro", false)
            }

            if (json.has("calendarIntegrationEnabled")) {
                prefs[CALENDAR_INTEGRATION_KEY] = json.optBoolean("calendarIntegrationEnabled", false)
            }

            val calendars = json.optString("enabledCalendars", "")
            if (calendars.isNotEmpty()) {
                prefs[ENABLED_CALENDARS_KEY] = calendars.split(",").toSet()
            }

        }
    }

    /**
     * [exportPreferences] with device-local-only keys stripped — the basis for the Stage 3
     * preferences sync blob (`PreferencesDto`). `RUN_IN_BACKGROUND_KEY` is the one desktop-only
     * setting (no Android equivalent), but it is already omitted from [exportPreferences] itself
     * (see that key's own KDoc) — there is nothing left to strip today. This wrapper exists anyway
     * so the sync call site has one stable name to call, and so a future desktop-only preference
     * added to [exportPreferences] without also being added here is the one place that would need
     * updating, rather than every call site re-deriving its own exclusion list.
     */
    suspend fun exportPreferencesForSync(): JSONObject =
        // The path points into this computer's storage; the picture travels separately. Purchase
        // status and the per-category sync toggles are device-local and never sent, as on Android.
        exportPreferences().apply { SYNC_DEVICE_LOCAL_JSON_KEYS.forEach { remove(it) } }

    /**
     * [importPreferences] applied verbatim — kept as its own name (rather than calling
     * `importPreferences` directly from the sync engine) purely so it reads symmetrically with
     * [exportPreferencesForSync] at the call site, and so a future exclusion need only be added
     * here without touching the unmodified backup functions above.
     */
    suspend fun importPreferencesForSync(json: JSONObject) =
        importPreferences(json.apply { SYNC_DEVICE_LOCAL_JSON_KEYS.forEach { remove(it) } })

    /** Device-local preference names: never sent, and ignored if a peer sends them anyway. */
    private val SYNC_DEVICE_LOCAL_JSON_KEYS = listOf(
        "isPro", "isLifetimePro", "profilePictureUri",
        "syncHabitsEnabled", "syncRemindersEnabled", "syncEventsEnabled", "syncTodosEnabled",
        "syncClockEnabled", "syncNotesEnabled", "syncJournalEnabled", "syncPreferencesEnabled"
    )

    /**
     * Records the sender's own `updatedAt`, not "now" — called by the sync engine right after
     * [importPreferencesForSync] applies an incoming blob. [importPreferences] (and therefore
     * [importPreferencesForSync]) is deliberately left outside [touchPreferences] so it never
     * bumps [PREFERENCES_UPDATED_AT_KEY] to the current time itself; writing the peer's timestamp
     * here instead means both devices converge on the same value after a sync round, rather than
     * this device immediately looking "newer" than the peer it just matched and re-sending an
     * identical blob back on the very next round.
     */
    suspend fun setPreferencesUpdatedAtFromSync(updatedAt: Long) {
        dataStore.edit { it[PREFERENCES_UPDATED_AT_KEY] = updatedAt }
    }
}
