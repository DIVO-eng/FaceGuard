package com.faceguard.app.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.faceguard.app.data.AuthEventType
import com.faceguard.app.service.MonitorService

/**
 * Android's Device Admin API is the ONLY legitimate way a third-party app can
 * learn that a lock-screen unlock attempt happened. It never exposes the PIN,
 * password, pattern, or biometric data itself — only a bare "failed" or
 * "succeeded" signal. This is a documented, non-exploit, publicly available
 * Android API (android.app.admin.DeviceAdminReceiver), the same one used by
 * MDM / "Find My Device"-style apps.
 *
 * Limitation we surface to the user honestly: onPasswordSucceeded() only
 * fires for PIN/pattern/password unlocks. Android does NOT forward successful
 * *biometric* (fingerprint/face) unlocks to any third-party app, admin or not.
 * The closest legitimate alternative for "capture on successful auth" is
 * FaceGuard's own in-app BiometricPrompt challenge (see BiometricGate.kt),
 * which the Settings screen explains clearly.
 */
class FaceGuardDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        MonitorService.start(context)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        MonitorService.stop(context)
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        MonitorService.enqueueAuthEvent(context, AuthEventType.FAILED_UNLOCK)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        MonitorService.enqueueAuthEvent(context, AuthEventType.SUCCESSFUL_UNLOCK)
    }
}
