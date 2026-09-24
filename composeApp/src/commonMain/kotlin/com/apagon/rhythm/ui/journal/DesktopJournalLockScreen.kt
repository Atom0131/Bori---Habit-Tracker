package com.apagon.rhythm.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apagon.rhythm.data.repository.LockType

/**
 * Desktop port of JournalLockScreen.kt. Biometrics dropped entirely
 * (confirmed decision — PIN/password only on desktop v1): no
 * BiometricPrompt/FragmentActivity, no fingerprint keypad cell.
 */
@Composable
fun DesktopJournalLockScreen(
    lockType: LockType,
    onUnlock: (String) -> Unit,
    onCancel: () -> Unit = {}
) {
    var enteredCredential by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔒", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(24.dp))
        Text("Journal Locked", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (lockType == LockType.PIN) "Enter your passcode to continue" else "Enter your password to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        if (lockType == LockType.PIN) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(4) { index ->
                    val filled = index < enteredCredential.length
                    Box(
                        modifier = Modifier.size(16.dp).clip(CircleShape).background(
                            if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "back")
            )
            keys.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { key ->
                        DesktopKeypadButton(
                            label = key,
                            onClick = {
                                when (key) {
                                    "back" -> if (enteredCredential.isNotEmpty()) enteredCredential = enteredCredential.dropLast(1)
                                    "" -> {}
                                    else -> {
                                        if (enteredCredential.length < 4) {
                                            enteredCredential += key
                                            if (enteredCredential.length == 4) {
                                                onUnlock(enteredCredential)
                                                enteredCredential = ""
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                OutlinedTextField(
                    value = enteredCredential,
                    onValueChange = { enteredCredential = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Button(
                    onClick = { onUnlock(enteredCredential) },
                    enabled = enteredCredential.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Unlock") }
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun DesktopKeypadButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(enabled = label.isNotEmpty()) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (label) {
            "back" -> Text("⌫", fontSize = 24.sp)
            else -> Text(label, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
