package com.cmob.pattern_rec_mobile

import android.app.Service
import android.content.Intent
import android.os.*
import com.cmob.pattern_rec_mobile.agent.PatternRecMind

class MainService : Service() {

    // Agent mind:
    private lateinit var patternRecMind: PatternRecMind<MainService>

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
        patternRecMind = PatternRecMind(this)
        started = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        patternRecMind.stopMind()
    }
}