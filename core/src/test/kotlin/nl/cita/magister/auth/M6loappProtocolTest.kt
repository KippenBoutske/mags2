package nl.cita.magister.auth

import java.net.URL
import nl.cita.magister.api.MagisterEndpoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M6loappProtocolTest {
    @Test
    fun streamHandlerCapturesPostedCode() {
        M6loappProtocol.install()
        var seen: String? = null
        M6loappProtocol.listener = { seen = it }
        val url = URL("m6loapp://oauth2redirect/")
        val connection = url.openConnection()
        connection.doOutput = true
        connection.getOutputStream().use { it.write("code=tok123&state=s".toByteArray()) }
        connection.getInputStream().use { it.readBytes() }
        assertNotNull(seen)
        assertEquals("tok123", AuthRequestFactory.extractCode(seen!!))
        assertTrue(seen!!.startsWith(MagisterEndpoints.REDIRECT_URI.trimEnd('/')))
    }

    @Test
    fun streamHandlerCapturesGetQuery() {
        M6loappProtocol.install()
        var seen: String? = null
        M6loappProtocol.listener = { seen = it }
        val url = URL("m6loapp://oauth2redirect/?code=fromGet&state=s")
        url.openConnection().getInputStream().use { it.readBytes() }
        assertEquals("fromGet", AuthRequestFactory.extractCode(seen!!))
    }
}
