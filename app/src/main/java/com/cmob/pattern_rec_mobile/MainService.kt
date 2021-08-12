package com.cmob.pattern_rec_mobile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import androidx.annotation.RequiresApi
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainService : Service(), SensorEventListener {

    private var NUM_CLASSES: Int = 0
    private val MODEL_WINDOW: Int = 90
    private lateinit var mobileNetModel: MobileNetModel
    private val modelInputTensor =
        TensorBuffer.createFixedSize(intArrayOf(1, 1, MODEL_WINDOW, 3), DataType.FLOAT32)

    // Calculated from the dataset and used for normalization:
    private val mus = arrayOf(0.6628660249577367f, 7.2556261603547405f, 0.41107842498640323f)
    private val sigmas = arrayOf(6.849043077510763f, 6.746213296062538f, 4.754117645813611f)

    // Sensor objects:
    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    // Accelerometer and predictions buffers:
    private val accBuffer: MutableList<FloatArray> = ArrayList()
    private val STOP_FREQ_CHECK = 3
    private var predictionsBuffer: MutableList<Boolean> = ArrayList()

    private val STOP_LOWER_THRESHOLD = 0.1
    private val STOP_UPPER_THRESHOLD = 0.5

    private lateinit var vibrator: Vibrator

    // Communication variables:
    lateinit var texts: Array<String>
    lateinit var colors: Array<Int>
    var titleColor = Color.GRAY
    var newBatch = false // if the UI should be updated

    companion object {
        var started = false
    }

    private val binder = MainBinder() // binder given to clients

    inner class MainBinder : Binder() {
        // Return this instance of MainService so clients can call public methods:
        fun getService(): MainService = this@MainService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mobileNetModel = MobileNetModel(this) // creates the NN model
        NUM_CLASSES = mobileNetModel.labelsList.size

        // Init the communication arrays:
        texts = Array(NUM_CLASSES) { "" }
        colors = Array(NUM_CLASSES) { Color.GRAY }

        // Init the accelerometer sensor (20Hz):
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this, it, 50000, 50000
            )
        }

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        started = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mobileNetModel.close()
        sensorManager.unregisterListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun runModel() {
        val accArray = FloatArray(3 * MODEL_WINDOW)
        accBuffer.forEachIndexed { i, sample ->
            accArray[3 * i] = (sample[0] - mus[0]) / sigmas[0]
            accArray[3 * i + 1] = (sample[1] - mus[1]) / sigmas[1]
            accArray[3 * i + 2] = (sample[2] - mus[2]) / sigmas[2]
        } // converts the queue into a flattened array of floats normalizing the data
        modelInputTensor.loadArray(accArray)
        val ret = mobileNetModel.analyze(modelInputTensor) // runs the model with the buffered data

        // Choose the texts and colors:
        var maxIndex = 0 // index of the predicted class
        var maxVal: Float = Float.MIN_VALUE // prob of the predicted class
        var maxLabel = "" // label of the predicted class
        mobileNetModel.labelsList.mapIndexed { i, label ->
            val prob = ret.mapWithFloatValue[label]
            texts[i] = prob.toString()
            colors[i] = Color.GRAY
            if (prob!! > maxVal) {
                maxIndex = i
                maxVal = prob
                maxLabel = label
            }
        }
        colors[maxIndex] = Color.GREEN // highlights the predicted class

        // Vibrates to indicate GPS on/off:
        predictionsBuffer.add(maxLabel == "Sitting" || maxLabel == "Standing") // if the user is idle
        titleColor = Color.GRAY // default color
        if (predictionsBuffer.count() == STOP_FREQ_CHECK) { // if the buffer filled up
            val idleFraction = predictionsBuffer.count { it }.toDouble() / STOP_FREQ_CHECK
            if (idleFraction <= STOP_LOWER_THRESHOLD) { // should enable the GPS when moving
                titleColor = Color.GREEN
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else if (idleFraction >= STOP_UPPER_THRESHOLD) { // should disable the GPS when idle
                titleColor = Color.RED
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            }
            predictionsBuffer.clear() // reduces the update frequency and keeps the list bounded
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER)
            accBuffer.add(event.values.clone()) // buffers the acceleration vector (x, y, z)
        if (accBuffer.count() == MODEL_WINDOW) {
            runModel()
            newBatch = true
            accBuffer.clear() // reduces the update frequency and keeps the list bounded
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }
}