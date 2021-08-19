package com.cmob.pattern_rec_mobile.agent

import android.app.Service
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import br.unicamp.cst.core.entities.Codelet
import org.tensorflow.lite.support.label.TensorLabel

class ResponseCodelet<T : Service>(
    name: String,
    NUM_CLASSES: Int,
    private val labelsList: ArrayList<String>,
    context: T
) : Codelet() {

    private val STOP_FREQ_CHECK = 3

    // Communication variables:
    var texts: Array<String>
    var colors: Array<Int>
    var titleColor = Color.GRAY

    private val STOP_LOWER_THRESHOLD = 0.1
    private val STOP_UPPER_THRESHOLD = 0.5

    private var vibrator: Vibrator

    private val resultsBuffer: MutableList<Boolean> = ArrayList()

    init {
        setName(name)
        // Init the communication arrays:
        texts = Array(NUM_CLASSES) { "" }
        colors = Array(NUM_CLASSES) { Color.GRAY }
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun respond(predictionsBuffer: TensorLabel) {
        // Choose the texts and colors:
        var maxIndex = 0 // index of the predicted class
        var maxVal: Float = Float.MIN_VALUE // prob of the predicted class
        var maxLabel = "" // label of the predicted class
        labelsList.mapIndexed { i, label ->
            val prob = predictionsBuffer.mapWithFloatValue[label]
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
        resultsBuffer.add(maxLabel == "Sitting" || maxLabel == "Standing") // if the user is idle
        titleColor = Color.GRAY // default color
        if (resultsBuffer.count() == STOP_FREQ_CHECK) { // if the buffer filled up
            val idleFraction = resultsBuffer.count { it }.toDouble() / STOP_FREQ_CHECK
            if (idleFraction <= STOP_LOWER_THRESHOLD) { // should enable the GPS when moving
                titleColor = Color.GREEN
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else if (idleFraction >= STOP_UPPER_THRESHOLD) { // should disable the GPS when idle
                titleColor = Color.RED
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            }
            resultsBuffer.clear() // reduces the update frequency and keeps the list bounded
        }
    }

    override fun accessMemoryObjects() {
    }

    override fun calculateActivation() {
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun proc() {
        inputs.forEach { mo ->
            if (mo.i != null && mo.i is TensorLabel) {
                respond(mo.i as TensorLabel)
                // mo.i = null // makes this codelet waits for the next prediction
            }
        }
    }
}