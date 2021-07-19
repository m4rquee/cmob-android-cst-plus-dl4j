package com.cmob.pattern_rec_mobile

import android.content.Context
import androidx.annotation.NonNull
import com.cmob.pattern_rec_mobile.ml.AccSensorModel
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class MobileNetModel(context: Context) {

    private val model = AccSensorModel.newInstance(context)
    val labelsList =
        arrayListOf("Downstairs", "Jogging", "Sitting", "Standing", "Upstairs", "Walking")

    @NonNull
    fun analyze(@NonNull input: TensorBuffer): TensorLabel {
        val outputs = model.process(input)
        val outputBuffer = outputs.outputFeature0AsTensorBuffer
        return TensorLabel(labelsList, outputBuffer)
    }

    fun close() {
        model.close()
    }
}