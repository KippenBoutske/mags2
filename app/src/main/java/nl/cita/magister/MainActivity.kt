package nl.cita.magister

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
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
    private val viewModel: CitaViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CitaViewModel((application as CitaApp).container) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        handleLaunchExtras(intent)
        val shownAt = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition {
            val elapsed = SystemClock.elapsedRealtime() - shownAt
            if (elapsed >= 1_000L) return@setKeepOnScreenCondition false
            viewModel.booting.value || elapsed < 820L
        }
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
                val hapticStrength by viewModel.hapticStrength.collectAsStateWithLifecycle()
                CitaTheme(safeMode = safeMode, dynamicColor = dynamic) {
                    val haptics = rememberMagsHaptics(hapticsOn, hapticStrength)
                    CompositionLocalProvider(LocalMagsHaptics provides haptics) {
                        CitaRoot(viewModel)
                    }
                }
            }
            if (!safeMode) {
                val content = findViewById<View>(android.R.id.content)
                content.alpha = 0f
                content.scaleX = 0.96f
                content.scaleY = 0.96f
                splash.setOnExitAnimationListener { provider ->
                    playSplashExit(provider, content)
                }
            }
        } catch (error: Throwable) {
            CrashGuard.show(this, error)
            finish()
        }
    }

    private fun playSplashExit(provider: SplashScreenViewProvider, content: View) {
        val spring = PathInterpolator(0.22f, 1f, 0.36f, 1f)
        provider.iconView.animate()
            .scaleX(0.72f)
            .scaleY(0.72f)
            .alpha(0f)
            .setDuration(240)
            .setInterpolator(spring)
            .start()
        provider.view.animate()
            .alpha(0f)
            .setDuration(280)
            .setInterpolator(spring)
            .withEndAction { provider.remove() }
            .start()
        content.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(40)
            .setDuration(360)
            .setInterpolator(spring)
            .start()
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
