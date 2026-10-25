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
import javax.swing.JPanel
import javax.swing.SwingUtilities
import nl.cita.magister.api.MagisterEndpoints
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
                    onUnavailable = { useWebView = false },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                TextButton(
                    onClick = { useWebView = false },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("WebView lukt niet? Plak de redirect-URL")
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
            "Magister stuurt je terug naar m6loapp://oauth2redirect/ — geen localhost. " +
                "Open de login in je browser of plak die URL hier als de browser hem toont.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = { openSystemBrowser(authorizeUrl) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Magister in de browser")
        }
        OutlinedTextField(
            value = pasted,
            onValueChange = { pasted = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Redirect-URL (m6loapp://…)") },
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
        Text(
            "Op Windows kan het MSI-installatieprogramma later het schema m6loapp:// registreren, " +
                "zodat de browser de app terugroept. Tot die tijd: WebView of plakken.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        val ok = JavaFxBridge.attach(panel, url, onRedirect)
        if (!ok) onUnavailable()
        onDispose { JavaFxBridge.detach(panel) }
    }
    SwingPanel(factory = { panel }, modifier = modifier)
}

private object JavaFxBridge {
    fun available(): Boolean = runCatching {
        Class.forName("javafx.scene.web.WebView")
        Class.forName("javafx.embed.swing.JFXPanel")
        true
    }.getOrDefault(false)

    fun attach(host: JPanel, url: String, onRedirect: (String) -> Unit): Boolean {
        return runCatching {
            val jfxPanelClass = Class.forName("javafx.embed.swing.JFXPanel")
            val platformClass = Class.forName("javafx.application.Platform")
            val panel = jfxPanelClass.getConstructor().newInstance()
            host.removeAll()
            host.add(panel as java.awt.Component, BorderLayout.CENTER)
            val runLater = platformClass.getMethod("runLater", Runnable::class.java)
            runLater.invoke(null, Runnable {
                runCatching {
                    val webViewClass = Class.forName("javafx.scene.web.WebView")
                    val sceneClass = Class.forName("javafx.scene.Scene")
                    val webView = webViewClass.getConstructor().newInstance()
                    val engine = webViewClass.getMethod("getEngine").invoke(webView)
                    val engineClass = engine.javaClass
                    val locationProp = engineClass.getMethod("locationProperty").invoke(engine)
                    val addListener = locationProp.javaClass.methods.first {
                        it.name == "addListener" && it.parameterCount == 1
                    }
                    addListener.invoke(locationProp, javafxChangeListener(onRedirect))
                    engineClass.getMethod("load", String::class.java).invoke(engine, url)
                    val scene = sceneClass.constructors.first { it.parameterCount == 1 }
                        .newInstance(webView)
                    jfxPanelClass.getMethod("setScene", sceneClass).invoke(panel, scene)
                }
            })
            SwingUtilities.invokeLater { host.revalidate(); host.repaint() }
            true
        }.getOrDefault(false)
    }

    fun detach(host: JPanel) {
        SwingUtilities.invokeLater { host.removeAll() }
    }

    private fun javafxChangeListener(onRedirect: (String) -> Unit): Any {
        val listenerClass = Class.forName("javafx.beans.value.ChangeListener")
        return java.lang.reflect.Proxy.newProxyInstance(
            listenerClass.classLoader,
            arrayOf(listenerClass),
        ) { _, method, args ->
            if (method.name == "changed" && args != null && args.size >= 3) {
                val next = args[2]?.toString().orEmpty()
                if (next.startsWith(MagisterEndpoints.REDIRECT_URI.trimEnd('/'))) {
                    onRedirect(next)
                }
            }
            null
        }
    }
}
