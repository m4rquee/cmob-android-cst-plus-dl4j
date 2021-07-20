package com.cmob.pattern_rec_mobile

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.common.collect.EvictingQueue
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainActivity : AppCompatActivity(), SensorEventListener {

    private val MODEL_WINDOW: Int = 90
    private lateinit var mobileNetModel: MobileNetModel
    private val modelInputTensor =
        TensorBuffer.createFixedSize(intArrayOf(1, 1, MODEL_WINDOW, 3), DataType.FLOAT32)

    // Calculated from the dataset:
    private val mus = arrayOf(0.6628660249577367f, 7.2556261603547405f, 0.41107842498640323f)
    private val sigmas = arrayOf(6.849043077510763f, 6.746213296062538f, 4.754117645813611f)

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private val accBuffer: EvictingQueue<FloatArray> = EvictingQueue.create(MODEL_WINDOW)
    private val STOP_FREQ_CHECK = 10
    private var predictionsBuffer: EvictingQueue<Boolean> = EvictingQueue.create(STOP_FREQ_CHECK)

    private val STOP_LOWER_THRESHOLD = 0.1
    private val STOP_UPPER_THRESHOLD = 0.5

    private lateinit var downstairsTextView: TextView
    private lateinit var joggingTextView: TextView
    private lateinit var sittingTextView: TextView
    private lateinit var standingTextView: TextView
    private lateinit var upstairsTextView: TextView
    private lateinit var walkingTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var textViews: Array<TextView>

    private lateinit var vibrator: Vibrator

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
        titleTextView = findViewById(R.id.titleTextView)
        textViews = arrayOf(
            downstairsTextView,
            joggingTextView,
            sittingTextView,
            standingTextView,
            upstairsTextView,
            walkingTextView
        )

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
                this, it, 50000, 50000
            )
        }
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
        val ret = mobileNetModel.analyze(modelInputTensor)

        // Shows the values:
        var maxIndex = 0
        var maxVal: Float = Float.MIN_VALUE
        var maxLabel = ""
        mobileNetModel.labelsList.mapIndexed { i, label ->
            val prob = ret.mapWithFloatValue[label]
            textViews[i].text = prob.toString()
            textViews[i].setTextColor(Color.GRAY)
            if (prob!! > maxVal) {
                maxIndex = i
                maxVal = prob
                maxLabel = label
            }
        }
        textViews[maxIndex].setTextColor(Color.GREEN)

        // Vibrates to indicate GPS on/off:
        predictionsBuffer.add(maxLabel == "Sitting" || maxLabel == "Standing") // if the user is idle
        titleTextView.setTextColor(Color.GRAY) // default color
        if (predictionsBuffer.count() == STOP_FREQ_CHECK) {
            val stopFraction = predictionsBuffer.count { it }.toDouble() / STOP_FREQ_CHECK
            if (stopFraction <= STOP_LOWER_THRESHOLD) {
                titleTextView.setTextColor(Color.GREEN)
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (stopFraction >= STOP_UPPER_THRESHOLD) {
                titleTextView.setTextColor(Color.RED)
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            }
            predictionsBuffer.clear() // reduces the update frequency
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER)
            accBuffer.add(event.values.clone())
        if (accBuffer.count() == MODEL_WINDOW) {
            runModel()
            accBuffer.clear() // reduces the update frequency
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }
}