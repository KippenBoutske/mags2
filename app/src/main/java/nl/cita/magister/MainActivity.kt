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
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.crash.CrashGuard
import nl.cita.magister.ui.CitaRoot
import nl.cita.magister.ui.CitaViewModel
import nl.cita.magister.ui.theme.CitaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CitaViewModel by viewModels { CitaViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            runCatching { enableEdgeToEdge() }
            handleAuthIntent(intent)
            if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_DEMO, false)) {
                viewModel.enterDemo()
            }
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
                CitaTheme(safeMode = safeMode, dynamicColor = dynamic) {
                    CitaRoot(viewModel)
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
    }
}
