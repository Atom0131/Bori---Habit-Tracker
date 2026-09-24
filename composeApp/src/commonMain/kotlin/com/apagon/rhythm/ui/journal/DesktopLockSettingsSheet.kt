package com.apagon.rhythm.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.repository.LockType
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Desktop port of AddReflectionSheet.kt's LockSettingsSheet. Biometrics and
 * the Pro paywall are both dropped — desktop is unconditionally Pro (Stage
 * 6/9 decision) and has no biometric hardware story, so neither branch can
 * ever fire on Android either once isPro is always true; keeping the dead
 * parameters would just be unreachable code with no way to exercise it here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopLockSettingsSheet(
    currentLockType: LockType,
    currentPin: String?,
    currentPassword: String?,
    onDismiss: () -> Unit,
    onSavePin: (String?) -> Unit,
    onSavePassword: (String?) -> Unit,
    onClearAll: () -> Unit
) {
    var mode by remember { mutableStateOf("MAIN") }
    var tempInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when (mode) {
                "MAIN" -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Journal Security", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DesktopLockOptionRow(
                            title = "Numeric PIN",
                            description = if (currentLockType == LockType.PIN) "Currently active" else "4-digit numeric code",
                            selected = currentLockType == LockType.PIN,
                            onClick = { mode = "SET_PIN"; tempInput = ""; error = null }
                        )
                        DesktopLockOptionRow(
                            title = "Custom Password",
                            description = if (currentLockType == LockType.PASSWORD) "Currently active" else "Alphanumeric security",
                            selected = currentLockType == LockType.PASSWORD,
                            onClick = { mode = "SET_PASSWORD"; tempInput = ""; error = null }
                        )
                        if (currentLockType != LockType.NONE) {
                            TextButton(
                                onClick = { mode = "VERIFY_DISABLE"; tempInput = ""; error = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Disable All Locks", fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                "SET_PIN" -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Set PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Enter a 4-digit code to lock your journal.", style = MaterialTheme.typography.bodyMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(4) { i ->
                            Box(
                                Modifier.size(16.dp).clip(CircleShape).background(
                                    if (i < tempInput.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        }
                    }

                    DesktopLockKeypad(
                        tempInput = tempInput,
                        onDigit = { if (tempInput.length < 4) tempInput += it },
                        onClear = { if (tempInput.isNotEmpty()) tempInput = tempInput.dropLast(1) },
                        onConfirm = { if (tempInput.length == 4) { onSavePin(tempInput); onDismiss() } }
                    )

                    TextButton(onClick = { mode = "MAIN" }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
                }

                "SET_PASSWORD" -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Set Password", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Choose a strong alphanumeric password.", style = MaterialTheme.typography.bodyMedium)

                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = { tempInput = it },
                        label = { Text("New password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onSavePassword(tempInput); onDismiss() },
                        enabled = tempInput.length >= 4,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Password") }
                    TextButton(onClick = { mode = "MAIN" }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }

                "VERIFY_DISABLE" -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Verify Lock", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Confirm your current ${if (currentLockType == LockType.PIN) "PIN" else "password"} to disable security.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (currentLockType == LockType.PIN) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            repeat(4) { i ->
                                Box(
                                    Modifier.size(16.dp).clip(CircleShape).background(
                                        if (i < tempInput.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )
                            }
                        }
                        if (error != null) {
                            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                        DesktopLockKeypad(
                            tempInput = tempInput,
                            onDigit = { if (tempInput.length < 4) tempInput += it },
                            onClear = { if (tempInput.isNotEmpty()) tempInput = tempInput.dropLast(1) },
                            onConfirm = {
                                if (tempInput == currentPin) { onClearAll(); onDismiss() }
                                else { error = "Incorrect PIN"; tempInput = "" }
                            }
                        )
                    } else {
                        OutlinedTextField(value = tempInput, onValueChange = { tempInput = it }, label = { Text("Enter password") }, modifier = Modifier.fillMaxWidth())
                        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                        Button(
                            onClick = {
                                if (tempInput == currentPassword) { onClearAll(); onDismiss() }
                                else { error = "Incorrect Password"; tempInput = "" }
                            },
                            enabled = tempInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Disable Lock") }
                    }
                    TextButton(onClick = { mode = "MAIN" }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun DesktopLockKeypad(tempInput: String, onDigit: (String) -> Unit, onClear: () -> Unit, onConfirm: () -> Unit) {
    listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "✓")
    ).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            row.forEach { key ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable {
                            when (key) {
                                "C" -> onClear()
                                "✓" -> onConfirm()
                                else -> onDigit(key)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DesktopLockOptionRow(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}
