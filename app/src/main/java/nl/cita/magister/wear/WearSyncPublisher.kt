package nl.cita.magister.wear

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import nl.cita.magister.CitaApp

object WearSyncPublisher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncAsync(context: Context) {
        val app = context.applicationContext
        scope.launch { runCatching { sync(app) } }
    }

    suspend fun sync(context: Context) {
        val app = context.applicationContext as? CitaApp ?: return
        val repo = app.container.repository
        if (!repo.isLoggedIn() && !repo.isDemo()) {
            clear(app)
            return
        }
        val profile = repo.observeProfile().first()
        val appointments = repo.cachedAppointments()
        val grades = repo.cachedGrades()
        val latestGrades = repo.observeLatestGrades().first()
        val homework = repo.observeHomework().first()

        val payload = WearSyncPayload.build(
            profile = profile,
            appointments = appointments,
            grades = grades,
            latestGrades = latestGrades,
            homework = homework,
        )

        val request = PutDataMapRequest.create(WearSyncContract.PATH_SNAPSHOT).apply {
            dataMap.putString(WearSyncContract.KEY_PAYLOAD, payload.toJson())
            dataMap.putLong(WearSyncContract.KEY_UPDATED_AT, payload.updatedAtMillis)
        }.asPutDataRequest().setUrgent()

        runCatching {
            Wearable.getDataClient(app).putDataItem(request).await()
        }
    }

    fun clearAsync(context: Context) {
        val app = context.applicationContext
        scope.launch { runCatching { clear(app) } }
    }

    suspend fun clear(context: Context) {
        val uri = Uri.Builder()
            .scheme("wear")
            .path(WearSyncContract.PATH_SNAPSHOT)
            .build()
        runCatching {
            Wearable.getDataClient(context.applicationContext).deleteDataItems(uri).await()
        }
    }
}
