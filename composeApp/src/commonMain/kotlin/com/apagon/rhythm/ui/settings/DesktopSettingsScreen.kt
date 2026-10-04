package com.apagon.rhythm.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

/** Stage 15f: the section list a left rail drives — replacing the single scrolling LazyColumn of
 * every settings card stacked one after another. "Recently Deleted" moves here from the top-level
 * app sidebar (Stage 15a) per the confirmed decision that trash is secondary nav, not primary. */
private enum class SettingsSection(val label: String) {
    PROFILE("Profile"),
    APPEARANCE("Appearance"),
    LAYOUT("Layout & Calendar"),
    DATA("Data"),
    RECENTLY_DELETED("Recently Deleted"),
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
                SettingsSection.entries.forEach { section ->
                    val isSelected = section == selectedSection
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
                    SettingsSection.RECENTLY_DELETED -> DesktopRecentlyDeletedScreen()
                    else -> Column(
                        modifier = Modifier.fillMaxHeight().widthIn(max = DesktopLayout.contentMaxWidth).fillMaxWidth()
                            .verticalScroll(rememberScrollState()).padding(DesktopLayout.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        when (selectedSection) {
                            SettingsSection.PROFILE -> ProfileSectionContent(
                                userNickname = userNickname,
                                userName = userName,
                                userPronouns = userPronouns,
                                userAge = userAge,
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
                                crystalMeshCustomChroma = crystalMeshCustomChroma,
                                themeStyleExpanded = themeStyleExpanded,
                                onToggleThemeStyleExpanded = { themeStyleExpanded = !themeStyleExpanded },
                                crystalBackgroundExpanded = crystalBackgroundExpanded,
                                onToggleCrystalBackgroundExpanded = { crystalBackgroundExpanded = !crystalBackgroundExpanded },
                                crystalGlassExpanded = crystalGlassExpanded,
                                onToggleCrystalGlassExpanded = { crystalGlassExpanded = !crystalGlassExpanded },
                                colorPickerViewModel = colorPickerViewModel
                            )
                            SettingsSection.LAYOUT -> LayoutSectionContent(
                                viewModel = viewModel,
                                homeViewCalendar = homeViewCalendar,
                                swipeSectionsEnabled = swipeSectionsEnabled,
                                calendarIntegrationEnabled = calendarIntegrationEnabled,
                                runInBackground = runInBackground
                            )
                            SettingsSection.DATA -> DataSectionContent(
                                viewModel = viewModel,
                                backupState = backupState,
                                archivedHabits = archivedHabits,
                                archivedTodos = archivedTodos,
                                onShowArchivedHabits = { showArchivedHabits = true },
                                onShowArchivedTodos = { showArchivedTodos = true }
                            )
                            SettingsSection.ABOUT -> AboutSectionContent(onShowPrivacyPolicy = { showPrivacyPolicy = true })
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
    onEdit: () -> Unit
) {
    SettingsSection("Profile") {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    userNickname.ifBlank { userName.ifBlank { "Add your name" } },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (userPronouns.isNotBlank() || userAge.isNotBlank()) {
                    Text(
                        listOf(userPronouns, userAge).filter { it.isNotBlank() }.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onEdit) { Text("Edit") }
        }
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
            initialColor = crystalMeshCustomArgb?.let { Color(it) },
            initialChroma = crystalMeshCustomChroma,
            onSave = { color, chromaScale ->
                viewModel.setCrystalMeshCustom(color.toArgb(), chromaScale)
            },
            onPreview = { color, chromaScale ->
                viewModel.previewCrystalMeshCustom(color.toArgb(), chromaScale)
            },
            onCancelPreview = { viewModel.cancelCrystalMeshCustomPreview() },
            onDismiss = { showMeshCustomDialog = false }
        )
    }
    SettingsSection("Appearance") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingsRow("Theme") {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ThemeMode.entries.forEach { mode ->
                                    TextButton(onClick = { viewModel.setThemeMode(mode) }) {
                                        Text(
                                            mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                            fontWeight = if (mode == themeMode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        SettingsRow("AMOLED Black") {
                            Switch(checked = amoledMode, onCheckedChange = { viewModel.setAmoledMode(it) }, colors = crystalSwitchColors())
                        }
                        SettingsRow("Dark Readability") {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                DarkReadability.entries.forEach { level ->
                                    TextButton(onClick = { viewModel.setDarkReadability(level) }) {
                                        Text(
                                            level.name.lowercase().replaceFirstChar { it.uppercase() },
                                            fontWeight = if (level == darkReadability) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Accent Color", style = MaterialTheme.typography.bodyLarge)
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
    }

    SettingsExpandableCard(
                    title = "Theme Style",
                    expanded = themeStyleExpanded,
                    onToggle = onToggleThemeStyleExpanded,
                    summary = when (themeStyle) {
                        ThemeStyle.MATERIAL3 -> "Material 3"
                        ThemeStyle.EXPRESSIVE -> "Expressive"
                        ThemeStyle.CRYSTAL -> "Crystal"
                    }
                ) {
                    ThemeStyle.entries.forEachIndexed { i, style ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.setThemeStyle(style) }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                colors = crystalRadioButtonColors(),
                                selected = themeStyle == style,
                                onClick = { viewModel.setThemeStyle(style) }
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
                                        ThemeStyle.MATERIAL3 -> "The classic Rhythm look"
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
                }

    if (themeStyle == ThemeStyle.CRYSTAL) {
                    SettingsExpandableCard(
                        title = "Crystal Background",
                        expanded = crystalBackgroundExpanded,
                        onToggle = onToggleCrystalBackgroundExpanded,
                        summary = when (crystalBackground) {
                            CrystalBackground.MESH -> crystalMeshLabel(crystalMesh)
                            CrystalBackground.SOLID -> "Solid"
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
                            CrystalMesh.entries.chunked(MESH_SWATCHES_PER_ROW).forEach { rowMeshes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowMeshes.forEach { mesh ->
                                        CrystalMeshSwatch(
                                            mesh = mesh,
                                            selected = crystalBackground == CrystalBackground.MESH && crystalMesh == mesh,
                                            onClick = {
                                                if (mesh == CrystalMesh.CUSTOM) {
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
}

@Composable
private fun LayoutSectionContent(
    viewModel: DesktopSettingsViewModel,
    homeViewCalendar: Boolean,
    swipeSectionsEnabled: Boolean,
    calendarIntegrationEnabled: Boolean,
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
                "Keep Rhythm running when the window is closed, so alarms, reminders, and timers " +
                    "can still fire. Relaunch Rhythm to bring the window back.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

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
    onShowArchivedTodos: () -> Unit
) {
    var showExportDialog by remember { mutableStateOf(false) }
    // True once an import attempt reports back that the chosen file is encrypted (or that the
    // password just tried was wrong) — reusing one dialog for both "ask up front" and "retry"
    // rather than guessing whether a file needs a password before picking it.
    var showImportPasswordDialog by remember { mutableStateOf(false) }

    SettingsSection("Data Management") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showExportDialog = true }) { Text("Export Backup") }
                TextButton(onClick = { viewModel.importBackup() }) { Text("Import Backup") }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onShowArchivedHabits) { Text("Archived Habits (${archivedHabits.size})") }
                TextButton(onClick = onShowArchivedTodos) { Text("Archived To-dos (${archivedTodos.size})") }
            }
        }
    }

    SyncWithPhoneSection(viewModel)

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

/** Moved here from the Today screen so "Sync with phone" lives under Settings -> Data,
 *  matching Android's own placement (Settings -> Data Management -> "Pair with Desktop")
 *  instead of sitting on the Today screen. Desktop is always the sync server. */
@Composable
private fun SyncWithPhoneSection(viewModel: DesktopSettingsViewModel) {
    var showSync by remember { mutableStateOf(false) }
    var showQrCode by remember { mutableStateOf(false) }
    val qrCodeRenderer = koinInject<QrCodeRenderer>()
    val peerAddress by viewModel.peerAddress.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val vaultFileSyncEnabled by viewModel.vaultFileSyncEnabled.collectAsState()

    SettingsSection("Sync with Phone") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showSync = !showSync }) {
                Text(if (showSync) "Hide sync with phone" else "Pair with phone")
            }

            if (showSync) {
                // This device's own address, read-only — the user reads it off this line and
                // types it into the phone's peer-address field (desktop is always the sync
                // server). The QR toggle below is the easier path — same address, scanned
                // instead of typed.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = DesktopLayout.itemSpacing)
                        .crystalCardSurface().padding(DesktopLayout.compactCardPadding),
                    horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your address: ${viewModel.ownSyncAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    TextButton(onClick = { showQrCode = !showQrCode }) {
                        Text(if (showQrCode) "Hide QR Code" else "Show QR Code")
                    }
                }
                if (showQrCode) {
                    qrCodeRenderer.QrCodeImage(
                        text = viewModel.ownSyncAddressForPairing,
                        modifier = Modifier.size(200.dp).padding(top = 8.dp)
                    )
                }

                // Local sync test UI, still used for the reverse direction (desktop-initiates-
                // sync) and local dev testing.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = DesktopLayout.itemSpacing)
                        .crystalCardSurface().padding(DesktopLayout.compactCardPadding),
                    horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = peerAddress,
                        onValueChange = { viewModel.updatePeerAddress(it) },
                        label = { Text("Peer address (host:port)") },
                        colors = crystalBareTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Button(colors = crystalButtonColors(), onClick = { viewModel.syncNow() }) {
                        Text("Sync")
                    }
                }
                if (syncStatus != null) {
                    Text(syncStatus!!, modifier = Modifier.padding(top = 4.dp))
                }
                // Resets this device's "last synced at" watermark to 0 before syncing, so rows
                // created/last-edited before sync ever ran (or before an entity type was added to
                // sync's scope) are swept in too — the normal incremental sync only ever looks
                // forward from the watermark, so it can't retroactively pick these up on its own.
                // Meant to be tapped once per device after a meaningful sync-scope change, not
                // routinely.
                TextButton(onClick = { viewModel.forceFullResyncThenSync() }) {
                    Text("Force full resync (first-time or after an update)")
                }

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

@Composable
private fun AboutSectionContent(onShowPrivacyPolicy: () -> Unit) {
    SettingsSection("About") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Rhythm", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
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
 * collapsed-by-default card with an optional one-line summary, replacing the always-expanded
 * [SettingsSection] for the Crystal theme controls added in the layout-parity round. Desktop has no
 * `material-icons-extended` dependency, so the expand/collapse chevron is a plain glyph via `Text`
 * rather than `Icons.Default.ExpandMore`/`ExpandLess`, matching the convention already established
 * in `DesktopJournalWeekStrip.kt`.
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
                Text(
                    if (expanded) "▴" else "▾",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun CrystalMeshSwatch(mesh: CrystalMesh, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val base = if (dark) AmbientBaseDark else AmbientBaseLight
    val blobs = crystalFieldBlobs(mesh, dark)
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
            crystalMeshLabel(mesh),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
