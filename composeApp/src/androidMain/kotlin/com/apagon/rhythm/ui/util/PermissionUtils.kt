package com.apagon.rhythm.ui.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

object PermissionUtils {
    fun hasTechnicalPermissions(context: Context): Boolean {
        // We check for Exact Alarm permission (Android 12+)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun safeStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            if (intent.data != null) {
                try {
                    val fallbackIntent = Intent(intent.action)
                    context.startActivity(fallbackIntent)
                    return true
                } catch (ignored: Exception) {}
            }
            try {
                val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(appSettingsIntent)
                true
            } catch (ignored2: Exception) {
                Toast.makeText(
                    context,
                    "Unable to open settings. Please grant permissions in System Settings > Apps > Rhythm.",
                    Toast.LENGTH_LONG
                ).show()
                false
            }
        }
    }

    fun openTechnicalPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            safeStartActivity(context, intent)
        }
    }
}

/**
 * Composable function to check technical permissions directly in the UI logic.
 */
fun hasTechnicalPermissions(context: Context): Boolean = PermissionUtils.hasTechnicalPermissions(context)

@Composable
fun TechnicalPermissionDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exact Timing Required") },
        text = { 
            Text("To ensure your habit reminders and alarms arrive exactly on time, Rhythm needs the 'Exact Alarm' permission. On Android 14+, 'Full Screen Alerts' are also recommended for reliable notifications.") 
        },
        confirmButton = {
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    PermissionUtils.safeStartActivity(context, intent)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    PermissionUtils.safeStartActivity(context, intent)
                }
                onDismiss()
            }) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
