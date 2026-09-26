@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.faceguard.app.ui
import androidx.compose.ui.Alignment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.faceguard.app.data.*
import com.faceguard.app.service.MonitorService
import com.faceguard.app.util.SecurePrefs
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(onOpenSettings: () -> Unit, onOpenGallery: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { SecurePrefs.get(context) }
    var monitoringEnabled by remember {
        mutableStateOf(prefs.getBoolean(SecurePrefs.KEY_MONITORING_ENABLED, false))
    }

    val dao = remember { FaceGuardDatabase.get(context).authEventDao() }
    val events by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
    val todayStart = calendar.timeInMillis
    val todayEvents = events.filter { it.timestampMillis >= todayStart }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FaceGuard") },
                actions = {
                    IconButton(onClick = onOpenGallery) { Icon(Icons.Filled.PhotoLibrary, "Gallery") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Settings") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatusCard(
                    monitoringEnabled = monitoringEnabled,
                    onToggle = { enabled ->
                        monitoringEnabled = enabled
                        prefs.edit().putBoolean(SecurePrefs.KEY_MONITORING_ENABLED, enabled).apply()
                        if (enabled) MonitorService.start(context) else MonitorService.stop(context)
                    }
                )
            }
            item {
                TodayStatsGrid(
                    total = todayEvents.size,
                    successful = todayEvents.count { it.eventType == AuthEventType.SUCCESSFUL_UNLOCK },
                    failed = todayEvents.count { it.eventType == AuthEventType.FAILED_UNLOCK },
                    photosCaptured = todayEvents.count { it.encryptedPhotoPath != null },
                    pendingBackup = todayEvents.count { it.backupStatus == BackupStatus.PENDING },
                    pendingDeletion = todayEvents.count { it.scheduledDeletionMillis != null }
                )
            }
            item {
                Text("Timeline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            items(events) { event -> EventRow(event) }
        }
    }
}

@Composable
private fun StatusCard(monitoringEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (monitoringEnabled) Icons.Filled.Shield else Icons.Filled.ShieldMoon,
                    contentDescription = null,
                    tint = if (monitoringEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(if (monitoringEnabled) "Protection active" else "Protection off", fontWeight = FontWeight.Bold)
                    Text(
                        if (monitoringEnabled) "Watching for lock-screen attempts" else "Turn on to start monitoring",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Switch(checked = monitoringEnabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun TodayStatsGrid(
    total: Int, successful: Int, failed: Int,
    photosCaptured: Int, pendingBackup: Int, pendingDeletion: Int
) {
    Column {
        Text("Today", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Events", total, Modifier.weight(1f))
            StatChip("Failed", failed, Modifier.weight(1f))
            StatChip("Success", successful, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Photos", photosCaptured, Modifier.weight(1f))
            StatChip("To backup", pendingBackup, Modifier.weight(1f))
            StatChip("Expiring", pendingDeletion, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatChip(label: String, value: Int, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun EventRow(event: AuthEventEntity) {
    val timeFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    ListItem(
        headlineContent = { Text(event.eventType.name.replace("_", " ")) },
        supportingContent = {
            Column {
                Text(timeFmt.format(Date(event.timestampMillis)), style = MaterialTheme.typography.bodySmall)
                Text(
                    "Recognition: ${event.recognitionStatus.name.replace("_", " ")} · Backup: ${event.backupStatus.name.replace("_", " ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        leadingContent = {
            Icon(
                if (event.eventType == AuthEventType.FAILED_UNLOCK) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                contentDescription = null
            )
        }
    )
    Divider()
}
