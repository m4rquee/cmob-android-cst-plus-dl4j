package com.cmob.pattern_rec_mobile.agent

import android.util.Log
import br.unicamp.cst.core.entities.Codelet
import kotlinx.coroutines.*
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.Updater
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.factory.Nd4j

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet


class DL4JCodelet(name: String) : Codelet() {

    val NUM_SAMPLES = 4
    var trainingInputs: INDArray = Nd4j.zeros(NUM_SAMPLES, 2)
    var trainingOutputs: INDArray = Nd4j.zeros(NUM_SAMPLES, 2)
    lateinit var myData: DataSet
    var myNetwork: MultiLayerNetwork
    var trained = false

    private fun setTrainningData() {
        // If 0,0 show 0
        trainingInputs.putScalar(intArrayOf(0, 0), 0)
        trainingInputs.putScalar(intArrayOf(0, 1), 0)
        trainingOutputs.putScalar(intArrayOf(0, 0), 1)
        trainingOutputs.putScalar(intArrayOf(0, 1), 0)
        // If 0,1 show 1
        trainingInputs.putScalar(intArrayOf(1, 0), 0)
        trainingInputs.putScalar(intArrayOf(1, 1), 1)
        trainingOutputs.putScalar(intArrayOf(1, 0), 0)
        trainingOutputs.putScalar(intArrayOf(1, 1), 1)
        // If 1,0 show 1
        trainingInputs.putScalar(intArrayOf(2, 0), 1)
        trainingInputs.putScalar(intArrayOf(2, 1), 0)
        trainingOutputs.putScalar(intArrayOf(2, 0), 0)
        trainingOutputs.putScalar(intArrayOf(2, 1), 1)
        // If 1,1 show 0
        trainingInputs.putScalar(intArrayOf(3, 0), 1)
        trainingInputs.putScalar(intArrayOf(3, 1), 1)
        trainingOutputs.putScalar(intArrayOf(3, 0), 1)
        trainingOutputs.putScalar(intArrayOf(3, 1), 0)
        myData = DataSet(trainingInputs, trainingOutputs)
    }

    private fun trainNN() {
        val max = 10000
        for (l in 0..max) {
            myNetwork.fit(myData)
            if (l % 1000 == 0) Log.d("DL4JCodelet", "Training - ${l}/${max}")
        }
        trained = true
    }

    init {
        setName(name)
        val inputLayer: DenseLayer = DenseLayer.Builder()
            .nIn(2)
            .nOut(3)
            .name("Input")
            .activation(Activation.SIGMOID)
            .build()
        val hiddenLayer: DenseLayer = DenseLayer.Builder()
            .nIn(3)
            .nOut(2)
            .name("Hidden")
            .activation(Activation.SIGMOID)
            .build()
        val outputLayer: OutputLayer = OutputLayer.Builder()
            .nIn(2)
            .nOut(2)
            .name("Output")
            .activation(Activation.SOFTMAX)
            .build()

        val nncBuilder = NeuralNetConfiguration.Builder()
        nncBuilder.updater(Updater.ADAM)
        val listBuilder = nncBuilder.list()
        listBuilder.layer(0, inputLayer)
        listBuilder.layer(1, hiddenLayer)
        listBuilder.layer(2, outputLayer)
        // listBuilder.backprop(true)

        setTrainningData()
        myNetwork = MultiLayerNetwork(listBuilder.build())
    }

    override fun accessMemoryObjects() {
    }

    override fun calculateActivation() {
    }

    override fun proc() {
        if (trained) {
            val networkResults: INDArray? = myNetwork.output(trainingInputs)
            Log.d("DL4JCodelet", networkResults.toString())
        } else {
            runBlocking {
                launch {
                    myNetwork.init()
                    Log.d("DL4JCodelet", "Training started")
                    trainNN()
                    Log.d("DL4JCodelet", "Training finished")
                }
            }
        }
    }
}