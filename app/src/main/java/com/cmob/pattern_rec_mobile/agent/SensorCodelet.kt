package com.cmob.pattern_rec_mobile.agent

import br.unicamp.cst.core.entities.Codelet

abstract class SensorCodelet : Codelet() {

    abstract fun getData(): String

    override fun proc() {
        for (mo in outputs)
            mo.i = getData()
    }
}