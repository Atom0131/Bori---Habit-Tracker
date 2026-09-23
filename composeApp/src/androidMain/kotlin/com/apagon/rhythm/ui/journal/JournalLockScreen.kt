package com.apagon.rhythm.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.biometric.BiometricPrompt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.apagon.rhythm.data.repository.LockType
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.MomentumButton

@Composable
fun JournalLockScreen(
    lockType: LockType,
    onUnlock: (String) -> Unit,
    biometricEnabled: Boolean = false,
    onBiometricSuccess: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var enteredCredential by remember { mutableStateOf("") }
    val context = LocalContext.current

    val showBiometricPrompt = {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onBiometricSuccess()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Journal Locked")
            .setSubtitle("Authenticate to unlock your journal")
            .setNegativeButtonText("Use ${if (lockType == LockType.PIN) "PIN" else "Password"}")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            showBiometricPrompt()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Journal Locked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (lockType == LockType.PIN) "Enter your passcode to continue" else "Enter your password to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        if (lockType == LockType.PIN) {
            // PIN indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val filled = index < enteredCredential.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("fingerprint", "0", "back")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        KeypadButton(
                            label = key,
                            showFingerprint = key == "fingerprint" && biometricEnabled,
                            onClick = {
                                when (key) {
                                    "back" -> if (enteredCredential.isNotEmpty()) enteredCredential = enteredCredential.dropLast(1)
                                    "fingerprint" -> if (biometricEnabled) showBiometricPrompt()
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
            // Full Alphanumeric Password
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
                
                MomentumButton(
                    text = "Unlock",
                    onClick = { onUnlock(enteredCredential) },
                    enabled = enteredCredential.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (biometricEnabled) {
                    IconButton(
                        onClick = showBiometricPrompt,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = "Biometric Unlock",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    showFingerprint: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(enabled = label.isNotEmpty() && (label != "fingerprint" || showFingerprint)) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (label == "back") {
            Icon(
                Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                modifier = Modifier.size(24.dp)
            )
        } else if (label == "fingerprint") {
            if (showFingerprint) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Biometric Unlock",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
