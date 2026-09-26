package com.faceguard.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** All app settings + the wrapped DB passphrase live here, Keystore-backed. */
object SecurePrefs {

    private const val FILE_NAME = "faceguard_secure_prefs"

    // Setting keys
    const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    const val KEY_CAPTURE_ON_FAILED = "capture_on_failed"
    const val KEY_CAPTURE_ON_SUCCESS = "capture_on_success"
    const val KEY_CAPTURE_UNKNOWN_ONLY = "capture_unknown_only"
    const val KEY_RETENTION_HOURS = "retention_hours"          // -1 = never
    const val KEY_DAILY_DELETE_HOUR = "daily_delete_hour"      // -1 = disabled
    const val KEY_BACKUP_ENABLED = "backup_enabled"
    const val KEY_BACKUP_PROVIDER = "backup_provider"
    const val KEY_WIFI_ONLY_BACKUP = "wifi_only_backup"
    const val KEY_MAX_STORAGE_MB = "max_storage_mb"
    const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    const val KEY_SHOW_PHOTO_IN_NOTIFICATION = "show_photo_in_notification"
    const val KEY_FACE_MATCH_SENSITIVITY = "face_match_sensitivity" // 0-100
    const val KEY_DB_PASSPHRASE_B64 = "wrapped_db_passphrase"
    const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

    private var instance: SharedPreferences? = null

    fun get(context: Context): SharedPreferences {
        return instance ?: synchronized(this) {
            instance ?: create(context).also { instance = it }
        }
    }

    private fun create(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Sensible defaults matching the spec
    fun applyDefaults(context: Context) {
        val p = get(context)
        if (!p.contains(KEY_RETENTION_HOURS)) {
            p.edit()
                .putBoolean(KEY_MONITORING_ENABLED, false) // OFF until owner opts in
                .putBoolean(KEY_CAPTURE_ON_FAILED, true)
                .putBoolean(KEY_CAPTURE_ON_SUCCESS, false)
                .putBoolean(KEY_CAPTURE_UNKNOWN_ONLY, false)
                .putInt(KEY_RETENTION_HOURS, 24)
                .putInt(KEY_DAILY_DELETE_HOUR, -1)
                .putBoolean(KEY_BACKUP_ENABLED, false)
                .putBoolean(KEY_WIFI_ONLY_BACKUP, true)
                .putInt(KEY_MAX_STORAGE_MB, 500)
                .putBoolean(KEY_APP_LOCK_ENABLED, true)
                .putBoolean(KEY_SHOW_PHOTO_IN_NOTIFICATION, false)
                .putInt(KEY_FACE_MATCH_SENSITIVITY, 70)
                .putBoolean(KEY_ONBOARDING_COMPLETE, false)
                .apply()
        }
    }
}
