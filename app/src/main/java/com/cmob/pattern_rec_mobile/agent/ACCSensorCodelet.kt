package com.cmob.pattern_rec_mobile.agent

import android.app.Service
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi

class ACCSensorCodelet<T : Service>(name: String, context: T) : SensorCodelet(),
    SensorEventListener {

    // Sensor objects:
    private var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private val accBuffer: MutableList<FloatArray> = ArrayList()

    init {
        setName(name)
        // Init the accelerometer sensor (20Hz):
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this, it, 50000, 50000
            )
        }
    }

    override fun getData(): MutableList<FloatArray> {
        return accBuffer
    }

    override fun accessMemoryObjects() {
    }

    override fun calculateActivation() {
    }

    override fun stop() {
        super.stop()
        sensorManager.unregisterListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER)
            synchronized(accBuffer) {
                accBuffer.add(event.values.clone()) // buffers the acceleration vector (x, y, z)
            }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }
}