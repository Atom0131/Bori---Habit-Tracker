package com.apagon.rhythm.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.apagon.rhythm.CrashLogger
import com.apagon.rhythm.ui.util.EditorialTitle
import com.apagon.rhythm.ui.util.HabitCard

@Composable
fun CrashLogDebugCard(onDisable: () -> Unit) {
    val context = LocalContext.current
    var logText by remember { mutableStateOf(CrashLogger.read(context)) }
    var showDialog by remember { mutableStateOf(false) }

    HabitCard(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Debug: Crash Log (active)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (logText != null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (logText != null) "Crash detected — tap to view" else "No crashes recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (logText != null) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
        TextButton(
            onClick = {
                CrashLogger.setActive(context, false)
                logText = null
                onDisable()
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        ) { Text("Disable Debug Logging") }
    }

    if (showDialog && logText != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Crash Log") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = logText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { CrashLogger.share(context) }) { Text("Share") }
            },
            dismissButton = {
                TextButton(onClick = {
                    CrashLogger.clear(context)
                    logText = null
                    showDialog = false
                }) { Text("Clear") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicySheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditorialTitle("Privacy Policy")

            Text(
                text = "Last Updated: April 25, 2026",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Rhythm is committed to protecting your privacy. This policy explains how we handle your data.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            PrivacySection(
                title = "1. Data Collection",
                content = "We take a 'local-first' approach. All your habit data, notes, profile information, and photos are stored directly on your device. We do not transmit or store this personal data on our servers."
            )

            PrivacySection(
                title = "2. Backups & Portability",
                content = "When you use the 'Export' feature, a copy of your data is created on your device's storage in a location you choose. We do not have access to these files once they are created."
            )

            PrivacySection(
                title = "3. Third-Party Services",
                content = "We use Google Play Billing for Pro subscriptions and standard Google Play Services for app stability. These services may collect diagnostic information as governed by Google's Privacy Policy."
            )

            PrivacySection(
                title = "4. Your Control",
                content = "Since your data is stored locally, you have complete control. You can delete your data at any time by clearing the app's cache/storage or by deleting individual habits within the app."
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("I Understand")
            }
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    currentName: String,
    currentNickname: String,
    currentAge: String,
    currentPronouns: String,
    profilePictureUri: String?,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name     by remember { mutableStateOf(currentName) }
    var nickname by remember { mutableStateOf(currentNickname) }
    var age      by remember { mutableStateOf(currentAge) }
    var pronouns by remember { mutableStateOf(currentPronouns) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditorialTitle("Edit Profile")

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onPickPhoto() },
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
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (profilePictureUri != null) {
                TextButton(
                    onClick = onRemovePhoto,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = pronouns,
                    onValueChange = { pronouns = it },
                    label = { Text("Pronouns") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            Button(
                onClick = { onSave(name.trim(), nickname.trim(), age.trim(), pronouns.trim()) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Save") }
        }
    }
}
