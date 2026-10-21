package nl.cita.magister.api

import nl.cita.magister.auth.IdTokenClaims
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class TenantTest {
    @Test
    fun emptyInputDoesNotDefaultToCitadel() {
        assertEquals("", Tenant.normalize("  "))
        assertFalse(Tenant.looksLikeHost(""))
        assertFalse(Tenant.looksLikeHost("citadel"))
        assertFalse(Tenant.looksLikeHost("Citadel College"))
        assertTrue(Tenant.looksLikeHost("citadel.magister.net"))
    }

    @Test
    fun slugCandidatesPreferFirstMeaningfulWord() {
        val slugs = Tenant.slugCandidates("Citadel College")
        assertTrue(slugs.contains("citadel"))
        assertTrue(Tenant.slugCandidates("Karel de Grote College Nijmegen").contains("karel"))
    }

    @Test
    fun idTokenTenantClaimIsRead() {
        val payload = """{"given_name":"Jens","family_name":"de Vries","urn:magister:claims:iam:tenant":"citadel.magister.net"}"""
        val token = fakeJwt(payload)
        assertEquals("citadel.magister.net", IdTokenClaims.tenantHost(token))
        assertEquals("Jens", IdTokenClaims.givenName(token))
        assertEquals("de Vries", IdTokenClaims.familyName(token))
    }

    private fun fakeJwt(payload: String): String {
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray(Charsets.UTF_8))
        return "aaa.$encoded.sig"
    }
}
