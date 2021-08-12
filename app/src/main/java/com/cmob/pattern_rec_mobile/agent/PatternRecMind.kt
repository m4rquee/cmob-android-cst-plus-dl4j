package com.cmob.pattern_rec_mobile.agent

import android.app.Service
import android.hardware.SensorEventListener
import br.unicamp.cst.core.entities.Mind

class PatternRecMind<T>(context: T) where T : Service, T : SensorEventListener {

    private var mind: Mind

    init {
        mind = prepareMind(context)
    }

    private fun prepareMind(context: T): Mind {
        mind = Mind()

        mind.createCodeletGroup("Sensory")
        mind.createCodeletGroup("Motor")

        // Sensory codelet:
        val accMemoryObject = mind.createMemoryObject("ACCMO")
        val accCodelet = ACCSensorCodelet("ACCSensor", context)
        accCodelet.addOutput(accMemoryObject)
        mind.insertCodelet(accCodelet, "Sensory")

        // Sensory Motor:
        val printCodelet = PrintCodelet("Printer")
        printCodelet.addInput(accMemoryObject)
        mind.insertCodelet(printCodelet, "Motor")

        mind.start()
        return mind
    }

    fun stopMind() {
        mind.shutDown()
    }
}