package com.faceguard.app.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * All encryption keys live in the Android Keystore (hardware-backed where the
 * device supports it) and never leave it in extractable form. FaceGuard never
 * stores, derives from, or has access to the device's actual PIN, password,
 * or biometric templates — those remain entirely inside Android's own
 * TrustZone/lock-screen subsystem, which no third-party app can read.
 */
object KeystoreHelper {

    private const val PROVIDER = "AndroidKeyStore"
    private const val PHOTO_KEY_ALIAS = "faceguard_photo_key"
    private const val DB_PASSPHRASE_ALIAS = "faceguard_db_passphrase_wrap_key"

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(PROVIDER).apply { load(null) }

    /** AES-256 key used by EncryptedFile (Jetpack Security) for photo encryption. */
    fun getOrCreatePhotoKey(): SecretKey {
        val ks = keyStore()
        (ks.getKey(PHOTO_KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            PHOTO_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    /**
     * SQLCipher needs a passphrase byte array, not a Keystore key directly.
     * We generate a random 256-bit passphrase once, wrap it with a
     * Keystore-resident AES key, and store only the wrapped bytes in
     * SharedPreferences (EncryptedSharedPreferences). This keeps the
     * plaintext DB passphrase out of app storage entirely.
     */
    fun generateRandomDbPassphrase(): ByteArray {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    fun dbPassphraseWrapKeyAlias() = DB_PASSPHRASE_ALIAS
}
