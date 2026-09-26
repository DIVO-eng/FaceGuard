package com.faceguard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConsentScreen(onAccepted: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Welcome to FaceGuard") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Before you continue, here's exactly what this app does:", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            ConsentPoint("What it captures",
                "A single front-camera photo, only when a lock-screen unlock attempt " +
                "fails (and optionally on successful unlock), using Android's official " +
                "Device Admin event — this app never sees your actual PIN, password, or fingerprint.")

            ConsentPoint("Where it's stored",
                "Photos are encrypted (AES-256, Android Keystore) and kept only in this " +
                "app's private storage — never in your Photos/Gallery app — unless you " +
                "explicitly export a photo yourself.")

            ConsentPoint("How long it's kept",
                "You choose the retention period in Settings (1 hour up to 7 days, a " +
                "daily deletion time, or never auto-delete). Expired photos are securely deleted.")

            ConsentPoint("Cloud backup",
                "Off by default. If you turn it on, only photos you've allowed are " +
                "encrypted and uploaded to the cloud provider you choose.")

            ConsentPoint("What it needs from you",
                "Camera permission, notification permission, and Device Admin activation " +
                "(so Android can tell this app an unlock attempt failed). You can revoke " +
                "any of these at any time in Android Settings.")

            Spacer(Modifier.height(24.dp))
            Button(onClick = onAccepted, modifier = Modifier.fillMaxWidth()) {
                Text("I understand — continue setup")
            }
        }
    }
}

@Composable
private fun ConsentPoint(title: String, body: String) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
