package com.apagon.rhythm.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.apagon.rhythm.core.json.JSONObject


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

        /** Index meaning "no colour — the cool neutral room". Distinct from 0, a real palette entry. */
        const val CRYSTAL_BG_NEUTRAL = -1

        /** Dead centre of whichever Crystal band is active. */
        const val DEFAULT_CRYSTAL_INTENSITY = 0.5f
        const val DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA = 1.0f
        const val MIN_CRYSTAL_MESH_CUSTOM_CHROMA = 0.18f
    }

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

    val accentColorIndex: Flow<Int> = dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_KEY] ?: 0
    }

    val accentColorArgb: Flow<Int?> = dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_ARGB_KEY]
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

    val calendarListMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CALENDAR_LIST_MODE_KEY] ?: false
    }

    suspend fun setHomeViewCalendar(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[HOME_VIEW_CALENDAR_KEY] = enabled
        }
    }

    suspend fun setCalendarListMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[CALENDAR_LIST_MODE_KEY] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AMOLED_MODE_KEY] = enabled
        }
    }

    suspend fun setAccentColor(index: Int, argb: Int? = null) {
        dataStore.edit { prefs ->
            prefs[ACCENT_COLOR_KEY] = index
            if (argb != null) prefs[ACCENT_COLOR_ARGB_KEY] = argb
            else prefs.remove(ACCENT_COLOR_ARGB_KEY)
        }
    }

    suspend fun setAccentColorIndex(index: Int) {
        setAccentColor(index, null)
    }

    suspend fun setSwipeSectionsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
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
        dataStore.edit { it[NOTES_TOOLBAR_PINNED] = pinned }
    }

    val darkReadability: Flow<DarkReadability> = dataStore.data.map { prefs ->
        when (prefs[DARK_READABILITY_KEY]) {
            DarkReadability.COMFORTABLE.name -> DarkReadability.COMFORTABLE
            DarkReadability.HIGH.name        -> DarkReadability.HIGH
            else                             -> DarkReadability.STANDARD
        }
    }

    suspend fun setDarkReadability(mode: DarkReadability) {
        dataStore.edit { it[DARK_READABILITY_KEY] = mode.name }
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
        dataStore.edit { it[THEME_STYLE_KEY] = style.name }
    }

    val crystalStyle: Flow<CrystalStyle> = dataStore.data.map { prefs ->
        when (prefs[CRYSTAL_STYLE_KEY]) {
            CrystalStyle.SHEER.name -> CrystalStyle.SHEER
            else                    -> CrystalStyle.TINTED
        }
    }

    suspend fun setCrystalStyle(style: CrystalStyle) {
        dataStore.edit { it[CRYSTAL_STYLE_KEY] = style.name }
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
        dataStore.edit { it[CRYSTAL_INTENSITY_KEY] = value.coerceIn(0f, 1f) }
        crystalIntensityPreview.value = null
    }

    val crystalBackground: Flow<CrystalBackground> = dataStore.data.map { prefs ->
        when (prefs[CRYSTAL_BACKGROUND_KEY]) {
            CrystalBackground.SOLID.name -> CrystalBackground.SOLID
            else                         -> CrystalBackground.MESH
        }
    }

    suspend fun setCrystalBackground(background: CrystalBackground) {
        dataStore.edit { it[CRYSTAL_BACKGROUND_KEY] = background.name }
    }

    val crystalMesh: Flow<CrystalMesh> = dataStore.data.map { prefs ->
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
    }

    suspend fun setCrystalMesh(mesh: CrystalMesh) {
        dataStore.edit { it[CRYSTAL_MESH_KEY] = mesh.name }
    }

    val crystalMeshCustomArgb: Flow<Int?> = dataStore.data.map { it[CRYSTAL_MESH_CUSTOM_ARGB_KEY] }

    val crystalMeshCustomChroma: Flow<Float> = dataStore.data.map { prefs ->
        (prefs[CRYSTAL_MESH_CUSTOM_CHROMA_KEY] ?: DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA)
            .coerceIn(MIN_CRYSTAL_MESH_CUSTOM_CHROMA, 1f)
    }

    suspend fun setCrystalMeshCustom(argb: Int, chromaScale: Float) {
        dataStore.edit {
            it[CRYSTAL_MESH_CUSTOM_ARGB_KEY] = argb
            it[CRYSTAL_MESH_CUSTOM_CHROMA_KEY] = chromaScale.coerceIn(MIN_CRYSTAL_MESH_CUSTOM_CHROMA, 1f)
        }
    }

    val crystalBackgroundColorIndex: Flow<Int> = dataStore.data.map { prefs ->
        prefs[CRYSTAL_BG_COLOR_KEY] ?: CRYSTAL_BG_NEUTRAL
    }

    val crystalBackgroundColorArgb: Flow<Int?> = dataStore.data.map { prefs ->
        prefs[CRYSTAL_BG_COLOR_ARGB_KEY]
    }

    suspend fun setCrystalBackgroundColor(index: Int, argb: Int? = null) {
        dataStore.edit { prefs ->
            prefs[CRYSTAL_BG_COLOR_KEY] = index
            if (argb != null) prefs[CRYSTAL_BG_COLOR_ARGB_KEY] = argb
            else prefs.remove(CRYSTAL_BG_COLOR_ARGB_KEY)
        }
    }

    val calendarIntegrationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CALENDAR_INTEGRATION_KEY] ?: false
    }

    suspend fun setCalendarIntegrationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[CALENDAR_INTEGRATION_KEY] = enabled
        }
    }

    val enabledCalendars: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[ENABLED_CALENDARS_KEY] ?: emptySet()
    }

    suspend fun setEnabledCalendars(ids: Set<String>) {
        dataStore.edit { prefs ->
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
        dataStore.edit { prefs ->
            val raw = prefs[CUSTOM_COLORS_KEY] ?: ""
            val parts = raw.split(",").toMutableList()
            while (parts.size < 6) parts.add("")
            if (index in 0..5) {
                parts[index] = argb?.toString() ?: ""
                prefs[CUSTOM_COLORS_KEY] = parts.joinToString(",")
            }
        }
    }

    suspend fun setUserName(name: String) { dataStore.edit { it[USER_NAME_KEY] = name } }
    suspend fun setUserNickname(nickname: String) { dataStore.edit { it[USER_NICKNAME_KEY] = nickname } }
    suspend fun setUserAge(age: String) { dataStore.edit { it[USER_AGE_KEY] = age } }
    suspend fun setUserPronouns(pronouns: String) { dataStore.edit { it[USER_PRONOUNS_KEY] = pronouns } }
    suspend fun setIsPro(isPro: Boolean) { dataStore.edit { it[IS_PRO_KEY] = isPro } }
    suspend fun setHasSeenOnboarding(seen: Boolean) { dataStore.edit { it[HAS_SEEN_ONBOARDING_KEY] = seen } }
    suspend fun setHasCompletedInteractiveTutorial(completed: Boolean) { dataStore.edit { it[HAS_COMPLETED_INTERACTIVE_TUTORIAL_KEY] = completed } }
    suspend fun setHasSeenHabitCreationTutorial(seen: Boolean) { dataStore.edit { it[HAS_SEEN_HABIT_CREATION_TUTORIAL_KEY] = seen } }
    suspend fun setHasSeenNotesTemplateTutorial(seen: Boolean) { dataStore.edit { it[HAS_SEEN_NOTES_TEMPLATE_TUTORIAL_KEY] = seen } }
    suspend fun setHasSeenChecklistHabitTutorial(seen: Boolean) { dataStore.edit { it[HAS_SEEN_CHECKLIST_HABIT_TUTORIAL_KEY] = seen } }
    suspend fun setHasSeenSwipeGestureTutorial(seen: Boolean) { dataStore.edit { it[HAS_SEEN_SWIPE_GESTURE_TUTORIAL_KEY] = seen } }
    suspend fun setProfilePictureUri(uri: String?) {
        dataStore.edit { prefs ->
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
}
