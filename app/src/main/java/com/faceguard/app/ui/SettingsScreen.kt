@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.faceguard.app.ui
import androidx.compose.ui.Alignment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.faceguard.app.data.FaceGuardDatabase
import com.faceguard.app.util.SecurePrefs
import kotlinx.coroutines.launch

private data class RetentionOption(val label: String, val hours: Int)
private val retentionOptions = listOf(
    RetentionOption("1 hour", 1),
    RetentionOption("6 hours", 6),
    RetentionOption("24 hours", 24),
    RetentionOption("3 days", 72),
    RetentionOption("7 days", 168),
    RetentionOption("Never delete", -1)
)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { SecurePrefs.get(context) }
    val scope = rememberCoroutineScope()

    var captureFailed by remember { mutableStateOf(prefs.getBoolean(SecurePrefs.KEY_CAPTURE_ON_FAILED, true)) }
    var captureSuccess by remember { mutableStateOf(prefs.getBoolean(SecurePrefs.KEY_CAPTURE_ON_SUCCESS, false)) }
    var unknownOnly by remember { mutableStateOf(prefs.getBoolean(SecurePrefs.KEY_CAPTURE_UNKNOWN_ONLY, false)) }
    var sensitivity by remember { mutableStateOf(prefs.getInt(SecurePrefs.KEY_FACE_MATCH_SENSITIVITY, 70).toFloat()) }
    var retentionHours by remember { mutableStateOf(prefs.getInt(SecurePrefs.KEY_RETENTION_HOURS, 24)) }
    var backupEnabled by remember { mutableStateOf(prefs.getBoolean(SecurePrefs.KEY_BACKUP_ENABLED, false)) }
    var wifiOnly by remember { mutableStateOf(prefs.getBoolean(SecurePrefs.KEY_WIFI_ONLY_BACKUP, true)) }
    var appLock by remember { mutableStateOf(prefs.getBoolean(SecurePrefs.KEY_APP_LOCK_ENABLED, true)) }
    var showPhotoInNotif by remember { mutableStateOf(prefs.getBoolean(SecurePrefs.KEY_SHOW_PHOTO_IN_NOTIFICATION, false)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp)) {

            item { SectionHeader("Capture triggers") }
            item {
                SwitchRow("Capture on failed authentication", captureFailed) {
                    captureFailed = it; prefs.edit().putBoolean(SecurePrefs.KEY_CAPTURE_ON_FAILED, it).apply()
                }
            }
            item {
                SwitchRow(
                    "Capture on successful authentication",
                    captureSuccess,
                    subtitle = "PIN/pattern only — Android doesn't forward biometric-unlock events to any third-party app"
                ) {
                    captureSuccess = it; prefs.edit().putBoolean(SecurePrefs.KEY_CAPTURE_ON_SUCCESS, it).apply()
                }
            }
            item {
                SwitchRow("Only keep photos of unrecognized faces", unknownOnly) {
                    unknownOnly = it; prefs.edit().putBoolean(SecurePrefs.KEY_CAPTURE_UNKNOWN_ONLY, it).apply()
                }
            }

            item { SectionHeader("Face recognition sensitivity") }
            item {
                Slider(
                    value = sensitivity,
                    onValueChange = {
                        sensitivity = it
                        prefs.edit().putInt(SecurePrefs.KEY_FACE_MATCH_SENSITIVITY, it.toInt()).apply()
                    },
                    valueRange = 0f..100f
                )
                Text("On-device only — nothing is sent to any face-recognition cloud service.",
                    style = MaterialTheme.typography.bodySmall)
            }

            item { SectionHeader("Retention") }
            items(retentionOptions) { option ->
                RadioRow(
                    label = option.label,
                    selected = retentionHours == option.hours,
                    onSelect = {
                        retentionHours = option.hours
                        prefs.edit().putInt(SecurePrefs.KEY_RETENTION_HOURS, option.hours).apply()
                    }
                )
            }

            item { SectionHeader("Backup") }
            item {
                SwitchRow("Enable cloud backup", backupEnabled) {
                    backupEnabled = it; prefs.edit().putBoolean(SecurePrefs.KEY_BACKUP_ENABLED, it).apply()
                }
            }
            if (backupEnabled) {
                item {
                    SwitchRow("Wi-Fi only", wifiOnly) {
                        wifiOnly = it; prefs.edit().putBoolean(SecurePrefs.KEY_WIFI_ONLY_BACKUP, it).apply()
                    }
                }
            }

            item { SectionHeader("App security") }
            item {
                SwitchRow("Require unlock to open FaceGuard", appLock) {
                    appLock = it; prefs.edit().putBoolean(SecurePrefs.KEY_APP_LOCK_ENABLED, it).apply()
                }
            }
            item {
                SwitchRow(
                    "Show captured photo in notification",
                    showPhotoInNotif,
                    subtitle = "Off by default for privacy"
                ) {
                    showPhotoInNotif = it; prefs.edit().putBoolean(SecurePrefs.KEY_SHOW_PHOTO_IN_NOTIFICATION, it).apply()
                }
            }

            item { SectionHeader("Data") }
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch { FaceGuardDatabase.get(context).authEventDao().deleteAll() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) { Text("Delete all records") }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
