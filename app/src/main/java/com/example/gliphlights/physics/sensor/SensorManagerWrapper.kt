package com.example.gliphlights.physics.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Android SensorManager. No physics logic.
 */
@Singleton
class SensorManagerWrapper @Inject constructor(
    @ApplicationContext context: Context
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val filter = SensorFilter()

    private val _samples = MutableSharedFlow<com.example.gliphlights.physics.model.SensorSample>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val samples: SharedFlow<com.example.gliphlights.physics.model.SensorSample> = _samples.asSharedFlow()

    private var listening = false

    fun start(samplingUs: Int = SensorManager.SENSOR_DELAY_GAME) {
        if (listening) return
        filter.reset()
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        gravity?.let {
            sensorManager.registerListener(this, it, samplingUs)
        }
        accel?.let {
            sensorManager.registerListener(this, it, samplingUs)
        }
        rotation?.let {
            sensorManager.registerListener(this, it, samplingUs)
        }
        listening = true
    }

    fun stop() {
        if (!listening) return
        sensorManager.unregisterListener(this)
        listening = false
    }

    fun setSmoothing(amount: Float) = filter.setSmoothing(amount)

    fun latest(): com.example.gliphlights.physics.model.SensorSample = filter.sample()

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> filter.pushGravity(event.values[0], event.values[1], event.values[2])
            Sensor.TYPE_ACCELEROMETER -> {
                // If no dedicated gravity sensor, approximate from accel (already filtered)
                if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) == null) {
                    filter.pushGravity(event.values[0], event.values[1], event.values[2])
                }
                filter.pushAccelerometer(event.values[0], event.values[1], event.values[2])
            }
            Sensor.TYPE_ROTATION_VECTOR -> filter.pushRotationVector(event.values)
        }
        _samples.tryEmit(filter.sample(event.timestamp))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
