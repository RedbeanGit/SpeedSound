package com.speedsound

/**
 * Maps a GPS speed (km/h) to a target volume fraction [0.0, 1.0].
 *
 * Formula:
 *   vol = minVol + clamp(speed / maxSpeed, 0, 1) × (maxVol - minVol) × sensitivity
 *
 * A moving average of the last 3 speed readings smooths out GPS jitter
 * to avoid jarring volume jumps.
 */
class VolumeMapper {

    private val speedHistory = ArrayDeque<Float>(HISTORY_SIZE)

    fun computeVolume(speedKmh: Float, settings: SettingsRepository): Float {
        // Rolling average to smooth GPS noise
        if (speedHistory.size >= HISTORY_SIZE) speedHistory.removeFirst()
        speedHistory.addLast(speedKmh)
        val smoothedSpeed = speedHistory.average().toFloat()

        val maxSpeed = settings.maxSpeedKmh
        // Guard against a zero maxSpeed (shouldn't happen with slider min=10, but be safe)
        val t = if (maxSpeed > 0f) (smoothedSpeed / maxSpeed).coerceIn(0f, 1f) else 0f

        val minVol = settings.minVolumePercent / 100f
        val maxVol = settings.maxVolumePercent / 100f
        // Ensure min < max even if settings are somehow inconsistent
        val lo = minOf(minVol, maxVol)
        val hi = maxOf(minVol, maxVol)

        val rawVol = lo + t * (hi - lo) * settings.sensitivity
        return rawVol.coerceIn(lo, hi)
    }

    fun reset() = speedHistory.clear()

    companion object {
        private const val HISTORY_SIZE = 2
    }
}
