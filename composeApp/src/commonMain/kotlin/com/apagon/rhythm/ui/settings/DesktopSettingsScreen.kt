package com.apagon.rhythm.ui.settings

import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.ui.components.BorikenUnlockDialog
import com.apagon.rhythm.core.app.AppInfo
import androidx.compose.material3.FilterChip
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.apagon.rhythm.ui.theme.BorikenSlots
import com.apagon.rhythm.ui.theme.BorikenCombo
import com.apagon.rhythm.ui.theme.crystalOwnFieldBlobs
import com.apagon.rhythm.ui.theme.crystalComboBlobs
import com.apagon.rhythm.ui.theme.borikenFieldBlobs
import com.apagon.rhythm.ui.components.TainoGlyphs
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.apagon.rhythm.data.sync.SyncListenState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.preferences.CrystalBackground
import com.apagon.rhythm.data.preferences.CrystalMesh
import com.apagon.rhythm.data.preferences.CrystalStyle
import com.apagon.rhythm.data.preferences.DarkReadability
import com.apagon.rhythm.data.preferences.ThemeMode
import com.apagon.rhythm.data.preferences.ThemeStyle
import com.apagon.rhythm.platform.QrCodeRenderer
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.components.crystalBareTextFieldColors
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.components.crystalRadioButtonColors
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalSliderColors
import com.apagon.rhythm.ui.components.crystalSwitchColors
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.components.drawCrystalMeshField
import com.apagon.rhythm.ui.deleted.DesktopRecentlyDeletedScreen
import com.apagon.rhythm.ui.theme.AmbientBaseDark
import com.apagon.rhythm.ui.theme.AmbientBaseLight
import com.apagon.rhythm.ui.theme.crystalFieldBlobs
import com.apagon.rhythm.ui.theme.habitColorPalette
import kotlin.math.roundToInt
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import com.apagon.rhythm.ui.components.crystalSelectedChipContentColor
import com.apagon.rhythm.ui.components.crystalSelectedChipColor
import com.apagon.rhythm.ui.components.crystalChipSurface
import androidx.compose.foundation.isSystemInDarkTheme
import com.apagon.rhythm.platform.ImageBitmapLoader
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.layout.ContentScale

/** Stage 15f: the section list a left rail drives — replacing the single scrolling LazyColumn of
 * every settings card stacked one after another. "Recently Deleted" moves here from the top-level
 * app sidebar (Stage 15a) per the confirmed decision that trash is secondary nav, not primary. */
// 2026-10-13: names and grouping mirror the phone's Settings (Profile, Appearance, Calendar
// Integration, Data Management, Tutorials, About). Sync and Recently Deleted live inside Data
// Management as sub-pages, as on the phone, so they're off the rail ([inRail] false).
private enum class SettingsSection(val label: String, val inRail: Boolean = true) {
    PROFILE("Profile"),
    APPEARANCE("Appearance"),
    CALENDAR("Calendar Integration"),
    DATA("Data Management"),
    SYNC("Sync with Phone & Computers", inRail = false),
    RECENTLY_DELETED("Recently Deleted", inRail = false),
    TUTORIALS("Tutorials"),
    ABOUT("About")
}

