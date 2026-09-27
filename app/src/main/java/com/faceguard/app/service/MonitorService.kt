package com.faceguard.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import com.faceguard.app.MainActivity
import com.faceguard.app.R
import com.faceguard.app.camera.FaceMatcher
import com.faceguard.app.camera.MonitorLifecycleOwnerHolder
import com.faceguard.app.camera.SilentCapture
import com.faceguard.app.data.*
import com.faceguard.app.util.SecurePrefs
import com.faceguard.app.util.RetentionScheduler
import kotlinx.coroutines.*

/**
 * Kept alive as a foreground service ONLY while monitoring is enabled by the
 * owner, so the OS doesn't kill the process between DeviceAdmin callbacks.
 * The persistent notification is required by Android policy for any
 * foreground service — we can't hide the fact that FaceGuard is running.
 */
class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val channelId = "faceguard_monitor_channel"

    companion object {
        private const val NOTIF_ID = 1001
        private const val ACTION_AUTH_EVENT = "com.faceguard.app.ACTION_AUTH_EVENT"
        private const val EXTRA_EVENT_TYPE = "extra_event_type"

        fun start(context: Context) {
            if (!SecurePrefs.get(context).getBoolean(SecurePrefs.KEY_MONITORING_ENABLED, false)) return
            val intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }

        fun enqueueAuthEvent(context: Context, type: AuthEventType) {
            val intent = Intent(context, MonitorService::class.java).apply {
                action = ACTION_AUTH_EVENT
                putExtra(EXTRA_EVENT_TYPE, type.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                buildIdleNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, buildIdleNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_AUTH_EVENT) {
            val typeName = intent.getStringExtra(EXTRA_EVENT_TYPE)
            val type = typeName?.let { runCatching { AuthEventType.valueOf(it) }.getOrNull() }
            if (type != null) handleAuthEvent(type)
        }
        return START_STICKY
    }

    private fun handleAuthEvent(type: AuthEventType) {
        val prefs = SecurePrefs.get(this)
        if (!prefs.getBoolean(SecurePrefs.KEY_MONITORING_ENABLED, false)) return

        val shouldCapture = when (type) {
            AuthEventType.FAILED_UNLOCK -> prefs.getBoolean(SecurePrefs.KEY_CAPTURE_ON_FAILED, true)
            AuthEventType.SUCCESSFUL_UNLOCK -> prefs.getBoolean(SecurePrefs.KEY_CAPTURE_ON_SUCCESS, false)
            AuthEventType.REPEATED_FAILURES -> true
        }
        if (!shouldCapture) return

        scope.launch {
            val jpeg = SilentCapture.captureFrontJpeg(applicationContext)
            var recognitionStatus = RecognitionStatus.NOT_EVALUATED
            var photoPath: String? = null

            if (jpeg != null) {
                recognitionStatus = FaceMatcher(applicationContext).evaluate(
                    jpeg, prefs.getInt(SecurePrefs.KEY_FACE_MATCH_SENSITIVITY, 70)
                )

                val onlyUnknown = prefs.getBoolean(SecurePrefs.KEY_CAPTURE_UNKNOWN_ONLY, false)
                val shouldStore = !onlyUnknown || recognitionStatus == RecognitionStatus.UNKNOWN_FACE
                        || recognitionStatus == RecognitionStatus.NOT_EVALUATED

                if (shouldStore) {
                    photoPath = EncryptedPhotoStore(applicationContext).save(jpeg)
                }
            }

            val retentionHours = prefs.getInt(SecurePrefs.KEY_RETENTION_HOURS, 24)
            val deletionAt = if (retentionHours < 0) null
            else System.currentTimeMillis() + retentionHours * 3_600_000L

            val event = AuthEventEntity(
                timestampMillis = System.currentTimeMillis(),
                eventType = type,
                encryptedPhotoPath = photoPath,
                recognitionStatus = recognitionStatus,
                backupStatus = if (prefs.getBoolean(SecurePrefs.KEY_BACKUP_ENABLED, false) && photoPath != null)
                    BackupStatus.PENDING else BackupStatus.NOT_APPLICABLE,
                scheduledDeletionMillis = deletionAt
            )

            val db = FaceGuardDatabase.get(applicationContext)
            db.authEventDao().insert(event)

            if (deletionAt != null) {
                RetentionScheduler.scheduleDeletionCheck(applicationContext)
            }
            if (event.backupStatus == BackupStatus.PENDING) {
                RetentionScheduler.scheduleBackup(applicationContext)
            }

            NotificationHelper.notifyAuthEvent(applicationContext, event)
        }
    }

    private fun buildIdleNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_body))
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "FaceGuard Monitoring", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
