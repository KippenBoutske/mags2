package nl.cita.magister.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import java.awt.BorderLayout
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javax.swing.JPanel
import javax.swing.SwingUtilities
import netscape.javascript.JSObject
import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.auth.M6loappFormHook
import nl.cita.magister.auth.M6loappProtocol
import nl.cita.magister.platform.MagsLog
import nl.cita.magister.platform.appDataDir
import nl.cita.magister.platform.openSystemBrowser

@Composable
actual fun LoginWebView(
    url: String,
    onRedirect: (String) -> Unit,
    onClose: () -> Unit,
) {
    var useWebView by remember { mutableStateOf(JavaFxBridge.available()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inloggen bij Magister") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sluiten")
                    }
                },
            )
        },
    ) { padding ->
        if (useWebView) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                JavaFxWebPanel(
                    url = url,
                    onRedirect = onRedirect,
                    onUnavailable = {
                        MagsLog.error("WebView unavailable; falling back to instructions")
                        useWebView = false
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                TextButton(
                    onClick = { useWebView = false },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("WebView lukt niet? Andere manier")
                }
            }
        } else {
            RedirectPasteLogin(
                authorizeUrl = url,
                onRedirect = onRedirect,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun RedirectPasteLogin(
    authorizeUrl: String,
    onRedirect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pasted by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Magister sluit de login vaak af met een form POST naar m6loapp://oauth2redirect/ " +
                "(code zit in de body, niet in de URL). Chrome kan die body niet tonen, " +
                "dus plakken werkt dan niet. Blijf bij voorkeur in de in-app WebView.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Logbestand: ${appDataDir().resolve("logs/mags.log").absolutePath}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { openSystemBrowser(authorizeUrl) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Magister in de browser (werkt niet bij form POST)")
        }
        OutlinedTextField(
            value = pasted,
            onValueChange = { pasted = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Redirect-URL of form-body (code=…)") },
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onRedirect(pasted.trim()) },
                enabled = pasted.contains("m6loapp://") || pasted.contains("code="),
            ) {
                Text("Doorgaan")
            }
        }
    }
}

@Composable
private fun JavaFxWebPanel(
    url: String,
    onRedirect: (String) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val panel = remember { JPanel(BorderLayout()) }
    DisposableEffect(url) {
        val ok = JavaFxBridge.attach(
            host = panel,
            url = url,
            onRedirect = onRedirect,
            onFailure = onUnavailable,
        )
        if (!ok) onUnavailable()
        onDispose { JavaFxBridge.detach(panel) }
    }
    SwingPanel(factory = { panel }, modifier = modifier)
}

class MagsJsBridge {
    fun onRedirect(url: String?) {
        val next = url.orEmpty()
        if (next.isBlank()) return
        M6loappProtocol.notify(next, "", "js-redirect")
    }

    fun onFormPost(action: String?, body: String?) {
        M6loappProtocol.notify(action.orEmpty(), body.orEmpty(), "js-form")
    }
}

