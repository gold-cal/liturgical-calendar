package com.liturgical.calendar.helpers

import com.secure.commons.extensions.writeLn
import com.secure.commons.helpers.ensureBackgroundThread
import java.io.OutputStream

class Tracer {
    fun exportTrace(outputStream: OutputStream?, output: String) {
        if (outputStream == null) return
        if (output == "") return

        ensureBackgroundThread {
            val values = output.split(";")
            outputStream.bufferedWriter().use {out ->
                for (value in values) {
                    out.writeLn(value)
                }
            }
        }
    }
}
