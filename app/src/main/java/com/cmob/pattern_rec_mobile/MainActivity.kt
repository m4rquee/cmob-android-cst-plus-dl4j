package com.cmob.pattern_rec_mobile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mobileNetModel: MobileNetModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toast = Toast.makeText(applicationContext, "onCreate", Toast.LENGTH_SHORT)
        toast.show()
        mobileNetModel = MobileNetModel(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mobileNetModel.close()
    }

    fun onClick(view: View) {
        runModel()
    }

    private fun runModel() {
        val ret = mobileNetModel.analyze()
        val toast = Toast.makeText(applicationContext, ret.buffer.toString(), Toast.LENGTH_SHORT)
        toast.show()
    }
}