package com.faceguard.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.faceguard.app.util.SecurePrefs
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import android.util.Base64
import java.security.SecureRandom

@Database(entities = [AuthEventEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FaceGuardDatabase : RoomDatabase() {
    abstract fun authEventDao(): AuthEventDao

    companion object {
        @Volatile private var INSTANCE: FaceGuardDatabase? = null

        fun get(context: Context): FaceGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }
        }

        private fun build(context: Context): FaceGuardDatabase {
            SQLiteDatabase.loadLibs(context)
            val passphrase = getOrCreatePassphrase(context)
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(context, FaceGuardDatabase::class.java, "faceguard_events.db")
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * The DB passphrase is generated once with SecureRandom, then stored
         * only inside EncryptedSharedPreferences (itself Keystore-backed via
         * MasterKey). Plaintext passphrase never touches disk unencrypted.
         */
        private fun getOrCreatePassphrase(context: Context): ByteArray {
            val prefs = SecurePrefs.get(context)
            val existing = prefs.getString(SecurePrefs.KEY_DB_PASSPHRASE_B64, null)
            if (existing != null) {
                return Base64.decode(existing, Base64.NO_WRAP)
            }
            val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
            prefs.edit()
                .putString(SecurePrefs.KEY_DB_PASSPHRASE_B64, Base64.encodeToString(fresh, Base64.NO_WRAP))
                .apply()
            return fresh
        }
    }
}
