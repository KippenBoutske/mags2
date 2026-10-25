package nl.cita.magister.platform

import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * One running desktop app. A second instance started via `m6loapp://…`
 * writes the URL here and exits; the primary consumes it.
 */
object DesktopInstance {
    private val lockFile = java.io.File(appDataDir(), "instance.lock")
    private val handoffFile = java.io.File(appDataDir(), "oauth-handoff.txt")

    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    @Volatile
    var initialRedirect: String? = null
        private set

    private val _redirects = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val redirects: SharedFlow<String> = _redirects.asSharedFlow()

    fun becomePrimary(args: Array<String>): Boolean {
        val incoming = args.firstOrNull { it.startsWith("m6loapp:", ignoreCase = true) }
        lockFile.parentFile.mkdirs()
        return try {
            val ch = FileOutputStream(lockFile).channel
            val acquired = ch.tryLock()
            if (acquired == null) {
                ch.close()
                if (!incoming.isNullOrBlank()) {
                    handoffFile.writeText(incoming)
                    MagsLog.info("handed oauth URL to primary instance")
                } else {
                    MagsLog.info("another Mags instance is already running")
                }
                false
            } else {
                channel = ch
                lock = acquired
                initialRedirect = incoming
                MagsLog.info("primary instance lock acquired initial=${incoming != null}")
                true
            }
        } catch (error: Exception) {
            MagsLog.error("instance lock failed; continuing as primary", error)
            initialRedirect = incoming
            true
        }
    }

    fun startWatcher(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                delay(400)
                if (!handoffFile.exists()) continue
                val text = runCatching {
                    Files.readString(handoffFile.toPath()).trim()
                }.getOrNull()
                runCatching { Files.deleteIfExists(handoffFile.toPath()) }
                if (!text.isNullOrBlank()) {
                    MagsLog.info("consumed oauth handoff")
                    _redirects.emit(text)
                }
            }
        }
    }
}
