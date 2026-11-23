package nl.cita.magister.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import nl.cita.magister.CitaApp

class WearSyncListenerService : WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearSyncContract.PATH_REQUEST_SYNC) return
        val app = applicationContext as? CitaApp ?: return
        serviceScope.launch {
            runCatching {
                val repo = app.container.repository
                if (repo.isLoggedIn() && !repo.isDemo()) {
                    repo.refreshAll()
                }
                WearSyncPublisher.sync(app)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
