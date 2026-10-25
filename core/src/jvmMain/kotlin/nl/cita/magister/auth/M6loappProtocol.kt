package nl.cita.magister.auth

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.nio.charset.StandardCharsets
import nl.cita.magister.platform.MagsLog

/**
 * Captures GET/POST to `m6loapp://` from JavaFX WebKit (and anything else
 * that uses `java.net.URL`). Magister's default hybrid response is form_post:
 * code/state live in the POST body, not the URL.
 */
object M6loappProtocol {
    const val SCHEME = "m6loapp"

    @Volatile
    var listener: ((String) -> Unit)? = null

    fun install() {
        runCatching {
            URL.setURLStreamHandlerFactory { protocol ->
                if (protocol.equals(SCHEME, ignoreCase = true)) M6loappUrlStreamHandler() else null
            }
            MagsLog.info("registered m6loapp URLStreamHandlerFactory")
        }.onFailure { error ->
            MagsLog.info("URLStreamHandlerFactory already set (${error.javaClass.simpleName}); ServiceLoader provider may still apply")
        }
    }

    fun notify(action: String, body: String, source: String = "url-handler") {
        val redirect = AuthRequestFactory.redirectFromFormPost(action, body)
            ?: action.takeIf {
                AuthRequestFactory.extractCode(it) != null || AuthRequestFactory.extractError(it) != null
            }
            ?: body.takeIf { AuthRequestFactory.extractCode(it) != null }
        val code = redirect?.let { AuthRequestFactory.extractCode(it) }
        val error = redirect?.let { AuthRequestFactory.extractError(it) }
        MagsLog.info(
            "m6loapp hit source=$source action=${action.take(96)} bodyChars=${body.length} " +
                "hasCode=${code != null} hasError=${error != null}",
        )
        if (code != null) {
            MagsLog.info("oauth code captured via $source (len=${code.length})")
        } else if (error != null) {
            MagsLog.info("oauth error captured via $source: $error")
        }
        if (redirect != null) listener?.invoke(redirect)
    }
}

class M6loappUrlStreamHandler : URLStreamHandler() {
    override fun openConnection(url: URL): URLConnection = M6loappUrlConnection(url)

    override fun openConnection(url: URL, proxy: Proxy): URLConnection = M6loappUrlConnection(url)
}

class M6loappUrlConnection(url: URL) : URLConnection(url) {
    private val posted = ByteArrayOutputStream()
    private var response: ByteArray? = null
    private var notified = false

    override fun connect() {
        connected = true
    }

    override fun setDoOutput(dooutput: Boolean) {
        super.setDoOutput(dooutput)
    }

    override fun getDoOutput(): Boolean = true

    override fun getOutputStream(): OutputStream {
        connect()
        return object : FilterOutputStream(posted) {
            override fun close() {
                super.close()
                flushNotify("url-handler-post-close")
            }
        }
    }

    override fun getInputStream(): InputStream {
        connect()
        flushNotify("url-handler")
        if (response == null) {
            response = "<html><body>OK</body></html>".toByteArray(StandardCharsets.UTF_8)
        }
        return ByteArrayInputStream(response)
    }

    override fun getContentType(): String = "text/html"

    @Synchronized
    private fun flushNotify(source: String) {
        if (notified) return
        val body = posted.toString(StandardCharsets.UTF_8)
        if (body.isEmpty() && AuthRequestFactory.extractCode(url.toExternalForm()) == null &&
            AuthRequestFactory.extractError(url.toExternalForm()) == null
        ) {
            return
        }
        notified = true
        M6loappProtocol.notify(url.toExternalForm(), body, source)
    }
}

class M6loappUrlStreamHandlerProvider : java.net.spi.URLStreamHandlerProvider() {
    override fun createURLStreamHandler(protocol: String): URLStreamHandler? =
        if (protocol.equals(M6loappProtocol.SCHEME, ignoreCase = true)) {
            M6loappUrlStreamHandler()
        } else {
            null
        }
}

object WindowsM6loappProtocol {
    fun register() {
        if (!System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)) return
        val exe = executable() ?: run {
            MagsLog.info("skip m6loapp protocol registration (not a packaged Mags.exe)")
            return
        }
        val command = "\"${exe.absolutePath}\" \"%1\""
        MagsLog.info("register HKCU m6loapp:// -> $command")
        runReg("HKCU\\Software\\Classes\\m6loapp", "/ve", "/d", "URL:Mags", "/f")
        runReg("HKCU\\Software\\Classes\\m6loapp", "/v", "URL Protocol", "/d", "", "/f")
        runReg("HKCU\\Software\\Classes\\m6loapp\\shell\\open\\command", "/ve", "/d", command, "/f")
    }

    private fun executable(): File? {
        System.getProperty("jpackage.app-path")?.let { path ->
            return File(path).takeIf { it.isFile }
        }
        val command = ProcessHandle.current().info().command().orElse(null) ?: return null
        return File(command).takeIf { it.isFile && it.name.equals("Mags.exe", ignoreCase = true) }
    }

    private fun runReg(vararg args: String) {
        val full = listOf("reg", "add") + args
        val result = runCatching {
            ProcessBuilder(full)
                .redirectErrorStream(true)
                .start()
                .also { it.waitFor() }
        }
        result.onFailure { MagsLog.error("reg ${args.toList()} failed", it) }
    }
}
