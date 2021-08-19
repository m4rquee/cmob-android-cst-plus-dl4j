package com.cmob.pattern_rec_mobile.agent

import android.util.Log
import br.unicamp.cst.core.entities.Codelet

class PrintCodelet(name: String) : Codelet() {

    init {
        setName(name)
    }

    override fun accessMemoryObjects() {
    }

    override fun calculateActivation() {
    }

    override fun proc() {
        inputs.forEach { mo ->
            if (mo.i != null)
                Log.d(
                    "PrintCodelet",
                    mo.i.toString() + " received from " + mo.name + " at " + mo.timestamp
                )
        }
    }
}