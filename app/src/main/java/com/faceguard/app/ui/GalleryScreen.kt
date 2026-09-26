@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.faceguard.app.ui
import androidx.compose.ui.Alignment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.BitmapFactory
import com.faceguard.app.data.EncryptedPhotoStore
import com.faceguard.app.data.FaceGuardDatabase
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { FaceGuardDatabase.get(context).authEventDao() }
    val photoStore = remember { EncryptedPhotoStore(context) }
    val events by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val photoEvents = events.filter { it.encryptedPhotoPath != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Gallery") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (photoEvents.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No captured photos yet")
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photoEvents) { event ->
                val bytes = remember(event.id) { photoStore.read(event.encryptedPhotoPath!!) }
                val bitmap = remember(bytes) {
                    bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
                }
                Card {
                    Column {
                        bitmap?.let {
                            Image(it, contentDescription = null, modifier = Modifier.fillMaxWidth().height(140.dp))
                        }
                        Column(Modifier.padding(8.dp)) {
                            Text(
                                SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(event.timestampMillis)),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("Backup: ${event.backupStatus.name}", style = MaterialTheme.typography.labelSmall)
                            event.scheduledDeletionMillis?.let {
                                val remainingMin = ((it - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
                                Text("Deletes in ${remainingMin}m", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
