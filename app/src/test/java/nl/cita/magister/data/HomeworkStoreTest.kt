package nl.cita.magister.data

import nl.cita.magister.data.db.JsonOfflineStore
import nl.cita.magister.data.db.LocalHomeworkState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeworkStoreTest {
    @Test
    fun klaarSurvivesReread() {
        @Suppress("DEPRECATION")
        val context = RuntimeEnvironment.getApplication()
        val first = JsonOfflineStore(context)
        first.upsertHomework(LocalHomeworkState(8001, true))
        val again = JsonOfflineStore(context)
        assertTrue(again.homework.value.getValue(8001).done)
        first.upsertHomework(LocalHomeworkState(8001, false))
        val third = JsonOfflineStore(context)
        assertFalse(third.homework.value.getValue(8001).done)
    }
}
