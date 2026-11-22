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
     * Resolves the path to the packaged Mags.exe launcher on Windows (`jpackage.app-path`),
     * falling back to the current process executable when running outside jpackage.
     */
    fun currentExecutablePath(): String? =
        System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }
            ?: ProcessHandle.current().info().command().orElse(null)?.takeIf { it.isNotBlank() }

    /**
     * Generates a valid Windows `.reg` file registering `m6loapp://` under HKCU.
     * Using `reg import` avoids Java's ProcessBuilder embedded-quote escaping bug on Windows.
     */
    fun buildWindowsRegFile(executablePath: String): String {
        val escaped = executablePath
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return buildString {
            appendLine("Windows Registry Editor Version 5.00")
            appendLine()
            appendLine("[HKEY_CURRENT_USER\\Software\\Classes\\m6loapp]")
            appendLine("@=\"URL:Mags Magister Login\"")
            appendLine("\"URL Protocol\"=\"\"")
            appendLine()
            appendLine("[HKEY_CURRENT_USER\\Software\\Classes\\m6loapp\\shell\\open\\command]")
            appendLine("@=\"\\\"$escaped\\\" \\\"%1\\\"\"")
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
        val executable = currentExecutablePath()
            ?: return Result.failure(IllegalStateException("Kan pad naar Mags.exe niet bepalen."))

        return runCatching {
            val regFile = Files.createTempFile("mags-m6loapp-", ".reg")
            try {
                Files.writeString(regFile, buildWindowsRegFile(executable), Charsets.UTF_8)
                val process = ProcessBuilder("reg", "import", regFile.toAbsolutePath().toString())
                    .redirectErrorStream(true)
                    .start()
                val exit = process.waitFor()
                if (exit != 0) {
                    val out = process.inputStream.bufferedReader().readText().trim()
                    error("reg import mislukt (code $exit): $out")
                }
            } finally {
                runCatching { Files.deleteIfExists(regFile) }
            }
            "m6loapp:// gekoppeld aan $executable"
        }
    }

    /** Best-effort silent registration on Windows startup / login start. */
    fun ensureWindowsProtocolRegistered(): Boolean {
        if (!DesktopPaths.isWindows) return false
        return registerWindowsProtocol().isSuccess
    }
}
