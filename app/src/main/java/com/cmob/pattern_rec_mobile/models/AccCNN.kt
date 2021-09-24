package com.cmob.pattern_rec_mobile.models

import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.*
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.text.DateFormat
import java.util.*

class AccCNN(
    @NonNull var context: Context, @NonNull workerParams: WorkerParameters
) : Worker(context, workerParams) {
    // The model's input is the accelerometer data collected for 4,5s at 20Hz (90 samples).
    // Then the axis are "glued" side by side forming a 90x3 matrix.

    private var input_shape: Triple<Long, Long, Long> = Triple(1, 90, 3)
    private var num_classes: Int = 6
    private var learning_rate: Double = 1e-4
    private var l2_rate: Double = 1E-4
    private var seed: Long = 42

    private lateinit var model: MultiLayerNetwork

    companion object {
        lateinit var data_set: DataSetImporter
    }

    private fun build() {
        val builder = NeuralNetConfiguration.Builder()
            .seed(seed)
            .l2(l2_rate)
            .updater(Adam(learning_rate))
            .weightInit(WeightInit.XAVIER)
            .list()
            .setInputType(
                InputType.convolutional(
                    input_shape.first, input_shape.second, input_shape.third
                )
            )
            .layer(
                ConvolutionLayer.Builder(1, 16)
                    .stride(1, 1)
                    .activation(Activation.RELU)
                    .name("Conv2D-32-16")
                    // .nIn(input_shape.third)
                    .nOut(32)
                    .build()
            )
            .layer(
                SubsamplingLayer.Builder(PoolingType.MAX)
                    .kernelSize(1, 4)
                    .stride(1, 2)
                    .name("1st-MaxPool2D-1-4").build()
            )
            .layer(
                ConvolutionLayer.Builder(1, 8)
                    .stride(1, 1)
                    .activation(Activation.RELU)
                    .name("Conv2D-64-8")
                    // .nIn(input_shape.third)
                    .nOut(64)
                    .build()
            )
            .layer(
                SubsamplingLayer.Builder(PoolingType.MAX)
                    .kernelSize(1, 4)
                    .stride(1, 2)
                    .name("2nd-MaxPool2D-1-4").build()
            )
            .layer(

                ConvolutionLayer.Builder(1, 4)
                    .stride(1, 1)
                    .activation(Activation.RELU)
                    .name("Conv2D-96-4")
                    // .nIn(input_shape.third)
                    .nOut(96)
                    .build()
            )
            .layer(
                OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                    .activation(Activation.SOFTMAX)
                    .name("classify")
                    .nOut(num_classes)
                    .build()
            )

        model = MultiLayerNetwork(builder.build())
        model.init()
    }

    private fun train(epochs: Int = 10, batch_size: Int = 32) {
        model.setListeners(ScoreIterationListener(100))
        var i = 500
        while (i < data_set.size()) {
            var currentDateTimeString = DateFormat.getDateTimeInstance().format(Date())
            val dataSetIterator = data_set.getDataSetIterator(batch_size, i, seed)
            Log.d("AccCNN", "$currentDateTimeString Using $i samples")

            model.fit(dataSetIterator, epochs)

            currentDateTimeString = DateFormat.getDateTimeInstance().format(Date())
            Log.d("AccCNN", "$currentDateTimeString Finished $i samples")
            i += 500
        }
    }

    override fun doWork(): Result {
        Log.d("MainService", "Creating model")
        build()
        Log.d("MainService", "Training started")
        train()
        Log.d("MainService", "Training finished")
        return Result.success()
    }
}