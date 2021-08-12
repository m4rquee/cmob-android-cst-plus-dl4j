package com.cmob.pattern_rec_mobile.agent

import android.app.Service
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ACCSensorCodelet<T>(name: String, private val context: T) :
    SensorCodelet() where T : Service, T : SensorEventListener {

    // Sensor objects:
    private var sensorManager: SensorManager
    private var sensor: Sensor? = null

    // Accelerometer buffer:
    private val accBuffer: MutableList<FloatArray> = ArrayList()

    init {
        setName(name)
        // Init the accelerometer sensor (20Hz):
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                context, it, 50000, 50000
            )
        }
    }

    override fun getData(): String {
        return accBuffer.toString()
    }

    override fun accessMemoryObjects() {
    }

    override fun calculateActivation() {
    }

    override fun stop() {
        super.stop()
        sensorManager.unregisterListener(context)
    }
}