package nl.cita.magister.wear.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import nl.cita.magister.wear.WearSyncContract
import nl.cita.magister.wear.WearSyncPayload

object WearSyncClient {
    suspend fun pullCachedFromDataLayer(context: Context): Boolean = runCatching {
        val uri = Uri.Builder()
            .scheme("wear")
            .path(WearSyncContract.PATH_SNAPSHOT)
            .build()
        val items = Wearable.getDataClient(context.applicationContext).getDataItems(uri).await()
        try {
            val latest = items.firstOrNull() ?: return@runCatching false
            val json = DataMapItem.fromDataItem(latest).dataMap
                .getString(WearSyncContract.KEY_PAYLOAD) ?: return@runCatching false
            val payload = WearSyncPayload.fromJson(json) ?: return@runCatching false
            WearSnapshotStore.save(context, payload)
            true
        } finally {
            items.release()
        }
    }.getOrDefault(false)

    suspend fun requestPhoneSync(context: Context): Boolean = runCatching {
        val app = context.applicationContext
        val nodes = Wearable.getNodeClient(app).connectedNodes.await()
        if (nodes.isEmpty()) return@runCatching false
        val client = Wearable.getMessageClient(app)
        nodes.forEach { node ->
            client.sendMessage(node.id, WearSyncContract.PATH_REQUEST_SYNC, ByteArray(0)).await()
        }
        true
    }.getOrDefault(false)
}
