package com.faceguard.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuthEventType {
    FAILED_UNLOCK,
    SUCCESSFUL_UNLOCK,
    REPEATED_FAILURES
}

enum class RecognitionStatus {
    UNKNOWN_FACE,
    RECOGNIZED_OWNER,
    NOT_EVALUATED, // recognition disabled in settings
    NO_FACE_DETECTED
}

enum class BackupStatus {
    NOT_APPLICABLE,   // backup disabled
    PENDING,
    BACKED_UP,
    FAILED
}

@Entity(tableName = "auth_events")
data class AuthEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val eventType: AuthEventType,
    val encryptedPhotoPath: String?,     // null if capture failed/permission absent
    val recognitionStatus: RecognitionStatus,
    val backupStatus: BackupStatus,
    val scheduledDeletionMillis: Long?,   // null = "never auto-delete"
    val notifiedOwner: Boolean = false
)
