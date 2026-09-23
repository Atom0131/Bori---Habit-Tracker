package com.apagon.rhythm.ui.onboarding

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NotificationsActive
import com.apagon.rhythm.ui.util.FutureGridAnimation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.apagon.rhythm.ui.util.EditorialTitle
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val features: List<String> = emptyList()
)

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Everything in one rhythm",
        description = "One app for the whole day:",
        icon = Icons.Default.AddTask,
        features = listOf(
            "Habits with streaks",
            "Notes with templates",
            "Journal & calendar",
            "Smart reminders"
        )
    ),
    OnboardingPage(
        title = "Your Data, Your Device",
        description = "Everything stays on your phone. No cloud, no tracking.",
        icon = Icons.Default.Shield
    ),
    OnboardingPage(
        title = "Enable Notifications",
        description = "So reminders and alarms actually reach you on time.",
        icon = Icons.Default.NotificationsActive
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onComplete) {
                    Text("Skip")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                val page = onboardingPages[index]
                OnboardingPageContent(page = page)
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Pager Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(onboardingPages.size) { i ->
                        val selected = pagerState.currentPage == i
                        Box(
                            modifier = Modifier
                                .size(if (selected) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < onboardingPages.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (pagerState.currentPage < onboardingPages.size - 1) "Next" else "Get Started",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (page.title == "Everything in one rhythm") {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    FutureGridAnimation(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize(),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        EditorialTitle(
            text = page.title,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(16.dp))

        if (page.title == "Enable Notifications") {
            val context = LocalContext.current
            var hasNotificationPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else true
                )
            }
            
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasNotificationPermission = isGranted
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Notification Permission Button
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !hasNotificationPermission,
                        colors = if (hasNotificationPermission) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.buttonColors()
                    ) {
                        Icon(if (hasNotificationPermission) Icons.Default.Check else Icons.Default.NotificationsActive, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (hasNotificationPermission) "Notifications Allowed" else "Enable Notifications")
                    }
                }

                if (hasNotificationPermission) {
                    Text(
                        "All set! You're ready to stay on track.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (page.features.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    page.features.forEach { feature -> OnboardingFeatureItem(feature) }
                }
            }
        } else {
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnboardingFeatureItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun ComposeColor(color: Long): ComposeColor = ComposeColor(color)
