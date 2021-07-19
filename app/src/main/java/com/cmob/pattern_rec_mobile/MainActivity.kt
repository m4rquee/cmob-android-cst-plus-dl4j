package com.cmob.pattern_rec_mobile

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.common.collect.EvictingQueue
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var MODEL_WINDOW: Int = 90
    private lateinit var mobileNetModel: MobileNetModel
    private val modelInputTensor =
        TensorBuffer.createFixedSize(intArrayOf(1, 1, MODEL_WINDOW, 3), DataType.FLOAT32)

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private var accBuffer: EvictingQueue<FloatArray> = EvictingQueue.create(90)

    private lateinit var downstairsTextView: TextView
    private lateinit var joggingTextView: TextView
    private lateinit var sittingTextView: TextView
    private lateinit var standingTextView: TextView
    private lateinit var upstairsTextView: TextView
    private lateinit var walkingTextView: TextView
    private lateinit var textViews: Array<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Creates the NN model:
        mobileNetModel = MobileNetModel(this)

        // Find all the text views:
        downstairsTextView = findViewById(R.id.downstairsTextView)
        joggingTextView = findViewById(R.id.joggingTextView)
        sittingTextView = findViewById(R.id.sittingTextView)
        standingTextView = findViewById(R.id.standingTextView)
        upstairsTextView = findViewById(R.id.upstairsTextView)
        walkingTextView = findViewById(R.id.walkingTextView)
        textViews = arrayOf(
            downstairsTextView,
            joggingTextView,
            sittingTextView,
            standingTextView,
            upstairsTextView,
            walkingTextView
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mobileNetModel.close()
        sensorManager.unregisterListener(this)
    }

    fun onClick(view: View) {
        val toast = Toast.makeText(applicationContext, "Starting Inference", Toast.LENGTH_SHORT)
        toast.show()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun runModel() {
        val accArray = FloatArray(3 * MODEL_WINDOW)
        accBuffer.forEachIndexed { i, sample ->
            accArray[3 * i] = sample[0]
            accArray[3 * i + 1] = sample[1]
            accArray[3 * i + 2] = sample[2]
        } // converts the queue into a flattened array of floats
        modelInputTensor.loadArray(accArray)
        val ret = mobileNetModel.analyze(modelInputTensor)
        mobileNetModel.labelsList.mapIndexed { i, label ->
            textViews[i].text = ret.mapWithFloatValue[label].toString()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER)
            accBuffer.add(event.values)
        if (accBuffer.count() == MODEL_WINDOW) runModel()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }
}