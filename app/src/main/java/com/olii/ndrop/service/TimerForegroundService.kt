package com.olii.ndrop.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.olii.ndrop.MainActivity
import com.olii.ndrop.util.toTimerDisplay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * NDrop — TimerForegroundService
 *
 * Keeps the countdown alive when the user leaves the app.
 * Displays a persistent "Timer running" notification with
 * live countdown updates every second.
 * Fires a separate "Timer finished" notification on expiry.
 *
 * Start:  startForegroundService(TimerForegroundService.buildStartIntent(...))
 * Cancel: startService(TimerForegroundService.buildCancelIntent(...))
 *
 * Signature: Olii-8882
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    companion object {
        const val ACTION_START  = "ndrop.timer.START"
        const val ACTION_CANCEL = "ndrop.timer.CANCEL"
        const val EXTRA_SECONDS = "seconds"
        const val EXTRA_LABEL   = "label"
        const val NOTIF_ID_RUNNING = 2001

        fun buildStartIntent(context: Context, totalSeconds: Long, label: String): Intent =
            Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SECONDS, totalSeconds)
                putExtra(EXTRA_LABEL, label)
            }

        fun buildCancelIntent(context: Context): Intent =
            Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_CANCEL
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val seconds = intent.getLongExtra(EXTRA_SECONDS, 0L)
                val label   = intent.getStringExtra(EXTRA_LABEL) ?: "Timer"
                startTimer(seconds, label)
            }
            ACTION_CANCEL -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(totalSeconds: Long, label: String) {
        timerJob?.cancel()

        // Show persistent running notification immediately
        startForeground(NOTIF_ID_RUNNING, buildRunningNotification(label, totalSeconds))

        timerJob = serviceScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                // Update notification every 5s to reduce overhead
                if (remaining % 5 == 0L || remaining <= 10) {
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIF_ID_RUNNING, buildRunningNotification(label, remaining))
                }
            }
            // Timer done — cancel running notif, fire finished notif
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ID_RUNNING)
            notificationHelper.showTimerExpiredNotification(label)
            stopSelf()
        }
    }

    private fun buildRunningNotification(label: String, remainingSeconds: Long): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = PendingIntent.getService(
            this, 1,
            buildCancelIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏱ $label")
            .setContentText(remainingSeconds.toTimerDisplay())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(tapIntent)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
