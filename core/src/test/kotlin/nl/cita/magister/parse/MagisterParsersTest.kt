package nl.cita.magister.parse

import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.api.Tenant
import nl.cita.magister.auth.AuthRequestFactory
import nl.cita.magister.auth.Pkce
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.model.GradeColumnType
import nl.cita.magister.model.InfoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MagisterParsersTest {
    @Test
    fun parseAccountAndEnrollment() {
        val enrollments = MagisterParsers.parseEnrollments(MockFixtures.enrollmentsJson)
        val current = MagisterParsers.currentEnrollment(enrollments, "2026-10-21")
        val profile = MagisterParsers.parseProfile(
            MockFixtures.accountJson,
            Tenant.CITADEL_HOST,
            Tenant.CITADEL_DISPLAY,
            current,
        )
        assertEquals(424242, profile.personId)
        assertEquals("Jens", profile.firstName)
        assertEquals("de Vries", profile.lastName)
        assertEquals("5VWO-A", profile.className)
        assertEquals("VWO", profile.study)
        assertEquals(2025001, profile.enrollmentId)
    }

    @Test
    fun parseGradesKeepsWeightAndType() {
        val grades = MagisterParsers.parseGrades(MockFixtures.gradesJson)
        assertEquals(11, grades.size)
        val pw2 = grades.first { it.id == 302 }
        assertEquals(5.3, pw2.numericValue!!, 0.0001)
        assertFalse(pw2.isSufficient)
        assertEquals(3.0, pw2.weight!!, 0.0001)
        assertEquals(GradeColumnType.Grade, pw2.columnType)
        val avg = grades.first { it.id == 103 }
        assertEquals(GradeColumnType.Average, avg.columnType)
        assertFalse(avg.counts)
    }

    @Test
    fun parseAppointmentsAndHomework() {
        val rooster = MagisterParsers.parseAppointments(MockFixtures.appointmentsJson)
        assertEquals(8, rooster.size)
        assertTrue(rooster.first { it.id == 8006 }.isCancelled)
        val homework = MagisterParsers.homeworkFromAppointments(rooster)
        assertTrue(homework.any { it.infoType == InfoType.Homework })
        assertTrue(homework.any { it.infoType == InfoType.Test })
        assertTrue(homework.first { it.id == 8003 }.isDone)
        assertFalse(homework.first { it.id == 8001 }.isDone)
    }

    @Test
    fun parseMessages() {
        val messages = MagisterParsers.parseMessages(MockFixtures.messagesJson)
        assertEquals(3, messages.size)
        assertEquals(2, messages.count { !it.isRead })
        val detail = MockFixtures.message(9001)!!
        assertTrue(detail.bodyHtml!!.contains("PWS-plan"))
        assertEquals("Deadline profielwerkstuk", detail.preview.subject)
    }

    @Test
    fun tenantAliasesCitadelCollegeHost() {
        assertEquals(Tenant.CITADEL_HOST, Tenant.normalize("citadelcollege.magister.net"))
        assertEquals(Tenant.CITADEL_HOST, Tenant.normalize("https://Citadel.Magister.Net/"))
        assertEquals("andereschool.magister.net", Tenant.normalize("andereschool"))
        assertEquals("", Tenant.normalize(" "))
    }

    @Test
    fun authUrlContainsOfficialM6loappScopes() {
        val request = AuthRequestFactory.create("citadel.magister.net", Pkce.generate(), state = "s", nonce = "n")
        assertEquals(
            "openid profile offline_access magister.mobile magister.ecs",
            MagisterEndpoints.SCOPE,
        )
        assertTrue(request.authorizeUrl.contains("magister.mobile"))
        assertTrue(request.authorizeUrl.contains("magister.ecs"))
        assertTrue(request.authorizeUrl.contains("offline_access"))
        assertFalse(request.authorizeUrl.contains("opp.read"))
    }

    @Test
    fun authUrlContainsPkceAndTenant() {
        val pkce = Pkce.generate()
        val request = AuthRequestFactory.create("citadel.magister.net", pkce, state = "state-1", nonce = "nonce-1")
        assertTrue(request.authorizeUrl.startsWith("https://accounts.magister.net/connect/authorize?"))
        assertTrue(request.authorizeUrl.contains("client_id=M6LOAPP"))
        assertTrue(request.authorizeUrl.contains("code_challenge_method=S256"))
        assertTrue(request.authorizeUrl.contains("acr_values=tenant%3Acitadel.magister.net"))
        assertEquals(pkce.verifier, request.codeVerifier)
        assertEquals(Pkce.challenge(pkce.verifier), pkce.challenge)
    }

    @Test
    fun extractCodeFromFragmentRedirect() {
        val url = "m6loapp://oauth2redirect/#code=abc%2Fdef&id_token=xyz&state=s"
        assertEquals("abc/def", AuthRequestFactory.extractCode(url))
        assertEquals("access_denied: user", AuthRequestFactory.extractError("m6loapp://oauth2redirect/?error=access_denied&error_description=user"))
    }
}
