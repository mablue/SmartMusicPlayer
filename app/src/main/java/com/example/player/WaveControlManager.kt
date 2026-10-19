package com.example.player

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * Wave-to-Control using Proximity Sensor (official Sensor API).
 * Detects a hand wave over the top of the phone.
 * This is a power-efficient alternative to camera-based gesture detection.
 * 
 * Based on: developer.android.com/develop/sensors-and-location/sensors/sensors_position
 */
class WaveControlManager(
    context: Context,
    private val onWaveNext: () -> Unit,
    private val onWavePrevious: () -> Unit,
    private val onWavePlayPause: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var lastWaveTime = 0L
    private var isNear = false
    private val WAVE_DEBOUNCE_MS = 800L

    fun start() {
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: Log.w("WaveControl", "No proximity sensor available")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val distance = event.values[0]
        val maxRange = event.sensor.maximumRange
        val now = System.currentTimeMillis()

        // Proximity sensors report either near (0) or far (maxRange)
        val currentlyNear = distance < maxRange

        if (currentlyNear && !isNear) {
            // Hand entered proximity zone
            if (now - lastWaveTime > WAVE_DEBOUNCE_MS) {
                lastWaveTime = now
                // A single wave cycle: we can interpret this as "Play/Pause"
                // For next/previous, we'd need a different gesture (e.g., double wave)
                onWavePlayPause()
            }
        } else if (!currentlyNear && isNear) {
            // Hand left proximity zone
            // Could be used to detect a swipe direction if combined with timing
        }
        isNear = currentlyNear
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
}
