package nl.cita.magister.desktop

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.cita.magister.auth.StoredSession
import nl.cita.magister.desktop.data.DesktopAuthCallback
import nl.cita.magister.desktop.data.DesktopCache
import nl.cita.magister.desktop.data.DesktopHttpClient
import nl.cita.magister.desktop.data.DesktopPlanGoonClient
import nl.cita.magister.desktop.data.DesktopPrefs
import nl.cita.magister.desktop.data.DesktopRepository
import nl.cita.magister.desktop.data.DesktopSecretBox
import nl.cita.magister.desktop.data.DesktopTokenStore
import nl.cita.magister.desktop.data.ThemeMode
import nl.cita.magister.desktop.ui.DesktopDaySlot
import nl.cita.magister.desktop.ui.DesktopFormat
import nl.cita.magister.desktop.ui.HwFilter
import nl.cita.magister.desktop.ui.groupHomework
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.InfoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DesktopStorageTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun secretBoxEncryptsAndDecrypts() {
        val dir = tempFolder.newFolder("sec").toPath()
        val box = DesktopSecretBox(dir)
        val plain = "super-geheime-token-123"
        val cipher = box.encrypt(plain)
        assertNotEquals(plain, cipher)
        assertEquals(plain, box.decrypt(cipher))
    }

    @Test
    fun tokenStorePersistsAcrossInstances() {
        val dir = tempFolder.newFolder("tokens").toPath()
        val store1 = DesktopTokenStore(dir)
        assertNull(store1.read())

        val session = StoredSession(
            accessToken = "access-1",
            refreshToken = "refresh-1",
            idToken = null,
            expiresAtEpochMs = System.currentTimeMillis() + 3_600_000L,
            tenantHost = "citadel.magister.net",
            personId = 42,
            enrollmentId = 99,
            displayName = "Jens de Vries",
        )
        store1.write(session)

        val store2 = DesktopTokenStore(dir)
        assertEquals(session, store2.read())
        assertFalse(store2.expiringSoon())

        store2.clear()
        assertNull(store2.read())
    }

    @Test
    fun prefsAndWebhookKeyPersist() {
        val dir = tempFolder.newFolder("prefs").toPath()
        val prefs1 = DesktopPrefs(dir)
        prefs1.setThemeMode(ThemeMode.Dark)
        prefs1.setTenant("citadel.magister.net", "Citadel College", "t-1")
        prefs1.setWebhookUrl("https://example.com/hook")
        prefs1.setWebhookKey("secret-bearer")

        val prefs2 = DesktopPrefs(dir)
        assertEquals(ThemeMode.Dark, prefs2.current.themeMode)
        assertEquals("citadel.magister.net", prefs2.current.tenantHost)
        assertEquals("https://example.com/hook", prefs2.current.webhookUrl)
        assertTrue(prefs2.hasWebhookKey())
        assertEquals("secret-bearer", prefs2.webhookKey())
    }

    @Test
    fun repositoryDemoPopulatesCacheAndExportsJson() = runBlocking {
        val dir = tempFolder.newFolder("repo").toPath()
        val secretBox = DesktopSecretBox(dir)
        val prefs = DesktopPrefs(dir, secretBox)
        val tokens = DesktopTokenStore(dir, secretBox)
        val cache = DesktopCache(dir)
        val repo = DesktopRepository(
            prefs = prefs,
            tokenStore = tokens,
            http = DesktopHttpClient(tokens),
            planGoon = DesktopPlanGoonClient(),
            cache = cache,
        )

        repo.enterDemo()
        assertTrue(repo.isDemo())
        assertNotNull(repo.observeProfile().first())
        assertTrue(repo.observeGrades().first().isNotEmpty())
        assertTrue(repo.observeAppointments().first().isNotEmpty())
        assertTrue(repo.observeMessages().first().isNotEmpty())

        val hw = repo.observeHomework().first()
        assertTrue(hw.isNotEmpty())
        val firstHw = hw.first()
        repo.markHomeworkDone(firstHw, !firstHw.isDone)
        val updated = repo.observeHomework().first().first { it.id == firstHw.id }
        assertEquals(!firstHw.isDone, updated.isDone)

        val json = repo.exportJson()
        assertTrue(json.contains("\"source\""))
        assertTrue(json.contains("\"subjects\""))

        repo.logout()
        assertFalse(repo.isDemo())
        assertNull(repo.observeProfile().first())
    }

    @Test
    fun buildDaySlotsDistinguishesFreePeriodFromBreak() {
        val l1 = Appointment(
            id = 1,
            startIso = "2026-10-22T08:30:00Z",
            endIso = "2026-10-22T09:20:00Z",
            periodFrom = 1,
            periodTo = 1,
            title = "Biologie",
            location = "C2.01",
            classroom = "C2.01",
            teachers = listOf("LNG"),
            subjects = listOf("Biologie"),
            content = null,
            infoType = InfoType.None,
            isCancelled = false,
            isCompleted = false,
            hasAttachments = false,
        )
        val l3 = l1.copy(
            id = 2,
            startIso = "2026-10-22T10:20:00Z",
            endIso = "2026-10-22T11:10:00Z",
            periodFrom = 3,
            periodTo = 3,
            title = "Geschiedenis",
        )
        val l4 = l1.copy(
            id = 3,
            startIso = "2026-10-22T11:25:00Z",
            endIso = "2026-10-22T12:15:00Z",
            periodFrom = 4,
            periodTo = 4,
            title = "Wiskunde",
        )

        val slots = DesktopFormat.buildDaySlots(listOf(l1, l3, l4))
        assertEquals(5, slots.size)
        val gap1 = slots[1] as DesktopDaySlot.Gap
        assertTrue(gap1.isFreePeriod)
        val gap2 = slots[3] as DesktopDaySlot.Gap
        assertFalse(gap2.isFreePeriod)
    }

    @Test
    fun stripHtmlAndGroupHomeworkWork() {
        assertEquals("Hallo\nwereld", DesktopFormat.stripHtml("<p>Hallo<br/>wereld</p>"))
        val emptyBuckets = groupHomework(emptyList(), LocalDate.of(2026, 10, 22), HwFilter.Open)
        assertTrue(emptyBuckets.isEmpty())
    }

    @Test
    fun authCallbackLoopbackForwardsRedirectUrl() {
        val dir = tempFolder.newFolder("ipc").toPath()
        val latch = CountDownLatch(1)
        var received: String? = null
        val listener = DesktopAuthCallback.startListener(dir) { url ->
            received = url
            latch.countDown()
        }
        try {
            val expected = "m6loapp://oauth2redirect/?code=abc123&state=xyz"
            val forwarded = DesktopAuthCallback.tryForwardToRunningInstance(dir, expected)
            assertTrue(forwarded)
            assertTrue(latch.await(3, TimeUnit.SECONDS))
            assertEquals(expected, received)
        } finally {
            listener.close()
        }
    }

    @Test
    fun windowsRegFileEscapesExecutablePath() {
        val reg = DesktopAuthCallback.buildWindowsRegFile("""C:\Users\Jens\AppData\Local\Mags\Mags.exe""")
        assertTrue(reg.contains("Windows Registry Editor Version 5.00"))
        assertTrue(reg.contains("""[HKEY_CURRENT_USER\Software\Classes\m6loapp\shell\open\command]"""))
        assertTrue(reg.contains("""@="\"C:\\Users\\Jens\\AppData\\Local\\Mags\\Mags.exe\" \"%1\"""""))
    }
}
