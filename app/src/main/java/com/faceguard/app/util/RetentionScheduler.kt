package com.faceguard.app.util

import android.content.Context
import androidx.work.*
import com.faceguard.app.data.*
import java.util.concurrent.TimeUnit

object RetentionScheduler {

    fun scheduleDeletionCheck(context: Context) {
        val request = OneTimeWorkRequestBuilder<DeletionSweepWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "deletion_sweep", ExistingWorkPolicy.REPLACE, request
        )
    }

    fun scheduleBackup(context: Context) {
        val prefs = SecurePrefs.get(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (prefs.getBoolean(SecurePrefs.KEY_WIFI_ONLY_BACKUP, true))
                    NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "backup_upload", ExistingWorkPolicy.APPEND_OR_REPLACE, request
        )
    }

    /** Call once (e.g. from Application.onCreate) to set up the recurring daily-time deletion. */
    fun scheduleDailyDeletionIfConfigured(context: Context) {
        val hour = SecurePrefs.get(context).getInt(SecurePrefs.KEY_DAILY_DELETE_HOUR, -1)
        if (hour < 0) {
            WorkManager.getInstance(context).cancelUniqueWork("daily_deletion")
            return
        }
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            if (before(now)) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<DeletionSweepWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_deletion", ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }
}

class DeletionSweepWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = FaceGuardDatabase.get(applicationContext).authEventDao()
        val photoStore = EncryptedPhotoStore(applicationContext)
        val expired = dao.getExpiredEvents(System.currentTimeMillis())

        for (event in expired) {
            event.encryptedPhotoPath?.let { photoStore.delete(it) }
            dao.delete(event)
        }
        return Result.success()
    }
}

/**
 * Uploads pending photos to the owner's chosen provider. The actual
 * provider SDK call (Google Drive / Dropbox) is intentionally left as an
 * integration point — wire in the chosen SDK's authenticated upload call
 * here. This worker only handles encryption-before-upload + status bookkeeping.
 */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = FaceGuardDatabase.get(applicationContext).authEventDao()
        val photoStore = EncryptedPhotoStore(applicationContext)
        val pending = dao.getByBackupStatus(BackupStatus.PENDING)

        for (event in pending) {
            val path = event.encryptedPhotoPath ?: continue
            val bytes = photoStore.read(path) ?: continue
            val uploaded = CloudBackupClient.uploadEncrypted(applicationContext, path, bytes)
            dao.update(event.copy(backupStatus = if (uploaded) BackupStatus.BACKED_UP else BackupStatus.FAILED))
        }
        return Result.success()
    }
}

/** Integration point: plug in Google Drive / Dropbox / user-chosen provider SDK here. */
object CloudBackupClient {
    suspend fun uploadEncrypted(context: Context, fileName: String, bytes: ByteArray): Boolean {
        // TODO: implement provider-specific authenticated upload.
        // Must encrypt in transit (HTTPS, already required) and note that the
        // bytes here are already the app's own AES-256-GCM ciphertext, so the
        // provider never sees a plaintext photo.
        return false
    }
}