private object JavaFxBridge {
    private const val CHROME_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.36"
    private val poller = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "mags-webview-poll").apply { isDaemon = true }
    }
    private var pollTask: ScheduledFuture<*>? = null
    private val captured = AtomicBoolean(false)
    private var engineRef: WebEngine? = null

    fun available(): Boolean = runCatching {
        Class.forName("javafx.scene.web.WebView")
        Class.forName("javafx.embed.swing.JFXPanel")
        Class.forName("javafx.application.Platform")
        true
    }.getOrDefault(false)

    fun attach(
        host: JPanel,
        url: String,
        onRedirect: (String) -> Unit,
        onFailure: () -> Unit,
    ): Boolean {
        captured.set(false)
        M6loappProtocol.listener = { next ->
            if (captured.compareAndSet(false, true)) {
                onRedirect(next)
            }
        }
        return runCatching {
            startJavaFx()
            val createPanel = Runnable {
                val panel = JFXPanel()
                host.removeAll()
                host.add(panel, BorderLayout.CENTER)
                host.revalidate()
                host.repaint()
                Platform.runLater {
                    runCatching {
                        val webView = WebView()
                        val engine = webView.engine
                        engineRef = engine
                        applyUserAgent(engine)
                        engine.isJavaScriptEnabled = true
                        engine.documentProperty().addListener { _, _, doc ->
                            if (doc != null) hookDocument(engine)
                        }
                        engine.locationProperty().addListener { _, _, next ->
                            MagsLog.info("WebView location=$next")
                            if (!next.isNullOrBlank() &&
                                next.startsWith(MagisterEndpoints.REDIRECT_URI.trimEnd('/'))
                            ) {
                                M6loappProtocol.notify(next, "", "location")
                            }
                            hookDocument(engine)
                        }
                        engine.loadWorker.stateProperty().addListener { _, _, state ->
                            val ex = engine.loadWorker.exception
                            MagsLog.info("WebView worker=$state loc=${engine.location} err=${ex?.message}")
                            if (state == Worker.State.SUCCEEDED || state == Worker.State.RUNNING) {
                                hookDocument(engine)
                            }
                            if (state == Worker.State.FAILED) {
                                MagsLog.error("WebView load failed loc=${engine.location}", ex)
                            }
                        }
                        engine.loadWorker.exceptionProperty().addListener { _, _, error ->
                            if (error != null) MagsLog.error("WebView exception", error)
                        }
                        engine.load(url)
                        panel.scene = Scene(webView)
                        MagsLog.info("WebView started url=${url.take(120)}")
                    }.onFailure { error ->
                        MagsLog.error("JavaFX WebView scene setup failed", error)
                        SwingUtilities.invokeLater(onFailure)
                    }
                }
            }
            if (SwingUtilities.isEventDispatchThread()) {
                createPanel.run()
            } else {
                SwingUtilities.invokeAndWait(createPanel)
            }
            true
        }.onFailure { error ->
            MagsLog.error("JavaFX WebView attach failed", error)
        }.getOrDefault(false)
    }

    fun detach(host: JPanel) {
        pollTask?.cancel(false)
        pollTask = null
        engineRef = null
        M6loappProtocol.listener = null
        SwingUtilities.invokeLater { host.removeAll() }
    }

    private fun hookDocument(engine: WebEngine) {
        runCatching {
            val window = engine.executeScript("window") as? JSObject
            window?.setMember("magsBridge", MagsJsBridge())
            val hooked = engine.executeScript(M6loappFormHook.INJECT_JS)
            MagsLog.info("WebView JS hook=$hooked loc=${engine.location}")
            captureForm(engine)
        }.onFailure { error ->
            MagsLog.info("JS hook not ready: ${error.message}")
        }
        ensurePoller(engine)
    }

    private fun ensurePoller(engine: WebEngine) {
        val running = pollTask
        if (running != null && !running.isCancelled) return
        var ticks = 0
        pollTask = poller.scheduleAtFixedRate({
            Platform.runLater {
                if (captured.get() || ++ticks > 1500) {
                    pollTask?.cancel(false)
                    return@runLater
                }
                runCatching {
                    val window = engine.executeScript("window") as? JSObject
                    window?.setMember("magsBridge", MagsJsBridge())
                    engine.executeScript(M6loappFormHook.INJECT_JS)
                    captureForm(engine)
                }
            }
        }, 30, 80, TimeUnit.MILLISECONDS)
    }

    private fun captureForm(engine: WebEngine): Boolean {
        val parsed = M6loappFormHook.parseCaptureResult(
            runCatching { engine.executeScript(M6loappFormHook.CAPTURE_JS) }.getOrNull(),
        ) ?: return false
        M6loappProtocol.notify(parsed.first, parsed.second, "js-scan")
        return true
    }

    private fun startJavaFx() {
        runCatching {
            Platform.startup {}
        }.onFailure { error ->
            val cause = (error as? InvocationTargetException)?.targetException ?: error
            if (cause !is IllegalStateException) throw error
        }
        Platform.setImplicitExit(false)
    }

    private fun applyUserAgent(engine: WebEngine) {
        runCatching {
            val pageField = engine.javaClass.declaredFields.first {
                it.name == "page" || it.type.name.contains("WebPage")
            }
            pageField.isAccessible = true
            val page = pageField.get(engine) ?: return
            page.javaClass.methods.first { it.name == "setUserAgent" && it.parameterCount == 1 }
                .invoke(page, CHROME_UA)
            MagsLog.info("WebView user-agent set")
        }.onFailure { error ->
            MagsLog.info("WebView user-agent unchanged: ${error.message}")
        }
    }
}
