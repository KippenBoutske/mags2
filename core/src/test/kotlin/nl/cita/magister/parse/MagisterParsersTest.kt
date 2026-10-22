package nl.cita.magister.parse

import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.api.Tenant
import nl.cita.magister.auth.AuthRequestFactory
import nl.cita.magister.auth.Pkce
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.model.GradeColumnType
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.InfoType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals(12, grades.size)
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
    fun seCijferWithKolomSoort1IsAverageNotAGrade() {
        val grades = MagisterParsers.parseGrades(MockFixtures.citadelGradesJson)
        val se = grades.filter { it.columnHeading == "SE cijfer" }
        assertTrue(se.isNotEmpty())
        assertTrue(se.all { it.columnType == GradeColumnType.Average })
        assertTrue(se.all { !it.counts })
        val klassenwerk = grades.first { it.id == 1201 }
        assertEquals(GradeColumnType.Grade, klassenwerk.columnType)
        assertNull(klassenwerk.weight)
        val v = grades.first { it.value == "V" }
        assertEquals(GradeColumnType.Grade, v.columnType)
        assertFalse(v.counts)
        assertNull(v.numericValue)
    }

    @Test
    fun parseRecentGradesKeepsTestsAndDropsGemSe() {
        val parsed = MagisterParsers.parseRecentGrades(MockFixtures.recentGradesJson)
        assertEquals(5, parsed.size)
        assertEquals(GradeColumnType.Grade, parsed.first { it.columnHeading == "PW2" }.columnType)
        assertEquals(GradeColumnType.Average, parsed.first { it.columnHeading == "Voortschrijdend gemiddelde" }.columnType)
        assertEquals(GradeColumnType.Average, parsed.first { it.columnHeading == "SE cijfer" }.columnType)
        val latest = nl.cita.magister.grades.GradeCalculator.latestEarnedGrades(parsed)
        assertEquals(listOf("5,3", "7,6", "6,8"), latest.map { it.value })
        assertEquals(listOf("PW2", "Writing", "Literatuur"), latest.map { it.columnHeading })
        assertEquals(listOf("Wiskunde B", "Engels", "Nederlands"), latest.map { it.subjectName })
        assertEquals(3.0, latest.first().weight!!, 0.0001)
        assertEquals("2025-10-10T10:00:00.0000000Z", latest.first().enteredAt)
    }

    @Test
    fun extraColumnInfoFillsWeightWithoutClobberingHeading() {
        val parsed = MagisterParsers.parseGrades(MockFixtures.citadelGradesJson)
        val extra = mapOf(201 to MagisterParsers.parseColumnInfo(MockFixtures.extraColumnInfoJson(201)))
        val applied = MagisterParsers.applyColumnInfo(parsed, extra)
        val klassenwerk = applied.first { it.id == 1201 }
        assertEquals(2.0, klassenwerk.weight!!, 0.0001)
        assertEquals("Klassenwerk", klassenwerk.columnHeading)
        assertTrue(klassenwerk.counts)
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
    fun parseAbsencesGuidesAssignmentsAndAttachments() {
        val absences = MagisterParsers.parseAbsences(MockFixtures.absencesJson)
        assertEquals(2, absences.size)
        assertTrue(absences.first().excused)
        assertEquals("Nederlands", absences.first().lesson)
        val guides = MagisterParsers.parseStudyGuides(MockFixtures.studyGuidesJson)
        assertEquals(2, guides.size)
        assertTrue(guides.any { it.title.contains("Wiskunde") })
        val work = MagisterParsers.parseAssignments(MockFixtures.assignmentsJson)
        assertEquals(2, work.size)
        assertEquals(HomeworkItem.Source.Assignment, work.first().source)
        val files = MagisterParsers.parseAttachments(MockFixtures.attachmentsJson)
        assertEquals("PWS-sjabloon.docx", files.single().name)
    }

    @Test
    fun parseMessages() {
        val messages = MagisterParsers.parseMessages(MockFixtures.messagesJson)
        assertEquals(3, messages.size)
        assertEquals(2, messages.count { !it.isRead })
        val detail = MockFixtures.message(9001)!!
        assertTrue(detail.bodyHtml!!.contains("PWS-plan"))
        assertEquals("Deadline profielwerkstuk", detail.preview.subject)
        assertEquals("PWS-sjabloon.docx", detail.attachments.single().name)
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
