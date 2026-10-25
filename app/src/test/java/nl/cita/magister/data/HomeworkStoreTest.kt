package nl.cita.magister.data

import java.nio.file.Files
import nl.cita.magister.data.db.JsonOfflineStore
import nl.cita.magister.data.db.LocalHomeworkState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeworkStoreTest {
    @Test
    fun klaarSurvivesReread() {
        val dir = Files.createTempDirectory("mags-offline-app").toFile()
        val first = JsonOfflineStore(dir)
        first.upsertHomework(LocalHomeworkState(8001, true))
        val again = JsonOfflineStore(dir)
        assertTrue(again.homework.value.getValue(8001).done)
        first.upsertHomework(LocalHomeworkState(8001, false))
        val third = JsonOfflineStore(dir)
        assertFalse(third.homework.value.getValue(8001).done)
    }
}
