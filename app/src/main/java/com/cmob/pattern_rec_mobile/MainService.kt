package com.cmob.pattern_rec_mobile

import android.app.Service
import android.content.Intent
import android.os.*
import androidx.work.WorkManager
// import com.cmob.pattern_rec_mobile.agent.PatternRecMind
import com.cmob.pattern_rec_mobile.models.AccCNN
import androidx.work.OneTimeWorkRequest
import com.cmob.pattern_rec_mobile.models.DataSetImporter

class MainService : Service() {

    // Agent mind:
    // private lateinit var patternRecMind: PatternRecMind<MainService>
    val workRequest = OneTimeWorkRequest.Builder(AccCNN::class.java).build()
    private lateinit var data_set: DataSetImporter

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
        // patternRecMind = PatternRecMind(this)
        data_set = DataSetImporter(R.raw.acc_sensor_data, R.raw.acc_sensor_labels, this)
        AccCNN.data_set = data_set // TODO: Remove this workaround
        WorkManager.getInstance(this).enqueue(workRequest)
        started = true
        return START_STICKY
    }

    /*override fun onDestroy() {
        super.onDestroy()
        patternRecMind.stopMind()
    }*/
}