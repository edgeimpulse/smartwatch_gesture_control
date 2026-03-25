package com.example.wizbulb.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorDataCollector(context: Context, private val maxSamples: Int) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _acc  = ArrayDeque<Triple<Float, Float, Float>>()
    private val _gyro = ArrayDeque<Triple<Float, Float, Float>>()
    private val lock  = Any()

    fun start() {
        synchronized(lock) { _acc.clear(); _gyro.clear() }
        sensorManager.registerListener(this, accSensor,  20000) // 50 Hz
        sensorManager.registerListener(this, gyroSensor, 20000)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /** Thread-safe snapshot of the latest paired samples as a flat FloatArray ready for inference. */
    fun buildInputArray(featureCount: Int): FloatArray {
        synchronized(lock) {
            val count = minOf(_acc.size, _gyro.size, maxSamples)
            val input = FloatArray(featureCount) // zero-padded by default
            for (i in 0 until count) {
                input[i * 6 + 0] = _acc[i].first
                input[i * 6 + 1] = _acc[i].second
                input[i * 6 + 2] = _acc[i].third
                input[i * 6 + 3] = _gyro[i].first
                input[i * 6 + 4] = _gyro[i].second
                input[i * 6 + 5] = _gyro[i].third
            }
            return input
        }
    }

    val sampleCount: Int get() = synchronized(lock) { minOf(_acc.size, _gyro.size) }

    /**
     * Variance of accelerometer magnitude over the last [nSamples] samples.
     * Near zero at rest (~0.01–0.05), spikes during gestures (1.0+).
     * Used for motion onset detection.
     */
    fun recentAccVariance(nSamples: Int): Float {
        synchronized(lock) {
            if (_acc.size < 2) return 0f
            val recent = _acc.takeLast(minOf(nSamples, _acc.size))
            val mags = recent.map { (x, y, z) ->
                kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            }
            val mean = mags.average().toFloat()
            return mags.map { (it - mean) * (it - mean) }.average().toFloat()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        synchronized(lock) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    _acc.addLast(Triple(event.values[0], event.values[1], event.values[2]))
                    if (_acc.size > maxSamples) _acc.removeFirst()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    _gyro.addLast(Triple(event.values[0], event.values[1], event.values[2]))
                    if (_gyro.size > maxSamples) _gyro.removeFirst()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
