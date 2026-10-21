package nl.cita.magister

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.ui.CitaRoot
import nl.cita.magister.ui.CitaViewModel
import nl.cita.magister.ui.theme.CitaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CitaViewModel by viewModels { CitaViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthIntent(intent)
        lifecycleScope.launch {
            viewModel.shareJson.collect { json ->
                val clipboard = getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("cita-cijfers.json", json))
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, json)
                    putExtra(Intent.EXTRA_SUBJECT, "Cita cijfers")
                }
                startActivity(Intent.createChooser(share, "Cijfers delen als JSON"))
            }
        }
        setContent {
            CitaTheme {
                CitaRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.toString().startsWith(MagisterEndpoints.REDIRECT_URI.trimEnd('/'))) {
            viewModel.handleRedirect(uri.toString())
        }
    }
}
