package fr.redbean.speedsound

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [VolumeMapper].
 *
 * SettingsRepository is mocked so no Android Context is needed.
 */
class VolumeMapperTest {

    private lateinit var mapper: VolumeMapper
    private lateinit var settings: SettingsRepository

    @Before
    fun setup() {
        mapper = VolumeMapper()
        settings = mock()
        // Default realistic values matching app defaults
        whenever(settings.maxSpeedKmh).thenReturn(50f)
        whenever(settings.minVolumePercent).thenReturn(20)
        whenever(settings.maxVolumePercent).thenReturn(100)
        whenever(settings.sensitivity).thenReturn(1.0f)
    }

    // ── Boundary conditions ────────────────────────────────────────────────

    @Test
    fun `at zero speed volume equals minVolume`() {
        val vol = mapper.computeVolume(0f, settings)
        assertEquals(0.20f, vol, 0.001f)
    }

    @Test
    fun `at exactly maxSpeed volume equals maxVolume`() {
        // Fill the rolling average (HISTORY_SIZE = 2)
        mapper.computeVolume(50f, settings)
        val vol = mapper.computeVolume(50f, settings)
        assertEquals(1.00f, vol, 0.001f)
    }

    @Test
    fun `speed above maxSpeed is clamped to maxVolume`() {
        mapper.computeVolume(200f, settings)
        val vol = mapper.computeVolume(200f, settings)
        assertEquals(1.00f, vol, 0.001f)
    }

    @Test
    fun `negative speed is clamped to minVolume`() {
        val vol = mapper.computeVolume(-10f, settings)
        assertEquals(0.20f, vol, 0.001f)
    }

    // ── Mid-range linearity ────────────────────────────────────────────────

    @Test
    fun `at half maxSpeed volume is midpoint between min and max`() {
        // Two identical readings so rolling average equals the input
        mapper.computeVolume(25f, settings)
        val vol = mapper.computeVolume(25f, settings)
        // min=0.20, max=1.00, t=0.5 → 0.20 + 0.5 * 0.80 = 0.60
        assertEquals(0.60f, vol, 0.001f)
    }

    // ── Sensitivity scaling ────────────────────────────────────────────────

    @Test
    fun `sensitivity doubles the effective range`() {
        whenever(settings.sensitivity).thenReturn(2.0f)
        mapper.computeVolume(25f, settings)
        val vol = mapper.computeVolume(25f, settings)
        // t=0.5, sens=2.0 → rawVol = 0.20 + 0.5 * 0.80 * 2.0 = 1.00, clamped to 1.00
        assertEquals(1.00f, vol, 0.001f)
    }

    @Test
    fun `sensitivity below 1 compresses the range`() {
        whenever(settings.sensitivity).thenReturn(0.5f)
        mapper.computeVolume(25f, settings)
        val vol = mapper.computeVolume(25f, settings)
        // t=0.5, sens=0.5 → 0.20 + 0.5 * 0.80 * 0.5 = 0.40
        assertEquals(0.40f, vol, 0.001f)
    }

    // ── Rolling average smoothing ──────────────────────────────────────────

    @Test
    fun `rolling average smooths a sudden speed spike`() {
        mapper.computeVolume(0f, settings)      // history: [0]
        val vol = mapper.computeVolume(50f, settings) // history: [0, 50] → avg 25
        // avg=25 → t=0.5 → 0.60
        assertEquals(0.60f, vol, 0.001f)
    }

    // ── Edge cases in settings ─────────────────────────────────────────────

    @Test
    fun `min equals max volume returns minVolume regardless of speed`() {
        whenever(settings.minVolumePercent).thenReturn(60)
        whenever(settings.maxVolumePercent).thenReturn(60)
        val vol = mapper.computeVolume(30f, settings)
        assertEquals(0.60f, vol, 0.001f)
    }

    @Test
    fun `zero maxSpeed does not throw and returns minVolume`() {
        whenever(settings.maxSpeedKmh).thenReturn(0f)
        val vol = mapper.computeVolume(10f, settings)
        assertEquals(0.20f, vol, 0.001f)
    }

    // ── Reset ──────────────────────────────────────────────────────────────

    @Test
    fun `reset clears history so next reading is treated as first`() {
        // Build up history with high speed
        mapper.computeVolume(50f, settings)
        mapper.computeVolume(50f, settings)
        mapper.reset()
        // After reset, a reading of 0 should produce minVolume (no bleed from old readings)
        val vol = mapper.computeVolume(0f, settings)
        assertEquals(0.20f, vol, 0.001f)
    }
}
