package nl.cita.magister.desktop.ui.screens

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/**
 * Embedded JavaFX WebView login window that intercepts Magister's
 * `m6loapp://oauth2redirect/#code=...` redirect directly inside the app —
 * works seamlessly with Microsoft SSO / Entra ID without needing Chrome or F12.
 */
object InAppLoginWindow {
    private val jfxInitialized = AtomicBoolean(false)

    fun open(
        authorizeUrl: String,
        title: String = "Inloggen bij Magister",
        onRedirectUrl: (String) -> Unit,
        onError: (String) -> Unit = {},
    ) {
        SwingUtilities.invokeLater {
            try {
                if (jfxInitialized.compareAndSet(false, true)) {
                    Platform.setImplicitExit(false)
                }

                val handled = AtomicBoolean(false)
                val frame = JFrame(title).apply {
                    defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
                    preferredSize = Dimension(520, 740)
                    minimumSize = Dimension(420, 560)
                    layout = BorderLayout()
                }

                val fxPanel = JFXPanel()
                frame.add(fxPanel, BorderLayout.CENTER)
                frame.pack()
                frame.setLocationRelativeTo(null)

                fun deliverRedirect(url: String?) {
                    if (url.isNullOrBlank()) return
                    if (!url.startsWith("m6loapp://", ignoreCase = true)) return
                    if (!handled.compareAndSet(false, true)) return
                    SwingUtilities.invokeLater {
                        runCatching { frame.dispose() }
                        onRedirectUrl(url)
                    }
                }

                frame.addWindowListener(
                    object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent?) {
                            Platform.runLater {
                                runCatching { (fxPanel.scene?.root as? WebView)?.engine?.load("about:blank") }
                            }
                        }
                    },
                )

                Platform.runLater {
                    try {
                        val webView = WebView()
                        val engine = webView.engine
                        engine.isJavaScriptEnabled = true
                        engine.userAgent =
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

                        engine.locationProperty().addListener { _, _, newLocation ->
                            deliverRedirect(newLocation)
                        }

                        engine.loadWorker.stateProperty().addListener { _, _, _ ->
                            deliverRedirect(engine.location)
                        }

                        engine.loadWorker.exceptionProperty().addListener { _, _, ex ->
                            deliverRedirect(engine.location)
                            val msg = ex?.message.orEmpty()
                            val idx = msg.indexOf("m6loapp://", ignoreCase = true)
                            if (idx >= 0) {
                                val candidate = msg.substring(idx).takeWhile { !it.isWhitespace() && it != '\'' && it != '"' }
                                deliverRedirect(candidate)
                            }
                        }

                        fxPanel.scene = Scene(webView)
                        engine.load(authorizeUrl)
                    } catch (err: Throwable) {
                        SwingUtilities.invokeLater {
                            runCatching { frame.dispose() }
                            onError("Kon ingebouwde browser niet starten: ${err.message}")
                        }
                    }
                }

                frame.isVisible = true
                frame.toFront()
                frame.requestFocus()
            } catch (err: Throwable) {
                onError("Kon inlogvenster niet openen: ${err.message}")
            }
        }
    }
}
