package com.cmob.pattern_rec_mobile.agent

import android.app.Service
import android.os.Build
import androidx.annotation.RequiresApi
import br.unicamp.cst.core.entities.Codelet
import com.cmob.pattern_rec_mobile.MobileNetModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class NNModelCodelet<T : Service>(name: String, context: T) : Codelet() {

    var NUM_CLASSES: Int = 0
        private set
    val labelsList
        get() = mobileNetModel.labelsList
    private val MODEL_WINDOW: Int = 90
    private var mobileNetModel: MobileNetModel
    private val modelInputTensor =
        TensorBuffer.createFixedSize(intArrayOf(1, 1, MODEL_WINDOW, 3), DataType.FLOAT32)

    // Calculated from the dataset and used for normalization:
    private val mus = arrayOf(0.6628660249577367f, 7.2556261603547405f, 0.41107842498640323f)
    private val sigmas = arrayOf(6.849043077510763f, 6.746213296062538f, 4.754117645813611f)

    private var predictionsBuffer: TensorLabel? = null

    init {
        setName(name)
        mobileNetModel = MobileNetModel(context) // creates the NN model
        NUM_CLASSES = mobileNetModel.labelsList.size
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun runModel(accBuffer: ArrayList<FloatArray>) {
        val accArray = FloatArray(3 * MODEL_WINDOW)
        accBuffer.takeLast(MODEL_WINDOW).forEachIndexed { i, sample ->
            accArray[3 * i] = (sample[0] - mus[0]) / sigmas[0]
            accArray[3 * i + 1] = (sample[1] - mus[1]) / sigmas[1]
            accArray[3 * i + 2] = (sample[2] - mus[2]) / sigmas[2]
        } // converts the queue into a flattened array of floats normalizing the data
        modelInputTensor.loadArray(accArray)
        // Runs the model with the buffered data:
        predictionsBuffer = mobileNetModel.analyze(modelInputTensor)
    }

    override fun accessMemoryObjects() {
    }

    override fun calculateActivation() {
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun proc() {
        inputs.forEach { mo ->
            val info = mo.i
            if (info is ArrayList<*> && info.size >= MODEL_WINDOW) {
                synchronized(info) {
                    runModel(info as ArrayList<FloatArray>)
                    info.clear() // reduces the update frequency and keeps the list bounded
                }
            }
        }
        for (mo in outputs) mo.i = predictionsBuffer
    }

    override fun stop() {
        super.stop()
        mobileNetModel.close()
    }
}