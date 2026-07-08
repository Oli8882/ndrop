package com.olii.ndrop.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.olii.ndrop.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NDrop — NotificationHelper
 *
 * Manages notification channels and builds notifications for:
 *  - Parking proximity alert ("You're near your car")
 *  - Timer expiry alert
 *
 * Signature: Olii-8882
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_PARKING   = "ndrop_parking"
        const val CHANNEL_TIMER     = "ndrop_timer"
        const val NOTIF_ID_PARKING  = 1001
        const val NOTIF_ID_TIMER    = 1002
    }

    fun createChannels() {
        val parkingChannel = NotificationChannel(
            CHANNEL_PARKING,
            "Parking Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when you're near your parked car"
            enableVibration(true)
        }

        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            "Timer Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when a timer tag expires"
            enableVibration(true)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(parkingChannel)
        manager.createNotificationChannel(timerChannel)
    }

    fun showParkingProximityNotification(distanceMeters: Float) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distanceText = if (distanceMeters < 1000f) {
            "${distanceMeters.toInt()}m away"
        } else {
            "${"%.1f".format(distanceMeters / 1000f)}km away"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_PARKING)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("🅿️ Your car is nearby")
            .setContentText("Parked car is $distanceText — tap to navigate")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_PARKING, notification)
    }

    fun showTimerExpiredNotification(label: String) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏱ Timer finished")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_TIMER, notification)
    }

    fun cancelParkingNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID_PARKING)
    }
}
