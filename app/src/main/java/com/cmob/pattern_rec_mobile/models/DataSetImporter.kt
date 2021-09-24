package com.cmob.pattern_rec_mobile.models

import android.util.Log
import org.nd4j.linalg.factory.Nd4j
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator
import org.nd4j.linalg.dataset.DataSet
import com.github.dreamolight.jnpy.Npy
import android.content.Context
import java.util.*

class DataSetImporter(dataset_id: Int, labels_id: Int, context: Context) {
    private val input =
        Nd4j.create(Npy(context.resources.openRawResource(dataset_id)).doubleElements())
            .reshape(24403, 3, 1, 90)
            // .reshape(24403, 1, 90, 3)
    private val output =
        Nd4j.create(Npy(context.resources.openRawResource(labels_id)).uint16Elements().map { it })
            .reshape(24403, 6)

    // Join input and output matrices into a dataset:
    private val dataSet: DataSet = DataSet(input, output)

    // Convert the dataset into a list:
    private val listDataSet: List<DataSet> = dataSet.asList()

    init {
        Log.d("DataSetImporter", "Dataset loaded")
    }

    fun size(): Long {
        return input.size(0)
    }

    fun getDataSetIterator(
        batch_size: Int = 32,
        samples: Int,
        seed: Long = 42
    ): ListDataSetIterator<DataSet> {
        // Shuffle its content randomly:
        listDataSet.shuffled(Random(seed))
        // Build and return a dataset iterator that the network can use:
        return ListDataSetIterator(listDataSet.subList(0, samples), batch_size)
    }
}