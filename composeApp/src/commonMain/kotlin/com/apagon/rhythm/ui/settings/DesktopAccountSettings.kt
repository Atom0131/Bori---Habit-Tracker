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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.platform.ImageBitmapLoader
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalTextFieldShape
import com.apagon.rhythm.ui.util.RhythmSheet
import org.koin.compose.koinInject
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

// Desktop counterpart to androidMain's AccountSettings.kt (Stage 11). Drops
// CrashLogDebugCard (Android-only CrashLogger, no desktop equivalent, and
// the whole hidden-gesture feature it served is excluded per the roadmap).
// Swaps Coil's AsyncImage (androidMain-only, content:// URIs) for the
// project's own ImageBitmapLoader (Stage 9's Journal/Notes photo pattern) —
// same absolute-file-path model FilePicker/PhotoStorage already use here.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopPrivacyPolicySheet(onDismiss: () -> Unit) {
    RhythmSheet(
        onDismiss = onDismiss,
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
            Text("Privacy Policy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Text(
                text = "Last Updated: April 25, 2026",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Bori is committed to protecting your privacy. This policy explains how we handle your data.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            DesktopPrivacySection(
                title = "1. Data Collection",
                content = "We take a 'local-first' approach. All your habit data, notes, profile information, and photos are stored directly on your device. We do not transmit or store this personal data on our servers."
            )

            DesktopPrivacySection(
                title = "2. Backups & Portability",
                content = "When you use the 'Export' feature, a copy of your data is created on your device's storage in a location you choose. We do not have access to these files once they are created."
            )

            DesktopPrivacySection(
                title = "3. Third-Party Services",
                content = "We use Google Play Billing for Pro subscriptions and standard Google Play Services for app stability. These services may collect diagnostic information as governed by Google's Privacy Policy."
            )

            DesktopPrivacySection(
                title = "4. Your Control",
                content = "Since your data is stored locally, you have complete control. You can delete your data at any time by clearing the app's cache/storage or by deleting individual habits within the app."
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                colors = crystalButtonColors(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text("I Understand") }
        }
    }
}

@Composable
fun DesktopPrivacySection(title: String, content: String) {
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
fun DesktopEditProfileSheet(
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
    var name by remember { mutableStateOf(currentName) }
    var nickname by remember { mutableStateOf(currentNickname) }
    var age by remember { mutableStateOf(currentAge) }
    var pronouns by remember { mutableStateOf(currentPronouns) }
    val imageLoader = koinInject<ImageBitmapLoader>()

    RhythmSheet(
        onDismiss = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

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
                        imageLoader.LoadedImage(
                            path = profilePictureUri,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            "+",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
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
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
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
                colors = crystalTextFieldColors(),
                shape = crystalTextFieldShape(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname") },
                singleLine = true,
                colors = crystalTextFieldColors(),
                shape = crystalTextFieldShape(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    singleLine = true,
                    colors = crystalTextFieldColors(),
                    shape = crystalTextFieldShape(),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = pronouns,
                    onValueChange = { pronouns = it },
                    label = { Text("Pronouns") },
                    singleLine = true,
                    colors = crystalTextFieldColors(),
                    shape = crystalTextFieldShape(),
                    modifier = Modifier.weight(1.5f)
                )
            }
            Button(
                onClick = { onSave(name.trim(), nickname.trim(), age.trim(), pronouns.trim()) },
                colors = crystalButtonColors(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Save") }
        }
    }
}
