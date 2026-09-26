package com.faceguard.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.faceguard.app.R
import com.faceguard.app.data.AuthEventEntity
import com.faceguard.app.data.AuthEventType
import com.faceguard.app.data.EncryptedPhotoStore
import com.faceguard.app.util.SecurePrefs
import java.text.SimpleDateFormat
import java.util.*

object NotificationHelper {

    private const val EVENT_CHANNEL = "faceguard_event_channel"
    private const val ALERT_CHANNEL = "faceguard_alert_channel"

    fun notifyAuthEvent(context: Context, event: AuthEventEntity) {
        ensureChannels(context)
        val prefs = SecurePrefs.get(context)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestampMillis))

        val label = when (event.eventType) {
            AuthEventType.FAILED_UNLOCK -> context.getString(R.string.event_failed_unlock)
            AuthEventType.SUCCESSFUL_UNLOCK -> context.getString(R.string.event_successful_unlock)
            AuthEventType.REPEATED_FAILURES -> context.getString(R.string.event_repeated_failures)
        }

        val builder = NotificationCompat.Builder(context, EVENT_CHANNEL)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.notif_title_format, time))
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val showPhoto = prefs.getBoolean(SecurePrefs.KEY_SHOW_PHOTO_IN_NOTIFICATION, false)
        if (showPhoto && event.encryptedPhotoPath != null) {
            val bytes = EncryptedPhotoStore(context).read(event.encryptedPhotoPath)
            if (bytes != null) {
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bmp))
                }
            }
        }

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(event.id.toInt(), builder.build())
    }

    fun notifyRepeatedFailures(context: Context, count: Int) {
        ensureChannels(context)
        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_shield_alert)
            .setContentTitle(context.getString(R.string.repeated_failures_title))
            .setContentText(context.getString(R.string.repeated_failures_body, count))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        context.getSystemService(NotificationManager::class.java)
            .notify(9999, builder.build())
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(EVENT_CHANNEL, "Security Events", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL, "Security Alerts", NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
