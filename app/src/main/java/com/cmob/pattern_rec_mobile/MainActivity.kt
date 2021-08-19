package com.cmob.pattern_rec_mobile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REFRESH_RATE = 1000L
    private lateinit var downstairsTextView: TextView
    private lateinit var joggingTextView: TextView
    private lateinit var sittingTextView: TextView
    private lateinit var standingTextView: TextView
    private lateinit var upstairsTextView: TextView
    private lateinit var walkingTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var textViews: Array<TextView>

    // Used for the service management:
    private lateinit var mService: MainService
    private var mBound: Boolean = false
    private lateinit var serviceIntent: Intent

    // Defines callbacks for service binding, passed to bindService():
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance:
            val binder = service as MainService.MainBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg: ComponentName) {
            mBound = false
        }
    }

    private val updateThread: Thread = object : Thread() {
        override fun run() {
            try {
                while (!this.isInterrupted) {
                    sleep(REFRESH_RATE)
                    /*runOnUiThread {
                        if (mBound && mService.newBatch) {
                            textViews.forEachIndexed { i, textView ->
                                textView.text = mService.texts[i]
                                textView.setTextColor(mService.colors[i])
                                titleTextView.setTextColor(mService.titleColor)
                            }
                            mService.newBatch = false
                        }
                    }*/
                }
            } catch (e: InterruptedException) {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
    }

    fun onClickStart(view: View) {
        if (!mBound) // Bind to LocalService:
            serviceIntent = Intent(this, MainService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
                val text: String
                if (!MainService.started) { // don't start a new service if there is one running
                    text = "Starting the Inference Service"
                    startService(intent)
                } else
                    text = "Connecting to the Inference Service"
                val toast = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
                toast.show()
                updateThread.start() // start the update thread
            }
    }

    private fun stopListening() {
        updateThread.interrupt() // stop the update thread
        // Unbind the service:
        unbindService(connection)
        mBound = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBound) stopListening()
    }

    fun onClickStop(view: View) {
        val toast = Toast.makeText(applicationContext, "Stopping Inference", Toast.LENGTH_SHORT)
        toast.show()
        stopListening()
        stopService(serviceIntent) // stops the service
        this.finishAffinity()
    }
}