package com.apagon.rhythm

import org.koin.android.ext.android.inject

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.apagon.rhythm.data.billing.BillingRepository
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.ui.habit.HabitListScreen
import com.apagon.rhythm.ui.onboarding.OnboardingScreen
import com.apagon.rhythm.ui.notes.NotesScreen
import com.apagon.rhythm.ui.notes.NotebookDetailScreen
import com.apagon.rhythm.ui.notes.NoteEditorScreen
import com.apagon.rhythm.ui.notes.NotesSearchScreen
import com.apagon.rhythm.ui.deleted.RecentlyDeletedScreen
import com.apagon.rhythm.ui.sync.SyncScreen
import com.apagon.rhythm.ui.reminders.ClockScreen
import com.apagon.rhythm.ui.settings.SettingsScreen
import com.apagon.rhythm.ui.stats.StatsScreen
import com.apagon.rhythm.ui.theme.RhythmThemedRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.apagon.rhythm.data.backup.BackupManager

private data class NavTab(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

private val navTabs = listOf(
    NavTab("list",      Icons.Filled.Home,          Icons.Outlined.Home,             "Today"),
    NavTab("stats",     Icons.Filled.AutoStories,   Icons.Outlined.AutoStories,      "Journal"),
    NavTab("clock",     Icons.Filled.Alarm,         Icons.Outlined.Alarm,            "Clock"),
    NavTab("notes",     Icons.Filled.NoteAlt,       Icons.Outlined.NoteAlt,          "Notes"),
)
class MainActivity : FragmentActivity() {

    val themePreferences: ThemePreferences by inject()
    val billingRepository: BillingRepository by inject()
    private val purchaseLauncher: com.apagon.rhythm.platform.AndroidPurchaseLauncher by inject()
    val backupManager: BackupManager by inject()
    val habitRepository: com.apagon.rhythm.data.repository.HabitRepository by inject()
    internal var pendingTabRoute by mutableStateOf<String?>(null)
    internal var pendingHabitId by mutableStateOf<Long?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        pendingTabRoute = intent?.getStringExtra(EXTRA_OPEN_TAB)
        pendingHabitId = intent?.getLongExtra(EXTRA_OPEN_HABIT_ID, -1L)?.takeIf { it != -1L }

        // Bust Samsung One UI's icon cache on first launch after each update
        val launchPrefs = getSharedPreferences("launch_prefs", MODE_PRIVATE)
        if (launchPrefs.getInt("version_code", -1) != BuildConfig.VERSION_CODE) {
            launchPrefs.edit().putInt("version_code", BuildConfig.VERSION_CODE).apply()
            com.apagon.rhythm.notifications.PackageReplacedReceiver.refreshSamsungIconCache(applicationContext)
        }

        billingRepository.queryPurchases()
        purchaseLauncher.attach(this)

        // Request the highest supported display refresh mode before the first frame is drawn.
        // Using preferredDisplayModeId (vs preferredRefreshRate) lets the system pick the
        // exact mode the display supports rather than approximating from a float Hz value.
        @Suppress("DEPRECATION")
        val disp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display
                   else windowManager.defaultDisplay
        disp?.supportedModes?.maxByOrNull { it.refreshRate }?.let { best ->
            val params = window.attributes
            params.preferredDisplayModeId = best.modeId
            window.attributes = params
        }

        var isLoading by mutableStateOf(true)
        splashScreen.setKeepOnScreenCondition { isLoading }

        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            LaunchedEffect(Unit) {
                isLoading = false
            }

            RhythmThemedRoot(themePreferences = themePreferences) {
                val hasSeenOnboarding by themePreferences.hasSeenOnboarding.collectAsState(initial = false)
                val coroutineScope = rememberCoroutineScope()

                if (!hasSeenOnboarding) {
                    OnboardingScreen(
                        onComplete = {
                            coroutineScope.launch {
                                themePreferences.setHasSeenOnboarding(true)
                                // Add sample habit
                                habitRepository.addHabit(
                                    com.apagon.rhythm.data.model.Habit(
                                        name = "Explore Rhythm",
                                        iconIndex = 0
                                    )
                                )
                            }
                        }
                    )
                } else {
                    val swipeSectionsEnabled by themePreferences.swipeSectionsEnabled.collectAsState(initial = false)
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (swipeSectionsEnabled) {
                            SwipeNavigationContent(
                                useNavRail = useNavRail,
                                themePreferences = themePreferences,
                                windowWidthSizeClass = windowSizeClass.widthSizeClass
                            )
                        } else {
                            StandardNavigationContent(
                                useNavRail = useNavRail,
                                themePreferences = themePreferences,
                                windowWidthSizeClass = windowSizeClass.widthSizeClass
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        purchaseLauncher.detach(this)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTabRoute = intent.getStringExtra(EXTRA_OPEN_TAB)
        pendingHabitId = intent.getLongExtra(EXTRA_OPEN_HABIT_ID, -1L).takeIf { it != -1L }
    }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
        const val EXTRA_OPEN_HABIT_ID = "open_habit_id"
    }
}

@Composable
private fun SwipeNavigationContent(
    useNavRail: Boolean,
    themePreferences: ThemePreferences,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val navController = rememberNavController()
    val pages = remember { navTabs.map { it.route } }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val pagerCoroutineScope = rememberCoroutineScope()

    val activity = LocalContext.current as? MainActivity
    LaunchedEffect(activity?.pendingTabRoute) {
        val tab = activity?.pendingTabRoute ?: return@LaunchedEffect
        val idx = navTabs.indexOfFirst { it.route == tab }
        if (idx >= 0) pagerState.animateScrollToPage(idx)
        activity.pendingTabRoute = null
    }

    Scaffold(
        bottomBar = {
            if (!useNavRail) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    navTabs.forEachIndexed { index, tab ->
                        val selected = pagerState.currentPage == index
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = null,
                            selected = selected,
                            onClick = {
                                pagerCoroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (useNavRail) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    navTabs.forEachIndexed { index, tab ->
                        val selected = pagerState.currentPage == index
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                pagerCoroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
            NavHost(
                navController = navController,
                startDestination = "pager",
                modifier = Modifier.weight(1f)
            ) {
                composable(
                    "pager",
                    exitTransition     = { fadeOut(tween(400, easing = EaseInOut)) },
                    popEnterTransition = { fadeIn(tween(400, easing = EaseInOut)) }
                ) {
                    HorizontalPager(state = pagerState) { page ->
                        PageContent(
                            route = pages[page],
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToNotebook = { id -> navController.navigate("notebook_detail/$id") },
                            onNavigateToSearch = { navController.navigate("notes_search") },
                            swipeNavigationEnabled = true,
                            pendingHabitId = activity?.pendingHabitId,
                            onHabitIdConsumed = { activity?.pendingHabitId = null }
                        )
                    }
                }
                sharedRoutes(navController, windowWidthSizeClass)
            }
        }
    }
}

@Composable
private fun StandardNavigationContent(
    useNavRail: Boolean,
    themePreferences: ThemePreferences,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val activity = LocalContext.current as? MainActivity
    LaunchedEffect(activity?.pendingTabRoute) {
        val tab = activity?.pendingTabRoute ?: return@LaunchedEffect
        navController.navigate(tab) { launchSingleTop = true }
        activity.pendingTabRoute = null
    }

    Scaffold(
        bottomBar = {
            if (!useNavRail) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    navTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) { launchSingleTop = true }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (useNavRail) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    navTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) { launchSingleTop = true }
                            },
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
            NavHost(
                navController = navController,
                startDestination = "list",
                modifier = Modifier.weight(1f)
            ) {
                navTabs.forEach { tab ->
                    composable(
                        tab.route,
                        enterTransition    = { fadeIn(tween(300, easing = EaseInOut)) },
                        exitTransition     = { fadeOut(tween(250, easing = EaseInOut)) },
                        popEnterTransition = { fadeIn(tween(300, easing = EaseInOut)) },
                        popExitTransition  = { fadeOut(tween(250, easing = EaseInOut)) }
                    ) {
                        PageContent(
                            route = tab.route,
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToNotebook = { id -> navController.navigate("notebook_detail/$id") },
                            onNavigateToSearch = { navController.navigate("notes_search") },
                            pendingHabitId = activity?.pendingHabitId,
                            onHabitIdConsumed = { activity?.pendingHabitId = null }
                        )
                    }
                }
                sharedRoutes(navController, windowWidthSizeClass)
            }
        }
    }
}

/**
 * Shared navigation routes used by both SwipeNavigationContent and StandardNavigationContent.
 * Eliminates duplication of settings, deleted, notebook_detail, and note_editor routes.
 */
private fun NavGraphBuilder.sharedRoutes(
    navController: NavHostController,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    composable(
        "settings",
        enterTransition    = { fadeIn(tween(400, easing = EaseInOut)) },
        exitTransition     = { fadeOut(tween(400, easing = EaseInOut)) }
    ) {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDeleted = { navController.navigate("deleted") },
            onNavigateToSync = { navController.navigate("sync") },
            windowWidthSizeClass = windowWidthSizeClass
        )
    }
    composable("deleted") {
        RecentlyDeletedScreen(onNavigateBack = { navController.popBackStack() })
    }
    composable("sync") {
        SyncScreen(onNavigateBack = { navController.popBackStack() })
    }
    composable("notes_search") {
        NotesSearchScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToNote = { noteId, nbId -> navController.navigate("note_editor/$noteId/$nbId") }
        )
    }
    composable("notebook_detail/{notebookId}") { backStackEntry ->
        val notebookId = backStackEntry.arguments?.getString("notebookId")?.toLongOrNull() ?: 0L
        NotebookDetailScreen(
            notebookId = notebookId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToNote = { noteId, nbId, template ->
                if (template != null) {
                    navController.navigate("note_editor/$noteId/$nbId?template=${android.net.Uri.encode(template)}")
                } else {
                    navController.navigate("note_editor/$noteId/$nbId")
                }
            }
        )
    }
    composable(
        route = "note_editor/{noteId}/{notebookId}?template={template}",
        arguments = listOf(
            androidx.navigation.navArgument("noteId") { type = androidx.navigation.NavType.LongType },
            androidx.navigation.navArgument("notebookId") { type = androidx.navigation.NavType.LongType },
            androidx.navigation.navArgument("template") {
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
        val notebookId = backStackEntry.arguments?.getLong("notebookId") ?: 0L
        val template = backStackEntry.arguments?.getString("template")
        NoteEditorScreen(
            noteId = noteId,
            notebookId = notebookId,
            template = template,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}

@Composable
private fun PageContent(
    route: String,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotebook: (Long) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    swipeNavigationEnabled: Boolean = false,
    pendingHabitId: Long? = null,
    onHabitIdConsumed: () -> Unit = {}
) {
    when (route) {
        "list"      -> HabitListScreen(
            onNavigateToSettings = onNavigateToSettings,
            pendingHabitId = pendingHabitId,
            onHabitIdConsumed = onHabitIdConsumed
        )
        "stats"     -> StatsScreen(onNavigateToSettings = onNavigateToSettings)
        "clock"     -> ClockScreen(onNavigateToSettings = onNavigateToSettings)
        "notes"     -> NotesScreen(
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToNotebook = onNavigateToNotebook,
            onNavigateToSearch = onNavigateToSearch
        )
    }
}
