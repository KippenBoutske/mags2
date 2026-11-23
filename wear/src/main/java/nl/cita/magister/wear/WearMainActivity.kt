package nl.cita.magister.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.parse.MagisterParsers
import nl.cita.magister.wear.data.WearSnapshotStore
import nl.cita.magister.wear.data.WearSyncClient
import nl.cita.magister.wear.ui.MagsWearApp
import java.time.Instant

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearSnapshotStore.load(this)

        setContent {
            val payload by WearSnapshotStore.payload.collectAsStateWithLifecycle()
            var syncing by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            fun requestSync() {
                if (syncing) return
                syncing = true
                scope.launch {
                    WearSyncClient.pullCachedFromDataLayer(this@WearMainActivity)
                    WearSyncClient.requestPhoneSync(this@WearMainActivity)
                    syncing = false
                }
            }

            fun loadDemo() {
                val demoGrades = MockFixtures.grades()
                val demoLatest = MagisterParsers.parseRecentGrades(MockFixtures.recentGradesJson)
                val demo = WearSyncPayload.build(
                    profile = MockFixtures.profile(),
                    appointments = MockFixtures.appointments(),
                    grades = demoGrades,
                    latestGrades = demoLatest,
                    homework = MockFixtures.homework(),
                    now = Instant.parse("2026-10-22T09:00:00Z"),
                )
                WearSnapshotStore.save(this@WearMainActivity, demo)
            }

            LaunchedEffect(Unit) {
                WearSyncClient.pullCachedFromDataLayer(this@WearMainActivity)
                WearSyncClient.requestPhoneSync(this@WearMainActivity)
            }

            MagsWearApp(
                payload = payload,
                syncing = syncing,
                onRequestSync = ::requestSync,
                onLoadDemo = ::loadDemo,
            )
        }
    }
}
