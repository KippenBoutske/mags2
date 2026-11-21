package nl.cita.magister.wear.data

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import nl.cita.magister.wear.WearSyncContract
import nl.cita.magister.wear.WearSyncPayload

class WearDataListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.dataItem.uri.path != WearSyncContract.PATH_SNAPSHOT) continue
            when (event.type) {
                DataEvent.TYPE_CHANGED -> {
                    val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val json = map.getString(WearSyncContract.KEY_PAYLOAD) ?: continue
                    val payload = WearSyncPayload.fromJson(json) ?: continue
                    WearSnapshotStore.save(applicationContext, payload)
                }
                DataEvent.TYPE_DELETED -> {
                    WearSnapshotStore.clear(applicationContext)
                }
            }
        }
    }
}
