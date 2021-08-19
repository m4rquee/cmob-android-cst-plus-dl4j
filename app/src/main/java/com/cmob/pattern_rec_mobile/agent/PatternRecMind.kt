package com.cmob.pattern_rec_mobile.agent

import android.app.Service
import br.unicamp.cst.core.entities.Mind

class PatternRecMind<T : Service>(context: T) {

    private var mind: Mind

    init {
        mind = prepareMind(context)
    }

    private fun prepareMind(context: T): Mind {
        mind = Mind()

        mind.createCodeletGroup("Sensory")
        mind.createCodeletGroup("Model")
        mind.createCodeletGroup("Motor")

        // Sensory codelet:
        val accMemoryObject = mind.createMemoryObject("ACCMO")
        val accCodelet = ACCSensorCodelet("ACCSensor", context)
        accCodelet.addOutput(accMemoryObject)
        mind.insertCodelet(accCodelet, "Sensory")

        // Model codelet:
        val predMemoryObject = mind.createMemoryObject("PREDMO")
        val nnmodelCodelet = NNModelCodelet("NNMODEL", context)
        nnmodelCodelet.addInput(accMemoryObject)
        nnmodelCodelet.addOutput(predMemoryObject)
        mind.insertCodelet(nnmodelCodelet, "Model")

        // Motor codelets:
        val printCodelet = PrintCodelet("Printer")
        printCodelet.addInput(predMemoryObject)
        mind.insertCodelet(printCodelet, "Motor")
        val responseCodelet = ResponseCodelet(
            "Response",
            nnmodelCodelet.NUM_CLASSES,
            nnmodelCodelet.labelsList,
            context
        )
        responseCodelet.addInput(predMemoryObject)
        mind.insertCodelet(responseCodelet, "Motor")

        mind.start()
        return mind
    }

    fun stopMind() {
        mind.shutDown()
    }
}