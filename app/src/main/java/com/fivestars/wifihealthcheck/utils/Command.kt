package com.fivestars.wifihealthcheck.utils

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

fun executeAsRoot(command: String, lineBreak: String = ""): String {
    try {
        val proc = Runtime.getRuntime().exec("su")
        val os = DataOutputStream(proc.outputStream)

        os.use {
            it.writeBytes(command + "\n")
            it.writeBytes("exit\n")
            it.flush()
        }

        proc.waitFor()

        val output = StringBuffer()
        val reader = BufferedReader(InputStreamReader(proc.inputStream))

        var line = reader.readLine()
        while (line != null) {
            output.append(line + lineBreak)
            line = reader.readLine()
        }

        return output.toString()
    } catch (exc: InterruptedException) {
        return ""
    } catch (exc: IOException) {
        return ""
    }
}
