package com.espfancontrol.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.espfancontrol.MainActivity
import com.espfancontrol.R

class FanNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "esp_fan_control_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_TOGGLE_POWER = "com.espfancontrol.ACTION_TOGGLE_POWER"
        const val ACTION_SET_SILENT = "com.espfancontrol.ACTION_SET_SILENT"
        const val ACTION_SET_BALANCED = "com.espfancontrol.ACTION_SET_BALANCED"
        const val ACTION_SET_BOOST = "com.espfancontrol.ACTION_SET_BOOST"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rpm = intent?.getIntExtra("extra_rpm", 0) ?: 0
        val mode = intent?.getStringExtra("extra_mode") ?: "Balanced"
        val isPowered = intent?.getBooleanExtra("extra_powered", true) ?: true

        val notification = buildNotification(rpm, mode, isPowered)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun buildNotification(rpm: Int, mode: String, isPowered: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intents
        val powerIntent = PendingIntent.getService(
            this, 1, Intent(this, FanNotificationService::class.java).apply { action = ACTION_TOGGLE_POWER },
            PendingIntent.FLAG_IMMUTABLE
        )
        val silentIntent = PendingIntent.getService(
            this, 2, Intent(this, FanNotificationService::class.java).apply { action = ACTION_SET_SILENT },
            PendingIntent.FLAG_IMMUTABLE
        )
        val balancedIntent = PendingIntent.getService(
            this, 3, Intent(this, FanNotificationService::class.java).apply { action = ACTION_SET_BALANCED },
            PendingIntent.FLAG_IMMUTABLE
        )
        val boostIntent = PendingIntent.getService(
            this, 4, Intent(this, FanNotificationService::class.java).apply { action = ACTION_SET_BOOST },
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isPowered) "$rpm RPM • $mode" else "Fan Off"
        val statusText = if (isPowered) "Active Cooling" else "Standby"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fan)
            .setContentTitle(title)
            .setContentText(statusText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, if (isPowered) "Off" else "On", powerIntent)
            .addAction(0, "Silent", silentIntent)
            .addAction(0, "Balanced", balancedIntent)
            .addAction(0, "Boost", boostIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fan Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live fan telemetry and quick control toggles"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

