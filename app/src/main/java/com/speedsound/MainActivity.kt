package com.speedsound

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.speedsound.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsRepository

    // Guard flag to prevent re-entrant slider updates when loading settings programmatically
    private var updatingSliders = false

    // ── BroadcastReceiver (speed/volume updates from service) ─────────────────

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val speed = intent.getFloatExtra(SpeedVolumeService.EXTRA_SPEED_KMH, 0f)
            val vol = intent.getIntExtra(SpeedVolumeService.EXTRA_VOLUME_PERCENT, 0)
            binding.tvSpeed.text = "%.1f".format(speed)
            binding.tvVolume.text = vol.toString()
        }
    }

    // ── Permission launcher ───────────────────────────────────────────────────

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            startSpeedService()
        } else {
            // Location denied: revert toggle (notification denial is non-blocking)
            settings.isEnabled = false
            updatingSliders = true
            binding.switchService.isChecked = false
            updatingSliders = false
            updateServiceUI(false)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: draw behind status bar and navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dark icons on light background + apply insets as padding
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        settings = SettingsRepository(this)
        initDefaultsOnFirstRun()
        loadSettingsIntoUI()
        updateServiceUI(settings.isEnabled)
        setupListeners()
    }

    // ── First-run defaults ────────────────────────────────────────────────────

    private fun initDefaultsOnFirstRun() {
        if (settings.hasBeenInitialized) return
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxIdx = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curIdx = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val curPct = if (maxIdx > 0) curIdx * 100 / maxIdx else 60
        // Min volume = current volume - 20%, rounded down to nearest 5%, min 5%
        settings.minVolumePercent = maxOf(5, ((curPct - 20) / 5) * 5)
        settings.hasBeenInitialized = true
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            this,
            statusReceiver,
            IntentFilter(SpeedVolumeService.ACTION_STATUS_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statusReceiver)
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private fun loadSettingsIntoUI() {
        updatingSliders = true

        binding.switchService.isChecked = settings.isEnabled

        binding.sliderSensitivity.value = settings.sensitivity
        binding.tvSensitivityValue.text = "%.1f×".format(settings.sensitivity)

        binding.sliderMaxSpeed.value = settings.maxSpeedKmh
        binding.tvMaxSpeedValue.text = "${settings.maxSpeedKmh.toInt()} km/h"

        binding.sliderMinVolume.value = settings.minVolumePercent.toFloat()
        binding.tvMinVolumeValue.text = "${settings.minVolumePercent}%"

        binding.sliderMaxVolume.value = settings.maxVolumePercent.toFloat()
        binding.tvMaxVolumeValue.text = "${settings.maxVolumePercent}%"

        updatingSliders = false
    }

    private fun setupListeners() {
        // ── Main toggle ───────────────────────────────────────────────────────
        binding.switchService.setOnCheckedChangeListener { _, checked ->
            if (updatingSliders) return@setOnCheckedChangeListener
            settings.isEnabled = checked
            if (checked) requestPermissionOrStart() else stopSpeedService()
            updateServiceUI(checked)
        }

        // ── Sensitivity slider ────────────────────────────────────────────────
        binding.sliderSensitivity.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || updatingSliders) return@addOnChangeListener
            settings.sensitivity = value
            binding.tvSensitivityValue.text = "%.1f×".format(value)
        }

        // ── Max speed slider ──────────────────────────────────────────────────
        binding.sliderMaxSpeed.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || updatingSliders) return@addOnChangeListener
            settings.maxSpeedKmh = value
            binding.tvMaxSpeedValue.text = "${value.toInt()} km/h"
        }

        // ── Min volume slider (must stay below max volume) ────────────────────
        binding.sliderMinVolume.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || updatingSliders) return@addOnChangeListener
            val intVal = value.toInt()
            if (intVal >= settings.maxVolumePercent) {
                // Clamp: push back to 5% below current max
                val adjusted = (settings.maxVolumePercent - 5).coerceAtLeast(0)
                updatingSliders = true
                binding.sliderMinVolume.value = adjusted.toFloat()
                updatingSliders = false
                settings.minVolumePercent = adjusted
                binding.tvMinVolumeValue.text = "$adjusted%"
            } else {
                settings.minVolumePercent = intVal
                binding.tvMinVolumeValue.text = "$intVal%"
            }
        }

        // ── Max volume slider (must stay above min volume) ────────────────────
        binding.sliderMaxVolume.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || updatingSliders) return@addOnChangeListener
            val intVal = value.toInt()
            if (intVal <= settings.minVolumePercent) {
                // Clamp: push back to 5% above current min
                val adjusted = (settings.minVolumePercent + 5).coerceAtMost(100)
                updatingSliders = true
                binding.sliderMaxVolume.value = adjusted.toFloat()
                updatingSliders = false
                settings.maxVolumePercent = adjusted
                binding.tvMaxVolumeValue.text = "$adjusted%"
            } else {
                settings.maxVolumePercent = intVal
                binding.tvMaxVolumeValue.text = "$intVal%"
            }
        }
    }

    private fun updateServiceUI(active: Boolean) {
        binding.tvServiceStatus.text =
            getString(if (active) R.string.status_active else R.string.status_inactive)
        binding.cardStats.visibility = if (active) View.VISIBLE else View.GONE
        if (!active) {
            binding.tvSpeed.text = "0"
            binding.tvVolume.text = "—"
        }
    }

    // ── Service control ───────────────────────────────────────────────────────

    private fun requestPermissionOrStart() {
        val fine   = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val locationGranted =
            ContextCompat.checkSelfPermission(this, fine)   == PackageManager.PERMISSION_GRANTED
         || ContextCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED

        val permissionsToRequest = buildList {
            if (!locationGranted) { add(fine); add(coarse) }
            // POST_NOTIFICATIONS is a runtime permission from Android 13 (API 33)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notifPerm = Manifest.permission.POST_NOTIFICATIONS
                if (ContextCompat.checkSelfPermission(this@MainActivity, notifPerm)
                        != PackageManager.PERMISSION_GRANTED) {
                    add(notifPerm)
                }
            }
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) startSpeedService()
        else permissionLauncher.launch(permissionsToRequest)
    }

    private fun startSpeedService() {
        val intent = Intent(this, SpeedVolumeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopSpeedService() {
        stopService(Intent(this, SpeedVolumeService::class.java))
    }
}
