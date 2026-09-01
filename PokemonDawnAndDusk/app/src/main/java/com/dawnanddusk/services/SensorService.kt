package com.dawnanddusk.services

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

data class SensorOrientation(
    val pitch: Float = 0f, // Up/down tilt (-90 to +90)
    val roll: Float = 0f,  // Left/right tilt (-90 to +90)
    val yaw: Float = 0f,   // Compass direction
    val hasSensor: Boolean = true
)

class SensorService(context: Context) {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    private val rotationVectorSensor: Sensor? by lazy {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    private val gyroscopeSensor: Sensor? by lazy {
        sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private val accelerometerSensor: Sensor? by lazy {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private val _orientation = MutableStateFlow(SensorOrientation(hasSensor = true))
    val orientation: StateFlow<SensorOrientation> = _orientation.asStateFlow()

    private var isListening = false
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Low-pass filter smoothing constants
    private val alpha = 0.20f
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private var smoothedYaw = 0f

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return

            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    // Convert radians to degrees
                    val rawYaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val rawPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val rawRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                    // Apply low-pass exponential smoothing
                    smoothedPitch = (1 - alpha) * smoothedPitch + alpha * rawPitch
                    smoothedRoll = (1 - alpha) * smoothedRoll + alpha * rawRoll
                    smoothedYaw = (1 - alpha) * smoothedYaw + alpha * rawYaw

                    // Clamp values to valid field of view range
                    val clampedPitch = max(-80f, min(80f, smoothedPitch))
                    val clampedRoll = max(-80f, min(80f, smoothedRoll))

                    _orientation.value = SensorOrientation(
                        pitch = clampedPitch,
                        roll = clampedRoll,
                        yaw = smoothedYaw,
                        hasSensor = true
                    )
                }
                Sensor.TYPE_GYROSCOPE -> {
                    val dt = 0.02f
                    smoothedRoll += (event.values[1] * 57.2958f * dt)
                    smoothedPitch += (event.values[0] * 57.2958f * dt)

                    val clampedPitch = max(-80f, min(80f, smoothedPitch))
                    val clampedRoll = max(-80f, min(80f, smoothedRoll))

                    _orientation.value = SensorOrientation(
                        pitch = clampedPitch,
                        roll = clampedRoll,
                        yaw = smoothedYaw,
                        hasSensor = true
                    )
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun startListening() {
        if (isListening || sensorManager == null) return

        val hasRotation = rotationVectorSensor?.let {
            sensorManager?.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: false

        if (!hasRotation) {
            val hasGyro = gyroscopeSensor?.let {
                sensorManager?.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
            } ?: false

            if (!hasGyro) {
                _orientation.value = SensorOrientation(hasSensor = false)
            }
        }

        isListening = true
    }

    fun stopListening() {
        if (!isListening) return
        try {
            sensorManager?.unregisterListener(sensorListener)
        } catch (_: Exception) {}
        isListening = false
    }

    /**
     * Manual offset adjustment for fallback/touch panning mode.
     */
    fun applyManualPan(deltaPitch: Float, deltaRoll: Float) {
        smoothedPitch = max(-80f, min(80f, smoothedPitch + deltaPitch))
        smoothedRoll = max(-80f, min(80f, smoothedRoll + deltaRoll))
        _orientation.value = SensorOrientation(
            pitch = smoothedPitch,
            roll = smoothedRoll,
            yaw = smoothedYaw,
            hasSensor = false
        )
    }

    fun reset() {
        smoothedPitch = 0f
        smoothedRoll = 0f
        smoothedYaw = 0f
        _orientation.value = SensorOrientation(hasSensor = rotationVectorSensor != null || gyroscopeSensor != null)
    }
}