// Desktop counterpart to androidMain's SettingsScreen.kt (Stage 11) — a
// LazyColumn of sections rather than that 1512-line screen's full layout
// (BackHandler/permission launchers/Intent deep-links/billing all have no
// desktop equivalent). Reaches the same DesktopSettingsViewModel state Android's
// SettingsViewModel exposes, minus billing. No Material Icons, matching the
// desktop-wide convention (DesktopTodoScreen.kt, Stage 7).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopSettingsScreen(viewModel: DesktopSettingsViewModel = koinViewModel()) {
    val userName by viewModel.userName.collectAsState(initial = "")
    val userNickname by viewModel.userNickname.collectAsState(initial = "")
    val userAge by viewModel.userAge.collectAsState(initial = "")
    val userPronouns by viewModel.userPronouns.collectAsState(initial = "")
    val profilePictureUri by viewModel.profilePictureUri.collectAsState(initial = null)

    val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val amoledMode by viewModel.amoledMode.collectAsState(initial = false)
    val darkReadability by viewModel.darkReadability.collectAsState(initial = DarkReadability.STANDARD)
    val accentColorIndex by viewModel.accentColorIndex.collectAsState(initial = 0)
    val accentColorArgb by viewModel.accentColorArgb.collectAsState(initial = null)

    val themeStyle by viewModel.themeStyle.collectAsState()
    val crystalStyle by viewModel.crystalStyle.collectAsState()
    val crystalIntensity by viewModel.crystalIntensity.collectAsState()
    val crystalBackground by viewModel.crystalBackground.collectAsState()
    val crystalMesh by viewModel.crystalMesh.collectAsState()
    val crystalBgColorIndex by viewModel.crystalBackgroundColorIndex.collectAsState()
    val crystalBgColorArgb by viewModel.crystalBackgroundColorArgb.collectAsState()
    val crystalMeshCustomArgb by viewModel.crystalMeshCustomArgb.collectAsState()
    val crystalMeshCustomColors by viewModel.crystalMeshCustomColors.collectAsState()
    val crystalMeshCustomChroma by viewModel.crystalMeshCustomChroma.collectAsState()
    val colorPickerViewModel: com.apagon.rhythm.ui.util.ColorPickerViewModel = koinViewModel()

    var themeStyleExpanded by remember { mutableStateOf(false) }
    var crystalBackgroundExpanded by remember { mutableStateOf(false) }
    var crystalGlassExpanded by remember { mutableStateOf(false) }

    val swipeSectionsEnabled by viewModel.swipeSectionsEnabled.collectAsState(initial = false)
    val homeViewCalendar by viewModel.homeViewCalendar.collectAsState()
    val calendarIntegrationEnabled by viewModel.calendarIntegrationEnabled.collectAsState()
    val runInBackground by viewModel.runInBackground.collectAsState(initial = false)

    val archivedHabits by viewModel.archivedHabits.collectAsState()
    val archivedTodos by viewModel.archivedTodos.collectAsState()
    val backupState by viewModel.backupState.collectAsState()

    var showEditProfile by remember { mutableStateOf(false) }
    var showArchivedHabits by remember { mutableStateOf(false) }
    var showArchivedTodos by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    var selectedSection by remember { mutableStateOf(SettingsSection.PROFILE) }

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = { TopAppBar(title = { Text("Settings") }, colors = crystalTopAppBarColors()) }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.width(200.dp).fillMaxHeight().padding(vertical = 8.dp, horizontal = 8.dp)
            ) {
                SettingsSection.entries.filter { it.inRail }.forEach { section ->
                    // A Data Management sub-page keeps Data Management highlighted.
                    val isSelected = section == selectedSection ||
                        (!selectedSection.inRail && section == SettingsSection.DATA)
                    // Stage 17a: no pre-clip — see main.kt's SidebarItem for why (crystalTileSurface
                    // draws its own shadow with its own shape; a mismatched outer clip lets it escape
                    // on one edge).
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) Modifier.crystalTileSurface(fill = MaterialTheme.colorScheme.secondaryContainer)
                                else Modifier
                            )
                            .clickable { selectedSection = section }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            section.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            VerticalDivider()

            // Stage 16a: same centered content-width cap every other primary screen got.
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                when (selectedSection) {
                    SettingsSection.RECENTLY_DELETED -> Column(Modifier.fillMaxSize()) {
                        SubPageBackRow { selectedSection = SettingsSection.DATA }
                        DesktopRecentlyDeletedScreen()
                    }
                    else -> Column(
                        modifier = Modifier.fillMaxHeight().widthIn(max = DesktopLayout.contentMaxWidth).fillMaxWidth()
                            .verticalScroll(rememberScrollState()).padding(DesktopLayout.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (!selectedSection.inRail) SubPageBackRow { selectedSection = SettingsSection.DATA }
                        when (selectedSection) {
                            SettingsSection.PROFILE -> ProfileSectionContent(
                                userNickname = userNickname,
                                userName = userName,
                                userPronouns = userPronouns,
                                userAge = userAge,
                                profilePictureUri = profilePictureUri,
                                onEdit = { showEditProfile = true }
                            )
                            SettingsSection.APPEARANCE -> AppearanceSectionContent(
                                viewModel = viewModel,
                                themeMode = themeMode,
                                amoledMode = amoledMode,
                                darkReadability = darkReadability,
                                accentColorIndex = accentColorIndex,
                                accentColorArgb = accentColorArgb,
                                themeStyle = themeStyle,
                                crystalStyle = crystalStyle,
                                crystalIntensity = crystalIntensity,
                                crystalBackground = crystalBackground,
                                crystalMesh = crystalMesh,
                                crystalBgColorIndex = crystalBgColorIndex,
                                crystalBgColorArgb = crystalBgColorArgb,
                                crystalMeshCustomArgb = crystalMeshCustomArgb,
                                crystalMeshCustomColors = crystalMeshCustomColors,
                                crystalMeshCustomChroma = crystalMeshCustomChroma,
                                themeStyleExpanded = themeStyleExpanded,
                                onToggleThemeStyleExpanded = { themeStyleExpanded = !themeStyleExpanded },
                                crystalBackgroundExpanded = crystalBackgroundExpanded,
                                onToggleCrystalBackgroundExpanded = { crystalBackgroundExpanded = !crystalBackgroundExpanded },
                                crystalGlassExpanded = crystalGlassExpanded,
                                onToggleCrystalGlassExpanded = { crystalGlassExpanded = !crystalGlassExpanded },
                                colorPickerViewModel = colorPickerViewModel
                            ).also {
                                // As the phone: Layout is a card in Appearance.
                                LayoutSectionContent(
                                    viewModel = viewModel,
                                    homeViewCalendar = homeViewCalendar,
                                    swipeSectionsEnabled = swipeSectionsEnabled,
                                    runInBackground = runInBackground
                                )
                            }
                            SettingsSection.CALENDAR -> CalendarSectionContent(viewModel, calendarIntegrationEnabled)
                            SettingsSection.DATA -> DataSectionContent(
                                viewModel = viewModel,
                                backupState = backupState,
                                archivedHabits = archivedHabits,
                                archivedTodos = archivedTodos,
                                onShowArchivedHabits = { showArchivedHabits = true },
                                onShowArchivedTodos = { showArchivedTodos = true },
                                onOpenSync = { selectedSection = SettingsSection.SYNC },
                                onOpenRecentlyDeleted = { selectedSection = SettingsSection.RECENTLY_DELETED }
                            )
                            SettingsSection.SYNC -> SyncPageContent(viewModel)
                            SettingsSection.TUTORIALS -> HelpSectionContent()
                            SettingsSection.ABOUT -> AboutSectionContent(
                                onShowPrivacyPolicy = { showPrivacyPolicy = true },
                                themePreferences = viewModel.themePreferences
                            )
                            SettingsSection.RECENTLY_DELETED -> Unit
                        }
                    }
                }
            }
        }
    }

    if (showEditProfile) {
        DesktopEditProfileSheet(
            currentName = userName,
            currentNickname = userNickname,
            currentAge = userAge,
            currentPronouns = userPronouns,
            profilePictureUri = profilePictureUri,
            onDismiss = { showEditProfile = false },
            onPickPhoto = { viewModel.pickAndSetProfilePicture() },
            onRemovePhoto = { viewModel.setProfilePictureUri(null) },
            onSave = { name, nickname, age, pronouns ->
                viewModel.setUserName(name)
                viewModel.setUserNickname(nickname)
                viewModel.setUserAge(age)
                viewModel.setUserPronouns(pronouns)
                showEditProfile = false
            }
        )
    }

    if (showArchivedHabits) {
        DesktopArchivedHabitsSheet(
            habits = archivedHabits,
            onDismiss = { showArchivedHabits = false },
            onUnarchive = { viewModel.unarchiveHabit(it) }
        )
    }

    if (showArchivedTodos) {
        DesktopArchivedTodosSheet(
            todos = archivedTodos,
            onDismiss = { showArchivedTodos = false },
            onUnarchive = { viewModel.unarchiveTodo(it) }
        )
    }

    if (showPrivacyPolicy) {
        DesktopPrivacyPolicySheet(onDismiss = { showPrivacyPolicy = false })
    }
}

