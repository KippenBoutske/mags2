package nl.cita.magister.platform

import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

object MagsLog {
    private val logger = Logger.getLogger("nl.cita.magister")

    val logFile: File by lazy {
        File(File(appDataDir(), "logs").apply { mkdirs() }, "mags.log")
    }

    init {
        runCatching {
            logger.useParentHandlers = true
            val handler = FileHandler(logFile.absolutePath, 512 * 1024, 2, true)
            handler.formatter = SimpleFormatter()
            logger.addHandler(handler)
            logger.level = Level.INFO
        }.onFailure { error ->
            System.err.println("Mags: could not open log file: ${error.message}")
        }
    }

    fun info(message: String) {
        logger.info(message)
        println("Mags: $message")
    }

    fun error(message: String, error: Throwable? = null) {
        if (error != null) logger.log(Level.SEVERE, message, error) else logger.severe(message)
        System.err.println("Mags: $message${error?.message?.let { ": $it" }.orEmpty()}")
        error?.printStackTrace()
    }
}
