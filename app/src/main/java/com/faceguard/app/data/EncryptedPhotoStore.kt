package com.faceguard.app.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.util.UUID

/**
 * Photos live only in the app's private storage directory
 * (context.filesDir), inside app-specific encrypted files — never in
 * MediaStore/Gallery. They only leave this directory if the owner uses the
 * explicit "Export photos" action.
 */
class EncryptedPhotoStore(private val context: Context) {

    private val photosDir: File by lazy {
        File(context.filesDir, "secure_photos").apply { mkdirs() }
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }

    /** Returns the relative filename stored, to save in the DB row. */
    fun save(jpegBytes: ByteArray): String {
        val fileName = "evt_${UUID.randomUUID()}.jpg.enc"
        val file = File(photosDir, fileName)

        val encryptedFile = EncryptedFile.Builder(
            context, file, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { it.write(jpegBytes) }
        return fileName
    }

    fun read(fileName: String): ByteArray? {
        val file = File(photosDir, fileName)
        if (!file.exists()) return null
        val encryptedFile = EncryptedFile.Builder(
            context, file, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        return encryptedFile.openFileInput().use { it.readBytes() }
    }

    /** Secure delete: overwrite then unlink, not just unlink. */
    fun delete(fileName: String) {
        val file = File(photosDir, fileName)
        if (file.exists()) {
            try {
                val len = file.length()
                file.outputStream().use { it.write(ByteArray(len.toInt())) }
            } catch (_: Throwable) { /* best-effort overwrite */ }
            file.delete()
        }
    }

    fun currentUsageBytes(): Long = photosDir.listFiles()?.sumOf { it.length() } ?: 0L
}