@Composable
private fun ProfileSectionContent(
    userNickname: String,
    userName: String,
    userPronouns: String,
    userAge: String,
    profilePictureUri: String?,
    onEdit: () -> Unit
) {
    // Android's profile card: photo, name, @nickname and a chevron, the whole card opening the
    // editor (it showed the nickname as the name, pronouns/age, and an "Edit" link).
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().crystalCardSurface().clickable(onClick = onEdit).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (profilePictureUri != null) {
                    koinInject<ImageBitmapLoader>().LoadedImage(
                        path = profilePictureUri,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    userName.ifBlank { "Set your name" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (userNickname.isNotBlank()) {
                    Text("@$userNickname", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        // As Android: one-tap sync sits right under the profile.
        com.apagon.rhythm.ui.sync.QuickSyncCard()
    }
}

@Composable
private fun AppearanceSectionContent(
    viewModel: DesktopSettingsViewModel,
    themeMode: ThemeMode,
    amoledMode: Boolean,
    darkReadability: DarkReadability,
    accentColorIndex: Int,
    accentColorArgb: Int?,
    themeStyle: ThemeStyle,
    crystalStyle: CrystalStyle,
    crystalIntensity: Float,
    crystalBackground: CrystalBackground,
    crystalMesh: CrystalMesh,
    crystalBgColorIndex: Int,
    crystalBgColorArgb: Int?,
    crystalMeshCustomArgb: Int?,
    crystalMeshCustomColors: List<Int>,
    crystalMeshCustomChroma: Float,
    themeStyleExpanded: Boolean,
    onToggleThemeStyleExpanded: () -> Unit,
    crystalBackgroundExpanded: Boolean,
    onToggleCrystalBackgroundExpanded: () -> Unit,
    crystalGlassExpanded: Boolean,
    onToggleCrystalGlassExpanded: () -> Unit,
    colorPickerViewModel: com.apagon.rhythm.ui.util.ColorPickerViewModel
) {
    var showMeshCustomDialog by remember { mutableStateOf(false) }
    if (showMeshCustomDialog) {
        com.apagon.rhythm.ui.util.CrystalCustomFieldDialog(
            initialColors = crystalMeshCustomColors.map { Color(it) }
                .ifEmpty { listOfNotNull(crystalMeshCustomArgb?.let { Color(it) }) },
            initialChroma = crystalMeshCustomChroma,
            onSave = { colors, chromaScale ->
                viewModel.setCrystalMeshCustomCombo(colors.map { it.toArgb() }, chromaScale)
            },
            onPreview = { colors, chromaScale ->
                viewModel.previewCrystalMeshCustom(colors.map { it.toArgb() }, chromaScale)
            },
            onCancelPreview = { viewModel.cancelCrystalMeshCustomPreview() },
            onDismiss = { showMeshCustomDialog = false }
        )
    }
    // Android's Appearance order and shape: one expandable glass card per setting — Theme Style,
    // Crystal Background, Glass, Accent Colour, Theme — not a card of rows with text-link choices.
    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    val borikenUnlocked by viewModel.themePreferences.borikenUnlocked.collectAsState(initial = false)
    val borikenMode by viewModel.themePreferences.borikenMode.collectAsState(initial = false)
    val borikenField by viewModel.themePreferences.borikenField.collectAsState(initial = true)
    val prefsScope = rememberCoroutineScope()
    val setBoriken = { on: Boolean -> prefsScope.launch { viewModel.themePreferences.setBorikenMode(on) }; Unit }
    var accentExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    SettingsExpandableCard(
                    title = "Theme Style",
                    expanded = themeStyleExpanded,
                    onToggle = onToggleThemeStyleExpanded,
                    summary = (if (borikenMode) "Borikén · " else "") + when (themeStyle) {
                        ThemeStyle.MATERIAL3 -> "Material 3"
                        ThemeStyle.EXPRESSIVE -> "Expressive"
                        ThemeStyle.CRYSTAL -> "Crystal"
                    }
                ) {
                    ThemeStyle.entries.forEachIndexed { i, style ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { setBoriken(false); viewModel.setThemeStyle(style) }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                colors = crystalRadioButtonColors(),
                                selected = !borikenMode && themeStyle == style,
                                onClick = { setBoriken(false); viewModel.setThemeStyle(style) }
                            )
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    when (style) {
                                        ThemeStyle.MATERIAL3 -> "Material 3"
                                        ThemeStyle.EXPRESSIVE -> "Material 3 Expressive"
                                        ThemeStyle.CRYSTAL -> "Crystal"
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    when (style) {
                                        ThemeStyle.MATERIAL3 -> "The classic Bori look"
                                        ThemeStyle.EXPRESSIVE -> "Flat and bold, with a punchier palette"
                                        ThemeStyle.CRYSTAL -> "Frosted glass panels over a soft colour field"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (i < ThemeStyle.entries.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    // The hidden edition: listed only once unlocked from the version in About.
                    if (borikenUnlocked) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { setBoriken(true) }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                colors = crystalRadioButtonColors(),
                                selected = borikenMode,
                                onClick = { setBoriken(true) }
                            )
                            Column(modifier = Modifier.padding(vertical = 8.dp).weight(1f)) {
                                Text("Borikén", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Las Celestes, the flag and Taíno symbols — wepa!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(TainoGlyphs.Sun, contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                        }
                        // Borikén sits on top of a base style; pick which one under it.
                        if (borikenMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = themeStyle == style,
                                        onClick = { viewModel.setThemeStyle(style) },
                                        label = {
                                            Text(when (style) {
                                                ThemeStyle.MATERIAL3 -> "Material 3"
                                                ThemeStyle.EXPRESSIVE -> "Expressive"
                                                ThemeStyle.CRYSTAL -> "Crystal"
                                            })
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

    if (themeStyle == ThemeStyle.CRYSTAL) {
                    SettingsExpandableCard(
                        title = "Crystal Background",
                        expanded = crystalBackgroundExpanded,
                        onToggle = onToggleCrystalBackgroundExpanded,
                        summary = when {
                            borikenMode && borikenField -> "Borikén"
                            crystalBackground == CrystalBackground.MESH -> crystalMeshLabel(crystalMesh)
                            else -> "Solid"
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.setCrystalBackground(CrystalBackground.MESH) }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                colors = crystalRadioButtonColors(),
                                selected = crystalBackground == CrystalBackground.MESH,
                                onClick = { viewModel.setCrystalBackground(CrystalBackground.MESH) }
                            )
                            Text("Colour Field", style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // null is the Borikén swatch, which leads the grid while the mode is on.
                            val swatches: List<CrystalMesh?> =
                                if (borikenMode) listOf(null) + CrystalMesh.entries else CrystalMesh.entries
                            swatches.chunked(MESH_SWATCHES_PER_ROW).forEach { rowMeshes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowMeshes.forEach { mesh ->
                                        val flagOn = borikenMode && borikenField
                                        CrystalMeshSwatch(
                                            mesh = mesh,
                                            selected = if (mesh == null) flagOn
                                                else !flagOn && crystalBackground == CrystalBackground.MESH && crystalMesh == mesh,
                                            onClick = {
                                                if (mesh == null) {
                                                    prefsScope.launch { viewModel.themePreferences.selectBorikenField() }
                                                } else if (mesh == CrystalMesh.CUSTOM) {
                                                    showMeshCustomDialog = true
                                                } else {
                                                    viewModel.setCrystalBackground(CrystalBackground.MESH)
                                                    viewModel.setCrystalMesh(mesh)
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    // Keep a short last row aligned with the ones above instead of
                                    // stretching to fill — same rule Android's CrystalMeshPickerRow uses.
                                    repeat(MESH_SWATCHES_PER_ROW - rowMeshes.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    viewModel.setCrystalBackground(CrystalBackground.SOLID)
                                    viewModel.setCrystalBackgroundColor(0, null)
                                }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                colors = crystalRadioButtonColors(),
                                selected = crystalBackground == CrystalBackground.SOLID,
                                onClick = {
                                    viewModel.setCrystalBackground(CrystalBackground.SOLID)
                                    viewModel.setCrystalBackgroundColor(0, null)
                                }
                            )
                            Text("Solid", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (crystalBackground == CrystalBackground.SOLID) {
                            Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                                com.apagon.rhythm.ui.util.ColorPickerRow(
                                    colorIndex = crystalBgColorIndex,
                                    colorArgb = crystalBgColorArgb,
                                    onColorSelected = { idx, argb -> viewModel.setCrystalBackgroundColor(idx, argb) },
                                    viewModel = colorPickerViewModel,
                                    onPreview = { argb -> viewModel.previewCrystalBackgroundColor(-1, argb) },
                                    onCancelPreview = { viewModel.cancelCrystalBackgroundColorPreview() }
                                )
                            }
                        }
                    }

                    SettingsExpandableCard(
                        title = "Glass",
                        expanded = crystalGlassExpanded,
                        onToggle = onToggleCrystalGlassExpanded,
                        summary = when (crystalStyle) {
                            CrystalStyle.SHEER -> "Sheer"
                            CrystalStyle.TINTED -> "Tinted"
                        }
                    ) {
                        CrystalStyle.entries.forEachIndexed { i, style ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { viewModel.setCrystalStyle(style) }
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    colors = crystalRadioButtonColors(),
                                    selected = crystalStyle == style,
                                    onClick = { viewModel.setCrystalStyle(style) }
                                )
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        when (style) {
                                            CrystalStyle.SHEER -> "Sheer"
                                            CrystalStyle.TINTED -> "Tinted"
                                        },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        when (style) {
                                            CrystalStyle.SHEER -> "More transparent — more of the colour field shows through"
                                            CrystalStyle.TINTED -> "More opaque — easier to read, with richer depth"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (i < CrystalStyle.entries.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Glass Intensity",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${(crystalIntensity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Slider(
                                colors = crystalSliderColors(),
                                value = crystalIntensity,
                                onValueChange = { viewModel.previewCrystalIntensity(it) },
                                onValueChangeFinished = { viewModel.setCrystalIntensity(crystalIntensity) },
                                valueRange = 0f..1f,
                                steps = 19,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Clearer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "Frostier",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
    }

    SettingsExpandableCard(
        title = "Accent Colour",
        expanded = accentExpanded,
        onToggle = { accentExpanded = !accentExpanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Choose a primary color for the whole app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            com.apagon.rhythm.ui.util.ColorPickerRow(
                colorIndex = accentColorIndex,
                colorArgb = accentColorArgb,
                onColorSelected = { idx, argb -> viewModel.setAccentColor(idx, argb) },
                viewModel = colorPickerViewModel,
                onPreview = { argb -> viewModel.previewAccentColor(-1, argb) },
                onCancelPreview = { viewModel.cancelAccentColorPreview() }
            )
        }
    }

    SettingsExpandableCard(
        title = "Theme",
        expanded = themeExpanded,
        onToggle = { themeExpanded = !themeExpanded },
        summary = when (themeMode) {
            ThemeMode.SYSTEM -> "System default"
            ThemeMode.LIGHT -> "Light"
            ThemeMode.DARK -> "Dark"
        }
    ) {
        Column {
            ThemeMode.entries.forEachIndexed { i, mode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setThemeMode(mode) }.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    RadioButton(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) }, colors = crystalRadioButtonColors())
                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge)
                }
                if (i < ThemeMode.entries.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            // Android shows Night Readability only outside Crystal (Crystal has its own glass controls).
            if (themeStyle != ThemeStyle.CRYSTAL) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Night Readability", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Controls how much surface contrast is added in dark mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkReadability.entries.forEach { option ->
                            val selected = darkReadability == option
                            Box(
                                modifier = Modifier
                                    .crystalChipSurface(
                                        fill = if (selected) crystalSelectedChipColor(MaterialTheme.colorScheme.primary)
                                        else MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                    .clickable { viewModel.setDarkReadability(option) }
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    when (option) {
                                        DarkReadability.STANDARD -> "Standard"
                                        DarkReadability.COMFORTABLE -> "Comfortable"
                                        DarkReadability.HIGH -> "High"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) crystalSelectedChipContentColor(MaterialTheme.colorScheme.onPrimary) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            val darkActive = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AMOLED Black",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (darkActive) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (darkActive) "True black backgrounds — saves battery on OLED screens" else "Available when the dark theme is active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = amoledMode, enabled = darkActive, onCheckedChange = { viewModel.setAmoledMode(it) }, colors = crystalSwitchColors())
            }
        }
    }
}

@Composable
private fun LayoutSectionContent(
    viewModel: DesktopSettingsViewModel,
    homeViewCalendar: Boolean,
    swipeSectionsEnabled: Boolean,
    runInBackground: Boolean
) {
    SettingsSection("Layout") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsRow("Calendar as home view") {
                Switch(checked = homeViewCalendar, onCheckedChange = { viewModel.setHomeViewCalendar(it) }, colors = crystalSwitchColors())
            }
            SettingsRow("Swipeable sections") {
                Switch(checked = swipeSectionsEnabled, onCheckedChange = { viewModel.setSwipeSectionsEnabled(it) }, colors = crystalSwitchColors())
            }
        }
    }

    SettingsSection("Background") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsRow("Run in background") {
                Switch(checked = runInBackground, onCheckedChange = { viewModel.setRunInBackground(it) }, colors = crystalSwitchColors())
            }
            Text(
                "Keep Bori running when the window is closed, so alarms, reminders, and timers " +
                    "can still fire. Relaunch Bori to bring the window back.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

}

@Composable
private fun CalendarSectionContent(viewModel: DesktopSettingsViewModel, calendarIntegrationEnabled: Boolean) {
    SettingsSection("Calendar Integration") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsRow("Enable device calendars") {
                Switch(checked = calendarIntegrationEnabled, onCheckedChange = { viewModel.setCalendarIntegrationEnabled(it) }, colors = crystalSwitchColors())
            }
            if (calendarIntegrationEnabled) {
                Text(
                    "No calendars found on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DataSectionContent(
    viewModel: DesktopSettingsViewModel,
    backupState: DesktopBackupState,
    archivedHabits: List<*>,
    archivedTodos: List<*>,
    onShowArchivedHabits: () -> Unit,
    onShowArchivedTodos: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit
) {
    var showExportDialog by remember { mutableStateOf(false) }
    // True once an import attempt reports back that the chosen file is encrypted (or that the
    // password just tried was wrong) — reusing one dialog for both "ask up front" and "retry"
    // rather than guessing whether a file needs a password before picking it.
    var showImportPasswordDialog by remember { mutableStateOf(false) }

    // Same groups and order as the phone's Data Management page.
    SettingsSection("Backup & Restore") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showExportDialog = true }) { Text("Export") }
                TextButton(onClick = { viewModel.importBackup() }) { Text("Import") }
            }
            when (backupState) {
                is DesktopBackupState.Success -> Text(backupState.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                is DesktopBackupState.Error -> Text(backupState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                DesktopBackupState.Idle -> {}
            }
            // "password protected" / "Incorrect password" are the two strings
            // DesktopBackupManager.importFromJson returns for an encrypted file — surface the
            // password prompt automatically rather than making the user notice the error text
            // and re-click Import themselves.
            LaunchedEffect(backupState) {
                val message = (backupState as? DesktopBackupState.Error)?.message
                if (message != null && message.contains("password", ignoreCase = true)) {
                    showImportPasswordDialog = true
                }
            }
        }
    }

    val listen by viewModel.syncListenState.collectAsState()
    SettingsSection("Sync with Phone & Computers") {
      Column {
        HelpRow(
            title = when (val l = listen) {
                is SyncListenState.Listening -> if (l.viaTailscale) "Ready for your phone" else "Tailscale isn't running"
                is SyncListenState.Failed -> "Sync can't start"
                SyncListenState.Starting -> "Starting…"
            },
            description = "Sync with Bori on your phone and your other computers over Tailscale: devices, pairing QR code, and what to sync.",
            action = "Open",
            onClick = onOpenSync
        )
        // As on the phone: how often to sync sits right under the way in, not only inside it.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        DesktopAutoSyncRow()
      }
    }
    SettingsSection("Habit Archive") {
        HelpRow("View Archived Habits", "${archivedHabits.size} archived", "Open", onShowArchivedHabits)
    }
    SettingsSection("To-do Archive") {
        HelpRow("View Archived To-dos", "${archivedTodos.size} archived", "Open", onShowArchivedTodos)
    }
    SettingsSection("Recently Deleted") {
        HelpRow("Trash Bin", "Restore or permanently delete items", "Open", onOpenRecentlyDeleted)
    }

    if (showExportDialog) {
        ExportBackupDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { password ->
                viewModel.exportBackup(password)
                showExportDialog = false
            }
        )
    }

    if (showImportPasswordDialog) {
        ImportPasswordDialog(
            onDismiss = {
                showImportPasswordDialog = false
                viewModel.clearBackupState()
            },
            onConfirm = { password ->
                viewModel.importBackup(password)
                showImportPasswordDialog = false
            }
        )
    }
}

/** "Sync automatically", as on the phone. The desktop reaches only computers; phones sync in on
 *  the schedule set on each phone, which [note] says. */
@Composable
private fun DesktopAutoSyncRow() {
    val quickSync = koinViewModel<com.apagon.rhythm.ui.sync.QuickSyncViewModel>()
    val minutes by quickSync.autoSyncMinutes.collectAsState()
    minutes?.let { m ->
        com.apagon.rhythm.ui.sync.AutoSyncRow(
            minutes = m,
            onChange = quickSync::setAutoSyncMinutes,
            note = "Your phone syncs with this computer on the schedule you pick on the phone (Settings → Data Management → Sync automatically)."
        )
    }
}

/** Settings → Data Management → Sync with Phone & Computers, laid out like the phone's Sync with
 *  Computers screen: what it is, whether this computer is reachable (with the pairing QR), every
 *  device with its switch, adding a computer, What to sync, and the rarer options. Desktop is
 *  always the sync server for phones; Tailscale is the only way in (see SyncAddressMonitor). */
@Composable
private fun SyncPageContent(viewModel: DesktopSettingsViewModel) {
    var showSync by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    val quickSync = koinViewModel<com.apagon.rhythm.ui.sync.QuickSyncViewModel>()
    val vaultFileSyncEnabled by viewModel.vaultFileSyncEnabled.collectAsState()
    val listen by viewModel.syncListenState.collectAsState()

    if (showGuide) com.apagon.rhythm.ui.help.SyncSetupGuideSheet(onDismiss = { showGuide = false })

    Text(
        "Sync directly with Bori on your phone and your other computers, over Tailscale. Switch a device off to stop syncing with it without unpairing. Choose below what this computer sends and receives.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    SyncStatusCard(listen, onHowTo = { showGuide = true })
    TextButton(onClick = { showGuide = true }) { Text("How to set up sync") }

    SettingsSection("Your devices") {
        // As the phone's "Your computers": every device, each with its own on/off switch.
        com.apagon.rhythm.ui.sync.SyncDeviceList(quickSync)
    }

    SettingsSection("Sync automatically") {
        DesktopAutoSyncRow()
    }

    SettingsSection("Add another computer") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Phones pair by scanning the QR code above. To sync with another computer, enter the address it shows under Ready for your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            run {
                var newComputer by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newComputer,
                        onValueChange = { newComputer = it },
                        label = { Text("Another computer's address (host:port)") },
                        colors = crystalBareTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        colors = crystalButtonColors(),
                        enabled = newComputer.isNotBlank(),
                        onClick = { quickSync.addComputerAndSync(newComputer); newComputer = "" }
                    ) {
                        Text("Add and Sync")
                    }
                }
            }
        }
    }

    WhatToSyncSection(viewModel.themePreferences)

    SettingsSection("Options") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Forgets every watermark before syncing, so rows made before sync ever ran (or
            // before an entity type joined sync) go across too. Once per device after a
            // meaningful sync-scope change, not routinely.
            TextButton(onClick = { quickSync.forceFullResyncThenSync() }) {
                Text("Force full resync (first-time or after an update)")
            }
            TextButton(onClick = { showSync = !showSync }) {
                Text(if (showSync) "Hide experimental options" else "Experimental options")
            }
            if (showSync) {

                // Stage 3.5 — optional, default-off vault *file* sync. Off unless explicitly
                // turned on here, independently on each device (not something a sync round can
                // enable on the peer) — see ThemePreferences.vaultFileSyncEnabled's own KDoc.
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = DesktopLayout.itemSpacing)
                        .crystalCardSurface().padding(DesktopLayout.compactCardPadding),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsRow("Sync Notes vault files (experimental)") {
                        Switch(
                            checked = vaultFileSyncEnabled,
                            onCheckedChange = { viewModel.setVaultFileSyncEnabled(it) },
                            colors = crystalSwitchColors()
                        )
                    }
                    Text(
                        "Also syncs the Notes Markdown vault folder (files, manifest, and images) " +
                            "so an external tool indexed against it stays current on both devices. " +
                            "Private notebooks are always excluded. Both devices must turn this on " +
                            "for files to transfer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * The top of the Sync with Phone & Computers page. Ready: the address and the QR code a phone scans. Not ready: why,
 * and the way to the setup guide. Follows SyncAddressMonitor, so it turns ready by itself once
 * Tailscale comes up.
 */
@Composable
private fun SyncStatusCard(listen: SyncListenState, onHowTo: () -> Unit) {
    val qrCodeRenderer = koinInject<QrCodeRenderer>()
    val ready = listen is SyncListenState.Listening && listen.viaTailscale
    Row(
        modifier = Modifier.fillMaxWidth().crystalCardSurface().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(CircleShape).background(
                        when {
                            ready -> Color(0xFF34A853)
                            listen is SyncListenState.Starting -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when (listen) {
                        is SyncListenState.Starting -> "Starting sync…"
                        is SyncListenState.Failed -> "Sync can't start"
                        is SyncListenState.Listening -> if (listen.viaTailscale) "Ready for your phone" else "Tailscale isn't running on this computer"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            when (listen) {
                is SyncListenState.Listening -> if (listen.viaTailscale) {
                    Text(
                        "On your phone: Bori → Settings → Data Management → Sync with Computers → Scan QR Code.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            "This computer's address: ${listen.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        "Your phone reaches this computer through Tailscale, a free private network. " +
                            "Install it and sign in with the same account as your phone; Bori notices " +
                            "within about 15 seconds, no restart needed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(colors = crystalButtonColors(), onClick = onHowTo) { Text("How to set up sync") }
                }
                is SyncListenState.Failed -> {
                    Text(listen.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Text(
                        "Bori tries again every 15 seconds. Closing the other program, or another copy of Bori, fixes it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SyncListenState.Starting -> {}
            }
        }
        if (ready) {
            Box(Modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp)) {
                qrCodeRenderer.QrCodeImage(text = (listen as SyncListenState.Listening).address, modifier = Modifier.size(168.dp))
            }
        }
    }
}

/** Minimal password prompt for an encrypted export — a checkbox reveals the field, matching
 *  the plan's "functional, not elaborate" bar for this UI. */
@Composable
private fun ExportBackupDialog(onDismiss: () -> Unit, onConfirm: (CharArray?) -> Unit) {
    var encrypt by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = encrypt, onCheckedChange = { encrypt = it })
                    Text("Encrypt this backup")
                }
                if (encrypt) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(if (encrypt && password.isNotEmpty()) password.toCharArray() else null) },
                enabled = !encrypt || password.isNotEmpty()
            ) { Text("Export") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Shown when an import turns out to need a password — either up front (the file is an
 *  encrypted envelope) or again after a wrong guess. */
@Composable
private fun ImportPasswordDialog(onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password Required") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This backup is password protected. Enter the password to restore it.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password.toCharArray()) },
                enabled = password.isNotEmpty()
            ) { Text("Unlock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** "← Data Management" above a Data Management sub-page (Sync, Recently Deleted), as the phone's
 *  back arrow on those pages. */
@Composable
private fun SubPageBackRow(onBack: () -> Unit) {
    TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Data Management")
    }
}

/**
 * "What to sync", the phone's eight switches with the phone's wording. Each one gates both
 * directions for that category on this computer only (SyncEngine.gateByWhatToSync).
 */
@Composable
private fun WhatToSyncSection(prefs: ThemePreferences) {
    val scope = rememberCoroutineScope()
    @Composable
    fun row(label: String, flow: kotlinx.coroutines.flow.Flow<Boolean>, set: suspend (Boolean) -> Unit) {
        val on by flow.collectAsState(initial = true)
        SettingsRow(label) {
            Switch(checked = on, onCheckedChange = { v -> scope.launch { set(v) } }, colors = crystalSwitchColors())
        }
    }
    SettingsSection("What to sync") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Each switch stops this computer sending and receiving that kind of item. It doesn't change what your other devices send.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            row("Habits", prefs.syncHabitsEnabled, prefs::setSyncHabitsEnabled)
            row("Reminders", prefs.syncRemindersEnabled, prefs::setSyncRemindersEnabled)
            row("Events", prefs.syncEventsEnabled, prefs::setSyncEventsEnabled)
            row("To-dos", prefs.syncTodosEnabled, prefs::setSyncTodosEnabled)
            row("Clock (alarms & timers)", prefs.syncClockEnabled, prefs::setSyncClockEnabled)
            row("Notes", prefs.syncNotesEnabled, prefs::setSyncNotesEnabled)
            row("Journal", prefs.syncJournalEnabled, prefs::setSyncJournalEnabled)
            row("Appearance", prefs.syncPreferencesEnabled, prefs::setSyncPreferencesEnabled)
        }
    }
}

/** Settings → Tutorials, as the phone's: the sync setup guide and the welcome intro, any time. */
@Composable
private fun HelpSectionContent() {
    var showGuide by remember { mutableStateOf(false) }
    if (showGuide) com.apagon.rhythm.ui.help.SyncSetupGuideSheet(onDismiss = { showGuide = false })
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsSection("Tutorials") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HelpRow(
                    "Set up computer sync",
                    "Connect your phone and computers over Tailscale: install, sign in, scan the QR code.",
                    "Open"
                ) { showGuide = true }
                HelpRow(
                    "Welcome intro",
                    "The welcome pages Bori showed the first time you opened it.",
                    "Replay"
                ) { com.apagon.rhythm.ui.onboarding.OnboardingReplay.requested = true }
                HelpRow(
                    "Downloads and install help",
                    "Every Bori release for Windows and Linux, with install steps for each system.",
                    "Open GitHub"
                ) { runCatching { uri.openUri(com.apagon.rhythm.ui.help.SyncGuideLinks.RELEASES) } }
            }
        }
    }
}

@Composable
private fun HelpRow(title: String, description: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onClick) { Text(action) }
    }
}

@Composable
private fun AboutSectionContent(onShowPrivacyPolicy: () -> Unit, themePreferences: ThemePreferences) {
    val scope = rememberCoroutineScope()
    // The Borikén Easter egg, as on the phone: ten clicks on the version within three seconds.
    // Nothing hints at it beforehand. (The desktop has no paid tier, so nothing else gates it.)
    var clicks by remember { mutableStateOf(0) }
    var firstClickAt by remember { mutableStateOf(0L) }
    var showUnlocked by remember { mutableStateOf(false) }
    if (showUnlocked) {
        BorikenUnlockDialog(
            onDismiss = { showUnlocked = false },
            onTryIt = {
                showUnlocked = false
                scope.launch { themePreferences.setBorikenMode(true) }
            }
        )
    }
    SettingsSection("About") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Bori", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                "Version ${AppInfo.VERSION}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        val now = System.currentTimeMillis()
                        if (clicks == 0 || now - firstClickAt > 3_000) { clicks = 0; firstClickAt = now }
                        clicks++
                        if (clicks >= 10) {
                            clicks = 0
                            scope.launch { themePreferences.setBorikenUnlocked() }
                            showUnlocked = true
                        }
                    }
                    .padding(vertical = 4.dp)
            )
            TextButton(onClick = onShowPrivacyPolicy, modifier = Modifier.padding(start = 0.dp)) {
                Text("Privacy Policy")
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier.fillMaxWidth()
                .crystalCardSurface(fill = MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp)
        ) { content() }
    }
}

@Composable
private fun SettingsRow(label: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        trailing()
    }
}

/**
 * Desktop port of the Android app's `SettingsExpandableCard` (`ui/settings/SettingsScreen.kt`) — a
 * collapsed-by-default card with an optional one-line summary, used for every Appearance setting as
 * on Android.
 */
@Composable
private fun SettingsExpandableCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    summary: String? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().crystalCardSurface()) {
        Column(Modifier.animateContentSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (!expanded && summary != null) {
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.padding(bottom = 4.dp)) { content() }
            }
        }
    }
}

/** Matches Android's `CrystalMeshPickerRow` — 5 swatches per row, ragged last row padded with
 * spacers rather than stretched. */
private const val MESH_SWATCHES_PER_ROW = 5

private fun crystalMeshLabel(mesh: CrystalMesh): String = when (mesh) {
    CrystalMesh.AURORA -> "Aurora"
    CrystalMesh.EMBER -> "Ember"
    CrystalMesh.VERDANT -> "Verdant"
    CrystalMesh.MIST -> "Mist"
    CrystalMesh.ROSE -> "Rose"
    CrystalMesh.TIDE -> "Tide"
    CrystalMesh.INDIGO -> "Indigo"
    CrystalMesh.ORCHID -> "Orchid"
    CrystalMesh.DUNE -> "Dune"
    CrystalMesh.GRAPHITE -> "Graphite"
    CrystalMesh.CUSTOM -> "Custom"
}

/** One mesh swatch, drawn with the same [drawCrystalMeshField] the real ambient field uses, so a
 * swatch cannot drift from what tapping it produces. */
@Composable
private fun CrystalMeshSwatch(
    mesh: CrystalMesh?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val base = if (dark) AmbientBaseDark else AmbientBaseLight
    // null is the Borikén swatch: the flag field. Every other swatch shows its own field, never
    // the flag that may be on screen behind it.
    val blobs = if (mesh == null) borikenFieldBlobs(dark)
                else crystalOwnFieldBlobs(mesh, dark)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier
                )
                .clickable(onClick = onClick)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCrystalMeshField(base = base, blobs = blobs, dark = dark)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (mesh == null) "Borikén" else crystalMeshLabel(mesh),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
