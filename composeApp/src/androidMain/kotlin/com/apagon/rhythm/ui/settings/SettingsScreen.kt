package com.apagon.rhythm.ui.settings
import com.apagon.rhythm.core.time.*

import org.koin.compose.viewmodel.koinViewModel

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.apagon.rhythm.data.billing.BillingRepository
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.apagon.rhythm.BuildConfig
import com.apagon.rhythm.data.preferences.DarkReadability
import com.apagon.rhythm.data.preferences.ThemeMode
import com.apagon.rhythm.notifications.CHANNEL_ID
import com.apagon.rhythm.ui.util.ColorPickerRow
import com.apagon.rhythm.ui.util.HabitCard
import com.apagon.rhythm.ui.util.MomentumButton
import com.apagon.rhythm.ui.util.TutorialViewModel
import com.apagon.rhythm.ui.util.PermissionUtils
import com.apagon.rhythm.ui.util.ProPaywallSheet
import com.apagon.rhythm.ui.util.findActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.apagon.rhythm.CrashLogger
import com.apagon.rhythm.ui.util.ColorPickerViewModel
import kotlinx.datetime.LocalDate

/** Collapsible group header used to organize related settings sections together. */
@Composable
private fun SettingsGroupHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/** One row in the Tutorials settings group — resets a single tutorial's "seen" flag so it
 * shows again next time the user reaches that feature. */
