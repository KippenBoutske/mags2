package nl.cita.magister.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import nl.cita.magister.desktop.data.DesktopAuthCallback
import nl.cita.magister.desktop.data.DesktopCache
import nl.cita.magister.desktop.data.DesktopHttpClient
import nl.cita.magister.desktop.data.DesktopPaths
import nl.cita.magister.desktop.data.DesktopPendingAuthStore
import nl.cita.magister.desktop.data.DesktopPlanGoonClient
import nl.cita.magister.desktop.data.DesktopPrefs
import nl.cita.magister.desktop.data.DesktopRepository
import nl.cita.magister.desktop.data.DesktopSecretBox
import nl.cita.magister.desktop.data.DesktopTokenStore
import nl.cita.magister.desktop.ui.DesktopPage
import nl.cita.magister.desktop.ui.MagsDesktopApp
import nl.cita.magister.desktop.ui.MagsDesktopController
import java.awt.Dimension
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    val dataDir = DesktopPaths.defaultDataDir()
    val incomingRedirect = args.firstOrNull { it.startsWith("m6loapp://", ignoreCase = true) }

    // If invoked via m6loapp:// while Mags is already open, hand the redirect URL to the running window.
    if (incomingRedirect != null && DesktopAuthCallback.tryForwardToRunningInstance(dataDir, incomingRedirect)) {
        return
    }

    val secretBox = DesktopSecretBox(dataDir)
    val prefs = DesktopPrefs(dataDir, secretBox)
    val tokenStore = DesktopTokenStore(dataDir, secretBox)
    val pendingAuthStore = DesktopPendingAuthStore(dataDir)
    val cache = DesktopCache(dataDir)
    val http = DesktopHttpClient(tokenStore)
    val planGoon = DesktopPlanGoonClient()
    val repo = DesktopRepository(prefs, tokenStore, http, planGoon, cache)

    application {
        val controller = remember { MagsDesktopController(repo, pendingAuthStore) }
        val windowState = rememberWindowState(size = DpSize(1240.dp, 820.dp))

        LaunchedEffect(incomingRedirect) {
            if (incomingRedirect != null) {
                controller.handleRedirectUrl(incomingRedirect)
            }
        }

        DisposableEffect(Unit) {
            val ipc = runCatching {
                DesktopAuthCallback.startListener(dataDir) { redirectUrl ->
                    SwingUtilities.invokeLater {
                        controller.handleRedirectUrl(redirectUrl)
                    }
                }
            }.getOrNull()
            onDispose {
                runCatching { ipc?.close() }
                controller.dispose()
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Mags",
            onPreviewKeyEvent = { event ->
                if (event.type != KeyEventType.KeyDown) return@Window false
                when {
                    event.key == Key.F5 || (event.isCtrlPressed && event.key == Key.R) -> {
                        if (controller.authenticated.value) controller.refreshAll()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.One -> {
                        controller.navigate(DesktopPage.Vandaag); true
                    }
                    event.isCtrlPressed && event.key == Key.Two -> {
                        controller.navigate(DesktopPage.Rooster); true
                    }
                    event.isCtrlPressed && event.key == Key.Three -> {
                        controller.navigate(DesktopPage.Cijfers); true
                    }
                    event.isCtrlPressed && event.key == Key.Four -> {
                        controller.navigate(DesktopPage.Huiswerk); true
                    }
                    event.isCtrlPressed && event.key == Key.Five -> {
                        controller.navigate(DesktopPage.Berichten); true
                    }
                    event.isCtrlPressed && event.key == Key.Six -> {
                        controller.navigate(DesktopPage.Meer); true
                    }
                    else -> false
                }
            },
        ) {
            window.minimumSize = Dimension(900, 620)
            MagsDesktopApp(controller)
        }
    }
}
