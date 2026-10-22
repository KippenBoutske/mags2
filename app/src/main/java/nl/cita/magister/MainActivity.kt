package nl.cita.magister

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.crash.CrashGuard
import nl.cita.magister.haptics.LocalMagsHaptics
import nl.cita.magister.haptics.rememberMagsHaptics
import nl.cita.magister.ui.CitaRoot
import nl.cita.magister.ui.CitaViewModel
import nl.cita.magister.ui.theme.CitaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CitaViewModel by viewModels { CitaViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        handleLaunchExtras(intent)
        splash.setKeepOnScreenCondition { viewModel.booting.value }
        try {
            runCatching { enableEdgeToEdge() }
            handleAuthIntent(intent)
            requestNotifyPermission()
            lifecycleScope.launch {
                viewModel.shareJson.collect { json ->
                    val clipboard = getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("mags-cijfers.json", json))
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, json)
                        putExtra(Intent.EXTRA_SUBJECT, "Mags cijfers")
                    }
                    startActivity(Intent.createChooser(share, "Cijfers delen"))
                }
            }
            val safeMode = CrashGuard.isSafeMode(this)
            setContent {
                val dynamic by viewModel.dynamicColor.collectAsStateWithLifecycle()
                val hapticsOn by viewModel.haptics.collectAsStateWithLifecycle()
                CitaTheme(safeMode = safeMode, dynamicColor = dynamic) {
                    val haptics = rememberMagsHaptics(hapticsOn)
                    CompositionLocalProvider(LocalMagsHaptics provides haptics) {
                        CitaRoot(viewModel)
                    }
                }
            }
        } catch (error: Throwable) {
            CrashGuard.show(this, error)
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
        handleLaunchExtras(intent)
    }

    private fun handleLaunchExtras(intent: Intent?) {
        val page = nl.cita.magister.ui.nav.MagsChrome.fromShortcut(intent?.getStringExtra(EXTRA_PAGE))
        if (page != null) viewModel.openPage(page)
        val clock = nl.cita.magister.ui.AppClock.parse(intent?.getStringExtra(EXTRA_CLOCK))
        if (clock != null) viewModel.setClock(clock)
        val offline = intent?.getBooleanExtra(EXTRA_OFFLINE, false) == true
        val fixture = nl.cita.magister.fixtures.DemoFixture.parse(intent?.getStringExtra(EXTRA_FIXTURE))
        if (intent?.getBooleanExtra(EXTRA_DEMO, false) == true) {
            viewModel.enterDemo(fixture, offline)
        } else if (offline) {
            viewModel.setOffline(true)
        }
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val raw = uri.toString()
        if (raw.startsWith(MagisterEndpoints.REDIRECT_URI.trimEnd('/'))) {
            // Custom Tabs kunnen code in query of fragment zetten; beide doorgeven.
            viewModel.handleRedirect(raw)
        }
    }

    private fun requestNotifyPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 33)
    }

    companion object {
        const val EXTRA_DEMO = "demo"
        const val EXTRA_PAGE = "page"
        const val EXTRA_CLOCK = "clock"
        const val EXTRA_FIXTURE = "fixture"
        const val EXTRA_OFFLINE = "offline"
    }
}