@Composable
private fun TutorialReplayRow(title: String, onReplay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onReplay) { Text("Replay") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDeleted: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    viewModel: SettingsViewModel = koinViewModel(),
    colorPickerViewModel: ColorPickerViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val userName     by viewModel.userName.collectAsState(initial = "")
    val userNickname by viewModel.userNickname.collectAsState(initial = "")
    val userAge      by viewModel.userAge.collectAsState(initial = "")
    val userPronouns by viewModel.userPronouns.collectAsState(initial = "")
    val profilePictureUri by viewModel.profilePictureUri.collectAsState(initial = null)
    var showEditProfile by remember { mutableStateOf(false) }

    val currentMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val accentColorIndex by viewModel.accentColorIndex.collectAsState(initial = 4)
    val accentColorArgb by viewModel.accentColorArgb.collectAsState(initial = null)
    val darkReadability by viewModel.darkReadability.collectAsState(initial = DarkReadability.STANDARD)
    val isPro by viewModel.isPro.collectAsState(initial = false)

    var showPaywall by remember { mutableStateOf(false) }
    var paywallReason by remember { mutableStateOf<String?>(null) }
    val homeViewCalendar by viewModel.homeViewCalendar.collectAsState()
    val calendarIntegrationEnabled by viewModel.calendarIntegrationEnabled.collectAsState()
    val backupState by viewModel.backupState.collectAsState()

    var showManageCalendars by remember { mutableStateOf(false) }
    // Permissions State
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    var canScheduleExactAlarms by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true
        )
    }

    var canUseFullScreenIntent by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.canUseFullScreenIntent()
            } else true
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasNotificationPermission = it }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                      permissions[Manifest.permission.WRITE_CALENDAR] == true
        hasCalendarPermission = granted
        if (granted) {
            viewModel.setCalendarIntegrationEnabled(true)
        }
    }

    // Re-check permissions when returning to settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
                hasCalendarPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    canScheduleExactAlarms = alarmManager.canScheduleExactAlarms()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    canUseFullScreenIntent = nm.canUseFullScreenIntent()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Pending (unsaved) state — mirrors current values until Save is tapped
    var pendingThemeMode by remember { mutableStateOf(currentMode) }
    var pendingHomeViewCalendar by remember { mutableStateOf(homeViewCalendar) }

    // Sync pending values when the backing StateFlow emits its real value after
    // WhileSubscribed resumes — avoids phantom "unsaved changes" on screen open
    LaunchedEffect(currentMode) { pendingThemeMode = currentMode }
    LaunchedEffect(homeViewCalendar) { pendingHomeViewCalendar = homeViewCalendar }
    var vipTapCount by remember { mutableIntStateOf(0) }
    var showVipDialog by remember { mutableStateOf(false) }
    var vipPasscodeInput by remember { mutableStateOf("") }
    var vipPasscodeError by remember { mutableStateOf(false) }
    var showNowBarInfo by remember { mutableStateOf(false) }
    var crashLogActive by remember { mutableStateOf(CrashLogger.isActive(context)) }

    // Collapsible section expanded state (all collapsed by default)
    var themeExpanded by remember { mutableStateOf(false) }
    var layoutExpanded by remember { mutableStateOf(false) }
    var appearanceGroupExpanded by remember { mutableStateOf(false) }
    var notificationsGroupExpanded by remember { mutableStateOf(false) }
    var calendarGroupExpanded by remember { mutableStateOf(false) }
    var dataManagementGroupExpanded by remember { mutableStateOf(false) }
    var tutorialsGroupExpanded by remember { mutableStateOf(false) }

    val hasUnsavedChanges = pendingThemeMode != currentMode ||
        pendingHomeViewCalendar != homeViewCalendar

    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showArchiveSheet by remember { mutableStateOf(false) }
    val archivedHabits by viewModel.archivedHabits.collectAsState()

    var showTodoArchiveSheet by remember { mutableStateOf(false) }
    val archivedTodos by viewModel.archivedTodos.collectAsState()

    BackHandler(enabled = hasUnsavedChanges) { showDiscardDialog = true }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingImportUri = it
            showImportConfirmDialog = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {}
                viewModel.setProfilePictureUri(it.toString())
            }
        }
    )

    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearBackupState()
            }
            is BackupState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearBackupState()
            }
            else -> {}
        }
    }

    var showPrivacyPolicy by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { if (hasUnsavedChanges) showDiscardDialog = true else onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasUnsavedChanges) {
                        TextButton(onClick = {
                            viewModel.setThemeMode(pendingThemeMode)
                            viewModel.setHomeViewCalendar(pendingHomeViewCalendar)
                        }) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) 120.dp else 0.dp)
        ) {
            // Debug crash log card — hidden unless unlocked via the 7-tap version-number passcode
            if (crashLogActive) {
                item { CrashLogDebugCard(onDisable = { crashLogActive = false }) }
            }

            // Profile Section
            item {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditProfile = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePictureUri != null) {
                                AsyncImage(
                                    model = profilePictureUri,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (userName.isBlank()) "Set your name" else userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (userNickname.isNotBlank()) {
                                Text(
                                    "@$userNickname",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // ── Appearance group: App Accent Color, Theme, Layout ──────────────
            item {
                SettingsGroupHeader(
                    title = "Appearance",
                    expanded = appearanceGroupExpanded,
                    onToggle = { appearanceGroupExpanded = !appearanceGroupExpanded }
                )
            }
            if (appearanceGroupExpanded) {
                // Accent Color (Theme wide)
                item {
                    Text(
                        text = "App Accent Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Text(
                        text = "Choose a primary color for the whole app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    HabitCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            ColorPickerRow(
                                colorIndex = accentColorIndex,
                                colorArgb = accentColorArgb,
                                isPro = isPro,
                                onColorSelected = { idx, argb -> viewModel.setAccentColor(idx, argb) },
                                onShowPaywall = {
                                    paywallReason = it
                                    showPaywall = true
                                },
                                viewModel = colorPickerViewModel
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                // Theme — collapsible
                item {
                    HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(Modifier.animateContentSize()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { themeExpanded = !themeExpanded }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    "Theme",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!themeExpanded) {
                                    Text(
                                        when (pendingThemeMode) {
                                            ThemeMode.SYSTEM -> "System default"
                                            ThemeMode.LIGHT  -> "Light"
                                            ThemeMode.DARK   -> "Dark"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Icon(
                                    if (themeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (themeExpanded) "Collapse" else "Expand",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (themeExpanded) {
                                Column {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    ThemeMode.entries.forEachIndexed { i, mode ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { pendingThemeMode = mode }
                                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                        ) {
                                            RadioButton(
                                                selected = pendingThemeMode == mode,
                                                onClick = { pendingThemeMode = mode }
                                            )
                                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge)
                                        }
                                        if (i < ThemeMode.entries.size - 1) {
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                        }
                                    }

                                    // Night readability — only meaningful in dark mode
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                        Text(
                                            "Night Readability",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Controls how much surface contrast is added in dark mode",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                            DarkReadability.entries.forEachIndexed { idx, option ->
                                                SegmentedButton(
                                                    selected = darkReadability == option,
                                                    onClick = { viewModel.setDarkReadability(option) },
                                                    shape = SegmentedButtonDefaults.itemShape(
                                                        index = idx,
                                                        count = DarkReadability.entries.size
                                                    ),
                                                    label = {
                                                        Text(
                                                            when (option) {
                                                                DarkReadability.STANDARD    -> "Standard"
                                                                DarkReadability.COMFORTABLE -> "Comfortable"
                                                                DarkReadability.HIGH        -> "High"
                                                            },
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                // Layout — collapsible
                item {
                    HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(Modifier.animateContentSize()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { layoutExpanded = !layoutExpanded }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    "Layout",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!layoutExpanded) {
                                    Text(
                                        if (pendingHomeViewCalendar) "Calendar" else "List",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Icon(
                                    if (layoutExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (layoutExpanded) "Collapse" else "Expand",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (layoutExpanded) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = if (pendingHomeViewCalendar) Icons.Default.CalendarMonth else Icons.AutoMirrored.Filled.ViewList,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Today View Style", style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                "How habits are organized on the home screen",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        SegmentedButton(
                                            selected = !pendingHomeViewCalendar,
                                            onClick = { pendingHomeViewCalendar = false },
                                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                            icon = { Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                        ) { Text("List") }
                                        SegmentedButton(
                                            selected = pendingHomeViewCalendar,
                                            onClick = { pendingHomeViewCalendar = true },
                                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                        ) { Text("Calendar") }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val listBorderColor = if (!pendingHomeViewCalendar) MaterialTheme.colorScheme.primary else Color.Transparent
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(2.dp, listBorderColor, RoundedCornerShape(8.dp))
                                                .clickable { pendingHomeViewCalendar = false },
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    "List",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (!pendingHomeViewCalendar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                                // Mock items
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    repeat(3) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape))
                                                            Spacer(Modifier.width(6.dp))
                                                            Box(Modifier.height(4.dp).fillMaxWidth().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        val calBorderColor = if (pendingHomeViewCalendar) MaterialTheme.colorScheme.primary else Color.Transparent
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(2.dp, calBorderColor, RoundedCornerShape(8.dp))
                                                .clickable { pendingHomeViewCalendar = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    "Calendar",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (pendingHomeViewCalendar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                                // Mock grid
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    repeat(3) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            repeat(3) {
                                                                Box(Modifier.size(14.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // ── Notifications & Permissions group: System Permissions, Notification Sound, Live Timer Corner Chip ──
            item {
                SettingsGroupHeader(
                    title = "Notifications & Permissions",
                    expanded = notificationsGroupExpanded,
                    onToggle = { notificationsGroupExpanded = !notificationsGroupExpanded }
                )
            }
            if (notificationsGroupExpanded) {
                // Permissions Section
                item {
                    Text(
                        text = "System Permissions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            PermissionStatusRow(
                                title = "Notifications",
                                description = "Required for reminders and timer alerts",
                                isGranted = hasNotificationPermission,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                            PermissionStatusRow(
                                title = "Exact Alarms",
                                description = "Ensures timers finish precisely on time",
                                isGranted = canScheduleExactAlarms,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                        com.apagon.rhythm.ui.util.PermissionUtils.safeStartActivity(context, intent)
                                    }
                                }
                            )

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                PermissionStatusRow(
                                    title = "Full Screen Alerts",
                                    description = "Shows timer controls while screen is locked",
                                    isGranted = canUseFullScreenIntent,
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
                                        com.apagon.rhythm.ui.util.PermissionUtils.safeStartActivity(context, intent)
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                // Notification Sound — always visible, single action (opens system settings)
                item {
                    HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
                                    }
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Notification Sound", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Tap to customize",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Now Bar / Live Timer Corner Chip info card
                item {
                    Spacer(Modifier.height(8.dp))
                    HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNowBarInfo = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Timer Corner Chip", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Show running timers in the status bar corner",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // ── Calendar Integration group ──────────────
            item {
                SettingsGroupHeader(
                    title = "Calendar Integration",
                    expanded = calendarGroupExpanded,
                    onToggle = { calendarGroupExpanded = !calendarGroupExpanded }
                )
            }
            if (calendarGroupExpanded) {
            item {
                HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Enable Calendar Sync",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Show external events in your Today list",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = calendarIntegrationEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !hasCalendarPermission) {
                                        calendarPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CALENDAR,
                                                Manifest.permission.WRITE_CALENDAR
                                            )
                                        )
                                    } else {
                                        viewModel.setCalendarIntegrationEnabled(enabled)
                                    }
                                }
                            )
                        }

                        if (calendarIntegrationEnabled) {
                            Spacer(Modifier.height(16.dp))
                            MomentumButton(
                                text = "Manage Calendars",
                                onClick = { showManageCalendars = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Rhythm syncs securely with calendars already connected to your device (like Google or Outlook). To add a new calendar, please add the account in your Android System Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // ── Data Management group: Backup & Restore, Habit Archive, To-do Archive, Recently Deleted ──
            item {
                SettingsGroupHeader(
                    title = "Data Management",
                    expanded = dataManagementGroupExpanded,
                    onToggle = { dataManagementGroupExpanded = !dataManagementGroupExpanded }
                )
            }
            if (dataManagementGroupExpanded) {
                // Backup & Restore — always visible
                item {
                    Text(
                        text = "Backup & Restore",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val date = LocalDate.now().toString()
                                    exportLauncher.launch("rhythm_backup_$date.json")
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Export") }
                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json"))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Import") }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                // Stage 13: sync with the desktop app over Tailscale
                item {
                    Text(
                        text = "Sync",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSync() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sync with Desktop", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Connect over Tailscale to sync habits with the desktop app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                // Archived Habits
                item {
                    Text(
                        text = "Habit Archive",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showArchiveSheet = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("View Archived Habits", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${archivedHabits.size} archived habits available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                // Archived To-dos
                item {
                    Text(
                        text = "To-do Archive",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTodoArchiveSheet = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("View Archived To-dos", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${archivedTodos.size} archived to-dos available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                // Recently Deleted
                item {
                    Text(
                        text = "Recently Deleted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDeleted() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Trash Bin", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Restore or permanently delete items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            // ── Tutorials group: replay any contextual tutorial you skipped or dismissed ──
            item {
                SettingsGroupHeader(
                    title = "Tutorials",
                    expanded = tutorialsGroupExpanded,
                    onToggle = { tutorialsGroupExpanded = !tutorialsGroupExpanded }
                )
            }
            if (tutorialsGroupExpanded) {
                item {
                    val tutorialViewModel: TutorialViewModel = koinViewModel()
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Replay a tutorial you skipped or dismissed — it'll show again next time you open that screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        TutorialReplayRow("Creating a habit") { tutorialViewModel.setHasSeenHabitCreationTutorial(false) }
                        TutorialReplayRow("Checklist habits") { tutorialViewModel.setHasSeenChecklistHabitTutorial(false) }
                        TutorialReplayRow("Notes templates") { tutorialViewModel.setHasSeenNotesTemplateTutorial(false) }
                        TutorialReplayRow("Quick actions (swipe gestures)") { tutorialViewModel.setHasSeenSwipeGestureTutorial(false) }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // About Section
                Text(
                    text = "About",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HabitCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Rhythm", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "Version ${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable {
                                        vipTapCount++
                                        if (vipTapCount >= 7) {
                                            vipTapCount = 0
                                            showVipDialog = true
                                        }
                                    }
                                )
                            }
                            if (isPro) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("PRO") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = MaterialTheme.colorScheme.primary,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        TextButton(
                            onClick = { showPrivacyPolicy = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Privacy Policy", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                            }
                        }

                        TextButton(
                            onClick = { viewModel.queryPurchases() },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Restore Purchases", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                            }
                        }

                        if (isPro) {
                            TextButton(
                                onClick = {
                                    PermissionUtils.safeStartActivity(context, viewModel.manageSubscriptionsIntent())
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Manage Subscription", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                                }
                            }
                        }

                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showEditProfile) {
        EditProfileSheet(
            currentName = userName,
            currentNickname = userNickname,
            currentAge = userAge,
            currentPronouns = userPronouns,
            profilePictureUri = profilePictureUri,
            onDismiss = { showEditProfile = false },
            onPickPhoto = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemovePhoto = {
                viewModel.setProfilePictureUri(null)
            },
            onSave = { name, nickname, age, pronouns ->
                viewModel.setUserName(name)
                viewModel.setUserNickname(nickname)
                viewModel.setUserAge(age)
                viewModel.setUserPronouns(pronouns)
                showEditProfile = false
            }
        )
    }

    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("Restore backup?") },
            text = { Text("This will overwrite all current data with the backup. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { viewModel.importBackup(it) }
                    showImportConfirmDialog = false
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Leave without saving?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            }
        )
    }

    if (showPaywall) {
        ProPaywallSheet(
            reason = paywallReason,
            onDismiss = { 
                showPaywall = false
                paywallReason = null
            },
            onUpgrade = { productId ->
                val activity = context.findActivity()
                if (activity != null) {
                    viewModel.startBillingFlow(activity, productId)
                }
                showPaywall = false
                paywallReason = null
            }
        )
    }

    if (showPrivacyPolicy) {
        PrivacyPolicySheet(onDismiss = { showPrivacyPolicy = false })
    }

    if (showArchiveSheet) {
        ArchivedHabitsSheet(
            habits = archivedHabits,
            onDismiss = { showArchiveSheet = false },
            onUnarchive = { viewModel.unarchiveHabit(it) }
        )
    }

    if (showTodoArchiveSheet) {
        ArchivedTodosSheet(
            todos = archivedTodos,
            onDismiss = { showTodoArchiveSheet = false },
            onUnarchive = { viewModel.unarchiveTodo(it) }
        )
    }

    if (showManageCalendars) {
        ManageCalendarsSheet(
            onDismiss = { showManageCalendars = false },
            viewModel = viewModel
        )
    }

    if (showNowBarInfo) {
        NowBarInfoSheet(onDismiss = { showNowBarInfo = false })
    }

    if (showVipDialog) {
        AlertDialog(
            onDismissRequest = {
                showVipDialog = false
                vipPasscodeInput = ""
                vipPasscodeError = false
            },
            title = { Text("Enter Code") },
            text = {
                Column {
                    OutlinedTextField(
                        value = vipPasscodeInput,
                        onValueChange = { vipPasscodeInput = it; vipPasscodeError = false },
                        placeholder = { Text("Activation code") },
                        isError = vipPasscodeError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    if (vipPasscodeError) {
                        Text(
                            "Invalid code.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    when (vipPasscodeInput.trim()) {
                        BillingRepository.VIP_PROMO_PASSCODE -> {
                            showVipDialog = false
                            vipPasscodeInput = ""
                            vipPasscodeError = false
                            val activity = context.findActivity()
                            if (activity != null) viewModel.startVipPromoFlow(activity)
                        }
                        CrashLogger.DEBUG_PASSCODE -> {
                            showVipDialog = false
                            vipPasscodeInput = ""
                            vipPasscodeError = false
                            CrashLogger.setActive(context, true)
                            crashLogActive = true
                        }
                        else -> {
                            vipPasscodeError = true
                        }
                    }
                }) { Text("Activate") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showVipDialog = false
                    vipPasscodeInput = ""
                    vipPasscodeError = false
                }) { Text("Cancel") }
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowBarInfoSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Live Timer Corner Chip",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "On Samsung devices (One UI 7+) and phones running Android 16, your active timers can appear as a live countdown chip in the corner of the screen — the same way Samsung Clock and Google Clock do.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Text(
                        "Samsung limits the corner chip to its own apps by default. You need to turn on one developer setting to allow all apps including Rhythm to use it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            HorizontalDivider()

            Text(
                "How to turn it on",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            val steps = listOf(
                "Open your phone's Settings app.",
                "Tap About Phone, then Software Information.",
                "Tap Build number 7 times in a row until you see \"Developer mode enabled\". Skip this if Developer Options already appears in Settings.",
                "Go back to the main Settings screen and open Developer Options.",
                "Search for or scroll to Live notifications for all apps and turn it ON.",
                "Start any timer in Rhythm — it will now appear as a live countdown chip in the corner of your screen."
            )

            steps.forEachIndexed { i, step ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        step,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    try {
                        // Android 16+: direct per-app Live Notification settings page.
                        // Pre-16: fall back to Developer Options where the global toggle lives.
                        val intent = if (android.os.Build.VERSION.SDK_INT >= 36) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (android.os.Build.VERSION.SDK_INT >= 36) "Open Live Notification Settings"
                    else "Open Developer Options"
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}

