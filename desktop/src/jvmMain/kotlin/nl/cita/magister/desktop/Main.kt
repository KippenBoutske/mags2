package nl.cita.magister.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import nl.cita.magister.di.AppContainer
import nl.cita.magister.haptics.LocalMagsHaptics
import nl.cita.magister.haptics.rememberMagsHaptics
import nl.cita.magister.platform.platformShareText
import nl.cita.magister.ui.CitaRoot
import nl.cita.magister.ui.CitaViewModel
import nl.cita.magister.ui.theme.CitaTheme

fun main() = application {
    val container = AppContainer.create()
    val viewModel = CitaViewModel(container)
    val windowState = rememberWindowState(width = 1100.dp, height = 760.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mags",
        state = windowState,
    ) {
        val dynamic by viewModel.dynamicColor.collectAsState()
        val hapticsOn by viewModel.haptics.collectAsState()
        val hapticStrength by viewModel.hapticStrength.collectAsState()
        LaunchedEffect(Unit) {
            viewModel.shareJson.collect { json ->
                platformShareText("Mags cijfers", json, "application/json")
            }
        }
        CitaTheme(dynamicColor = dynamic) {
            val haptics = rememberMagsHaptics(hapticsOn, hapticStrength)
            CompositionLocalProvider(LocalMagsHaptics provides haptics) {
                CitaRoot(viewModel)
            }
        }
    }
}
