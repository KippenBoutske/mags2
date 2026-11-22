package nl.cita.magister.desktop.data

import java.awt.Desktop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread

/**
 * Handles opening the system browser for Magister OAuth, forwarding `m6loapp://`
 * redirects to an already-running Mags instance over loopback, and registering
 * the `m6loapp://` URI protocol handler in HKCU on Windows.
 */
object DesktopAuthCallback {
    private const val PORT_FILE = "ipc.port"

    fun openInBrowser(url: String): Boolean = runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
            true
        } else {
            val cmd = when {
                DesktopPaths.isWindows -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
                DesktopPaths.isMac -> listOf("open", url)
                else -> listOf("xdg-open", url)
            }
            ProcessBuilder(cmd).start()
            true
        }
    }.getOrDefault(false)

    /**
     * If another Mags Desktop instance is already listening on loopback, forward
     * the redirect URL to it and return true so the caller can exit cleanly.
     */
    fun tryForwardToRunningInstance(dataDir: Path, redirectUrl: String): Boolean {
        val portFile = dataDir.resolve(PORT_FILE)
        if (!Files.exists(portFile)) return false
        val port = runCatching { Files.readString(portFile).trim().toInt() }.getOrNull() ?: return false
        return runCatching {
            Socket(InetAddress.getLoopbackAddress(), port).use { socket ->
                socket.soTimeout = 1500
                PrintWriter(socket.getOutputStream(), true).println(redirectUrl)
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Starts a lightweight loopback listener so `m6loapp://` invocations from
     * Windows Explorer/browser land directly in the active Mags window.
     */
    fun startListener(dataDir: Path, onRedirectUrl: (String) -> Unit): AutoCloseable {
        val server = ServerSocket(0, 8, InetAddress.getLoopbackAddress())
        val portFile = dataDir.resolve(PORT_FILE)
        Files.createDirectories(dataDir)
        Files.writeString(portFile, server.localPort.toString())

        val worker = thread(isDaemon = true, name = "mags-oauth-ipc") {
            while (!server.isClosed) {
                try {
                    val client = server.accept()
                    client.use { socket ->
                        val line = BufferedReader(InputStreamReader(socket.getInputStream())).readLine()
                        if (!line.isNullOrBlank() && line.startsWith("m6loapp://", ignoreCase = true)) {
                            onRedirectUrl(line.trim())
                        }
                    }
                } catch (_: Throwable) {
                    if (server.isClosed) break
                }
            }
        }

        return AutoCloseable {
            runCatching { server.close() }
            runCatching { Files.deleteIfExists(portFile) }
            worker.interrupt()
        }
    }

    /**
     * Registers `m6loapp://` under HKEY_CURRENT_USER on Windows (no admin needed)
     * pointing to the current Mags executable so browser login redirects straight
     * back into Mags Desktop.
     */
    fun registerWindowsProtocol(): Result<String> {
        if (!DesktopPaths.isWindows) {
            return Result.failure(IllegalStateException("Alleen beschikbaar op Windows."))
        }
        val executable = ProcessHandle.current().info().command().orElse(null)
            ?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Kan pad naar Mags.exe niet bepalen."))

        val commandValue = "\"$executable\" \"%1\""
        return runCatching {
            runReg("add", "HKCU\\Software\\Classes\\m6loapp", "/ve", "/d", "URL:Mags Magister Login", "/f")
            runReg("add", "HKCU\\Software\\Classes\\m6loapp", "/v", "URL Protocol", "/d", "", "/f")
            runReg(
                "add",
                "HKCU\\Software\\Classes\\m6loapp\\shell\\open\\command",
                "/ve",
                "/d",
                commandValue,
                "/f",
            )
            "m6loapp:// gekoppeld aan $executable"
        }
    }

    private fun runReg(vararg args: String) {
        val process = ProcessBuilder(listOf("reg") + args)
            .redirectErrorStream(true)
            .start()
        val exit = process.waitFor()
        if (exit != 0) {
            val out = process.inputStream.bufferedReader().readText().trim()
            error("reg.exe mislukt (code $exit): $out")
        }
    }
}
