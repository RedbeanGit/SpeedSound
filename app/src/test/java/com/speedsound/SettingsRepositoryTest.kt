package com.speedsound

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [SettingsRepository].
 *
 * Robolectric provides a lightweight Android Context so SharedPreferences works
 * without a real device or emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsRepositoryTest {

    private lateinit var settings: SettingsRepository

    @Before
    fun setup() {
        settings = SettingsRepository(ApplicationProvider.getApplicationContext())
    }

    // ── Default values ─────────────────────────────────────────────────────

    @Test
    fun `default sensitivity is 1_0`() {
        assertEquals(1.0f, settings.sensitivity, 0.001f)
    }

    @Test
    fun `default maxSpeedKmh is 50`() {
        assertEquals(50f, settings.maxSpeedKmh, 0.001f)
    }

    @Test
    fun `default minVolumePercent is 20`() {
        assertEquals(20, settings.minVolumePercent)
    }

    @Test
    fun `default maxVolumePercent is 100`() {
        assertEquals(100, settings.maxVolumePercent)
    }

    @Test
    fun `default isEnabled is false`() {
        assertFalse(settings.isEnabled)
    }

    @Test
    fun `default hasBeenInitialized is false`() {
        assertFalse(settings.hasBeenInitialized)
    }

    // ── Read-write round-trips ─────────────────────────────────────────────

    @Test
    fun `sensitivity is persisted`() {
        settings.sensitivity = 2.5f
        assertEquals(2.5f, settings.sensitivity, 0.001f)
    }

    @Test
    fun `maxSpeedKmh is persisted`() {
        settings.maxSpeedKmh = 80f
        assertEquals(80f, settings.maxSpeedKmh, 0.001f)
    }

    @Test
    fun `minVolumePercent is persisted`() {
        settings.minVolumePercent = 35
        assertEquals(35, settings.minVolumePercent)
    }

    @Test
    fun `maxVolumePercent is persisted`() {
        settings.maxVolumePercent = 90
        assertEquals(90, settings.maxVolumePercent)
    }

    @Test
    fun `isEnabled toggle is persisted`() {
        settings.isEnabled = true
        assertTrue(settings.isEnabled)
        settings.isEnabled = false
        assertFalse(settings.isEnabled)
    }

    @Test
    fun `hasBeenInitialized flag is persisted`() {
        settings.hasBeenInitialized = true
        assertTrue(settings.hasBeenInitialized)
    }

    // ── Overwrite existing value ───────────────────────────────────────────

    @Test
    fun `writing sensitivity twice keeps last value`() {
        settings.sensitivity = 1.5f
        settings.sensitivity = 0.8f
        assertEquals(0.8f, settings.sensitivity, 0.001f)
    }
}
