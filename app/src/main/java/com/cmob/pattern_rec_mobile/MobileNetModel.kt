package com.cmob.pattern_rec_mobile

import android.content.Context
import com.cmob.pattern_rec_mobile.ml.AccSensorModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class MobileNetModel(private val context: Context) {

    private val model = AccSensorModel.newInstance(context)
    private val inputFeatures =
        TensorBuffer.createFixedSize(intArrayOf(1, 1, 90, 3), DataType.FLOAT32)

    fun analyze(): TensorBuffer {
        val outputs = model.process(inputFeatures)
        return outputs.outputFeature0AsTensorBuffer
    }

    fun close() {
        model.close()
    }
}