package com.speedsound

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class SpeedVolumeService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var audioManager: AudioManager
    private lateinit var settings: SettingsRepository
    private val volumeMapper = VolumeMapper()
    // Tracks current volume fraction for smooth ramping (-1 = not yet initialised)
    private var currentVolumeFraction = -1f

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                // Use speed=0 when GPS has no speed (e.g. fresh fix) → applies min volume
                val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
                applyVolume(speedKmh)
                broadcastStatus(speedKmh)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(getString(R.string.notif_starting))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        volumeMapper.reset()
        currentVolumeFraction = -1f
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Location ──────────────────────────────────────────────────────────────

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS / 2)
            .build()
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // Permission was revoked while service was running → stop gracefully
            stopSelf()
        }
    }

    // ── Volume ────────────────────────────────────────────────────────────────

    private fun applyVolume(speedKmh: Float) {
        val maxIndex = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetFraction = volumeMapper.computeVolume(speedKmh, settings)

        // Initialise from actual device volume on the first call so there's no jump
        if (currentVolumeFraction < 0f) {
            currentVolumeFraction =
                if (maxIndex > 0) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxIndex
                else targetFraction
        }

        // Smooth ramp: rise instantly when accelerating, fade smoothly when slowing down
        currentVolumeFraction = when {
            targetFraction > currentVolumeFraction + MAX_VOLUME_STEP_UP   -> currentVolumeFraction + MAX_VOLUME_STEP_UP
            targetFraction < currentVolumeFraction - MAX_VOLUME_STEP_DOWN -> currentVolumeFraction - MAX_VOLUME_STEP_DOWN
            else -> targetFraction
        }

        val targetIndex = (currentVolumeFraction * maxIndex).toInt().coerceIn(0, maxIndex)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0 /* no UI flash */)
    }

    // ── Broadcast to UI ───────────────────────────────────────────────────────

    private fun broadcastStatus(speedKmh: Float) {
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volPercent = if (maxVol > 0) currentVol * 100 / maxVol else 0

        sendBroadcast(Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_SPEED_KMH, speedKmh)
            putExtra(EXTRA_VOLUME_PERCENT, volPercent)
        })
        updateNotification(speedKmh, volPercent)
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SpeedSound",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = getString(R.string.notif_channel_desc) }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(speedKmh: Float, volPercent: Int) {
        val text = getString(R.string.notif_status, speedKmh, volPercent)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        const val ACTION_STATUS_UPDATE = "com.speedsound.STATUS_UPDATE"
        const val EXTRA_SPEED_KMH = "speed_kmh"
        const val EXTRA_VOLUME_PERCENT = "volume_percent"

        private const val CHANNEL_ID = "speedsound_channel"
        private const val NOTIFICATION_ID = 1
        private const val LOCATION_INTERVAL_MS = 500L
        // Asymmetric ramp: volume rises quickly when accelerating, fades smoothly when slowing down
        private const val MAX_VOLUME_STEP_UP   = 1.00f  // no cap → instant increase
        private const val MAX_VOLUME_STEP_DOWN = 0.04f  // ~4% per 500ms → ~12s to fade full range
    }
}
