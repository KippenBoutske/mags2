package nl.cita.magister.desktop.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.cita.magister.api.KnownSchools
import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.api.School
import nl.cita.magister.api.Tenant
import nl.cita.magister.auth.IdTokenClaims
import nl.cita.magister.auth.StoredSession
import nl.cita.magister.dto.GradeColumnInfoDto
import nl.cita.magister.export.GradeExport
import nl.cita.magister.export.GradeExportPayload
import nl.cita.magister.fixtures.DemoFixture
import nl.cita.magister.fixtures.MockFixtures
import nl.cita.magister.fixtures.appointments
import nl.cita.magister.fixtures.grades
import nl.cita.magister.fixtures.homework
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.model.Absence
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.MessageDetail
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.model.SessionHydration
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.StudyGuide
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.parse.MagisterJson
import nl.cita.magister.parse.MagisterParsers
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DesktopRepository(
    val prefs: DesktopPrefs,
    private val tokenStore: DesktopTokenStore,
    private val http: DesktopHttpClient,
    private val planGoon: DesktopPlanGoonClient,
    private val cache: DesktopCache,
) {
    private var demoFixture: DemoFixture = DemoFixture.Default

    fun session(): StoredSession? = tokenStore.read()
    fun isLoggedIn(): Boolean = tokenStore.read() != null
    fun isDemo(): Boolean = prefs.current.isDemo

    fun observeProfile(): Flow<StudentProfile?> = cache.observe(KEY_PROFILE).map { raw ->
        raw?.let { MagisterJson.instance.decodeFromString(StudentProfile.serializer(), it) }
    }

    fun observeGrades(): Flow<List<Grade>> = cache.observe(KEY_GRADES).map { raw ->
        raw?.let { MagisterJson.instance.decodeFromString(ListSerializerGrade, it) } ?: emptyList()
    }

    fun observeAverages(): Flow<List<SubjectAverage>> = observeGrades().map {
        GradeCalculator.averagesBySubject(it)
    }

    fun observeLatestGrades(): Flow<List<Grade>> = combine(
        cache.observe(KEY_LATEST),
        observeGrades(),
    ) { raw, grades ->
        val stored = raw?.let {
            MagisterJson.instance.decodeFromString(ListSerializerGrade, it)
        }.orEmpty()
        GradeCalculator.latestEarnedGrades(stored.ifEmpty { grades }, limit = 8)
    }

    fun observeAppointments(): Flow<List<Appointment>> = cache.observe(KEY_APPOINTMENTS).map { raw ->
        raw?.let { MagisterJson.instance.decodeFromString(ListSerializerAppointment, it) } ?: emptyList()
    }

    fun observeHomework(): Flow<List<HomeworkItem>> = combine(
        cache.observe(KEY_HOMEWORK),
        cache.observeHomeworkOverrides(),
    ) { raw, overrides ->
        val items = raw?.let { MagisterJson.instance.decodeFromString(ListSerializerHomework, it) }
            ?: emptyList()
        items.map { item -> item.copy(isDone = overrides[item.id] ?: item.isDone) }
    }

    fun observeMessages(): Flow<List<MessagePreview>> = cache.observe(KEY_MESSAGES).map { raw ->
        raw?.let { MagisterJson.instance.decodeFromString(ListSerializerMessage, it) } ?: emptyList()
    }

    fun observeAbsences(): Flow<List<Absence>> = cache.observe(KEY_ABSENCES).map { raw ->
        raw?.let { MagisterJson.instance.decodeFromString(ListSerializerAbsence, it) } ?: emptyList()
    }

    fun observeStudyGuides(): Flow<List<StudyGuide>> = cache.observe(KEY_GUIDES).map { raw ->
        raw?.let { MagisterJson.instance.decodeFromString(ListSerializerGuide, it) } ?: emptyList()
    }

    fun observeAssignments(): Flow<List<HomeworkItem>> = cache.observe(KEY_ASSIGNMENTS).map { raw ->
        raw?.let { MagisterJson.instance.decodeFromString(ListSerializerHomework, it) } ?: emptyList()
    }

    suspend fun searchSchools(query: String): List<School> {
        val q = query.trim()
        if (q.length < 2) return KnownSchools.suggestions
        val remote = runCatching { http.searchTenants(q) }.getOrDefault(emptyList())
        val resolved = remote.map { hit ->
            val host = resolveHost(hit.displayName, q)
            School(
                name = hit.displayName,
                host = host.orEmpty(),
                tenantId = hit.id,
            )
        }
        val local = KnownSchools.search(q)
        return (resolved + local)
            .distinctBy { it.tenantId ?: it.host.ifBlank { it.name.lowercase() } }
            .take(8)
    }

    private suspend fun resolveHost(displayName: String, query: String): String? {
        if (Tenant.looksLikeHost(query)) return Tenant.normalize(query)
        val guessed = Tenant.slugCandidates(displayName) + Tenant.slugCandidates(query)
        for (slug in guessed.distinct()) {
            val host = "$slug.magister.net"
            if (http.tenantAlive(host)) return host
        }
        return KnownSchools.suggestions.firstOrNull {
            it.name.equals(displayName, ignoreCase = true)
        }?.host
    }

    suspend fun completeLogin(code: String, verifier: String, tenantHost: String): SessionHydration {
        val tokens = http.exchangeCode(code, verifier)
        val fromToken = tokens.idToken?.let { IdTokenClaims.tenantHost(it) }.orEmpty()
        val host = listOf(fromToken, Tenant.normalize(tenantHost))
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val school = prefs.current.schoolName.ifBlank { schoolNameFor(host) }
        tokenStore.write(
            StoredSession(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                idToken = tokens.idToken,
                expiresAtEpochMs = System.currentTimeMillis() + tokens.expiresIn * 1000L,
                tenantHost = host,
                personId = null,
                enrollmentId = null,
                displayName = tokens.idToken?.let { IdTokenClaims.givenName(it) },
            ),
        )
        prefs.setDemo(false)
        if (host.isNotBlank()) {
            prefs.setTenant(host, school, prefs.current.tenantId.ifBlank { null })
        }
        return refreshAll()
    }

    fun enterDemo(fixture: DemoFixture = DemoFixture.Default) {
        demoFixture = fixture
        prefs.setDemo(true)
        prefs.setTenant(Tenant.CITADEL_HOST, Tenant.CITADEL_DISPLAY, null)
        tokenStore.clear()
        cache.put(KEY_PROFILE, MagisterJson.instance.encodeToString(MockFixtures.profile()))
        val demoGrades = MockFixtures.grades(fixture)
        cache.put(KEY_GRADES, MagisterJson.instance.encodeToString(demoGrades))
        val demoLatest = if (fixture == DemoFixture.NoGrades) {
            emptyList()
        } else {
            GradeCalculator.latestEarnedGrades(
                MagisterParsers.parseRecentGrades(MockFixtures.recentGradesJson).ifEmpty { demoGrades },
                8,
            )
        }
        cache.put(KEY_LATEST, MagisterJson.instance.encodeToString(demoLatest))
        cache.put(KEY_APPOINTMENTS, MagisterJson.instance.encodeToString(MockFixtures.appointments(fixture)))
        cache.put(KEY_HOMEWORK, MagisterJson.instance.encodeToString(MockFixtures.homework(fixture)))
        cache.put(KEY_MESSAGES, MagisterJson.instance.encodeToString(MockFixtures.messages()))
        cache.put(KEY_ABSENCES, MagisterJson.instance.encodeToString(MockFixtures.absences()))
        cache.put(KEY_GUIDES, MagisterJson.instance.encodeToString(MockFixtures.studyGuides()))
        cache.put(KEY_ASSIGNMENTS, MagisterJson.instance.encodeToString(MockFixtures.assignments()))
    }

    fun logout() {
        tokenStore.clear()
        prefs.setDemo(false)
        cache.clear()
    }

    suspend fun refreshAll(): SessionHydration {
        if (prefs.current.isDemo || tokenStore.read() == null) {
            enterDemo(demoFixture)
            return SessionHydration(MockFixtures.profile())
        }
        val session = tokenStore.read()!!
        val warnings = mutableListOf<ApiFailure>()
        val host = session.tenantHost.ifBlank { prefs.current.tenantHost }
        if (host.isBlank()) {
            warnings += ApiFailure("/api/account", 0, "Geen school-host na login.")
            return SessionHydration(profileFromToken(session), warnings)
        }
        val root = MagisterEndpoints.tenantRoot(host)
        val accountJson = firstSuccessful(
            listOf(
                MagisterEndpoints.accountNoCache(root),
                MagisterEndpoints.account(root),
            ),
            warnings,
        )
        val userinfoJson = if (accountJson == null) {
            firstSuccessful(listOf(MagisterEndpoints.USERINFO), warnings)
        } else {
            null
        }
        if (accountJson == null && userinfoJson == null) {
            return SessionHydration(profileFromToken(session), warnings)
        }
        val personId = accountJson?.let { MagisterParsers.parseAccount(it).person?.resolvedId }
        val enrollmentsJson = if (personId != null) {
            firstSuccessful(
                listOf(
                    MagisterEndpoints.enrollments(root, personId),
                    MagisterEndpoints.enrollmentsFallback(root, personId),
                ),
                warnings,
            )
        } else {
            null
        }
        val enrollment = enrollmentsJson?.let {
            MagisterParsers.currentEnrollment(
                MagisterParsers.parseEnrollments(it),
                LocalDate.now().toString(),
            )
        }
        val profile = when {
            accountJson != null -> MagisterParsers.parseProfile(
                json = accountJson,
                tenantHost = host,
                schoolName = prefs.current.schoolName.ifBlank { schoolNameFor(host) },
                enrollment = enrollment,
            )
            else -> profileFromToken(session, host)
        }
        if (profile != null) {
            tokenStore.write(
                session.copy(
                    tenantHost = host,
                    personId = profile.personId.takeIf { it > 0 },
                    enrollmentId = profile.enrollmentId,
                    displayName = listOf(profile.firstName, profile.lastName)
                        .filter { it.isNotBlank() }
                        .joinToString(" "),
                ),
            )
            cache.put(KEY_PROFILE, MagisterJson.instance.encodeToString(profile))
            if (profile.personId > 0) {
                runCatching { refreshGrades(profile) }.onFailure { warnings += failureOf(it, "/cijfers") }
                runCatching { refreshAgenda(profile) }.onFailure { warnings += failureOf(it, "/afspraken") }
                runCatching { refreshMessages(profile) }.onFailure { warnings += failureOf(it, "/berichten") }
                runCatching { refreshAbsences(profile) }.onFailure { warnings += failureOf(it, "/absenties") }
                runCatching { refreshStudyGuides(profile) }.onFailure { warnings += failureOf(it, "/studiewijzers") }
                runCatching { refreshAssignments(profile) }.onFailure { warnings += failureOf(it, "/opdrachten") }
            }
        }
        return SessionHydration(profile, warnings)
    }

    suspend fun refreshGrades(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        val enrollmentId = resolved?.enrollmentId
        if (prefs.current.isDemo || resolved == null || enrollmentId == null) {
            val demo = MockFixtures.grades()
            cache.put(KEY_GRADES, MagisterJson.instance.encodeToString(demo))
            val latest = GradeCalculator.latestEarnedGrades(
                MagisterParsers.parseRecentGrades(MockFixtures.recentGradesJson).ifEmpty { demo },
                8,
            )
            cache.put(KEY_LATEST, MagisterJson.instance.encodeToString(latest))
            return
        }
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val peildatum = LocalDate.now().toString()
        val json = try {
            http.get(MagisterEndpoints.gradesOverview(root, resolved.personId, enrollmentId, false, peildatum))
        } catch (error: Throwable) {
            val forbidden = error is DesktopApiException && error.code in listOf(403, 404)
            if (!forbidden) throw error
            http.get(MagisterEndpoints.recentGrades(root, resolved.personId))
        }
        val parsed = runCatching { MagisterParsers.parseGrades(json) }.getOrElse {
            MagisterParsers.parseRecentGrades(json)
        }
        val extra = fetchColumnInfo(root, resolved.personId, enrollmentId, parsed)
        val enriched = MagisterParsers.applyColumnInfo(parsed, extra)
        cache.put(KEY_GRADES, MagisterJson.instance.encodeToString(enriched))
        val recent = runCatching {
            MagisterParsers.parseRecentGrades(
                http.get(MagisterEndpoints.recentGrades(root, resolved.personId, top = 20)),
            )
        }.getOrDefault(emptyList())
        val recentExtra = fetchColumnInfo(root, resolved.personId, enrollmentId, recent)
        val recentEnriched = MagisterParsers.applyColumnInfo(recent, recentExtra)
        cache.put(
            KEY_LATEST,
            MagisterJson.instance.encodeToString(
                GradeCalculator.latestEarnedGrades(recentEnriched.ifEmpty { enriched }, 8),
            ),
        )
    }

    private suspend fun fetchColumnInfo(
        root: String,
        personId: Int,
        enrollmentId: Int,
        grades: List<Grade>,
    ): Map<Int, GradeColumnInfoDto> {
        val ids = grades.mapNotNull { it.columnId }.distinct()
        if (ids.isEmpty()) return emptyMap()
        val limit = Semaphore(6)
        return coroutineScope {
            ids.map { columnId ->
                async {
                    limit.withPermit {
                        runCatching {
                            val body = http.get(
                                MagisterEndpoints.gradeColumnInfo(root, personId, enrollmentId, columnId),
                            )
                            columnId to MagisterParsers.parseColumnInfo(body)
                        }.getOrNull()
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }

    suspend fun refreshAgenda(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        if (prefs.current.isDemo || resolved == null) {
            cache.put(KEY_APPOINTMENTS, MagisterJson.instance.encodeToString(MockFixtures.appointments()))
            cache.put(KEY_HOMEWORK, MagisterJson.instance.encodeToString(MockFixtures.homework()))
            return
        }
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val from = LocalDate.now().minusWeeks(8)
        val to = LocalDate.now().plusWeeks(8)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val json = http.get(
            MagisterEndpoints.appointments(root, resolved.personId, from.format(fmt), to.format(fmt)),
        )
        val appointments = MagisterParsers.parseAppointments(json)
        cache.put(KEY_APPOINTMENTS, MagisterJson.instance.encodeToString(appointments))
        val homework = MagisterParsers.homeworkFromAppointments(appointments).toMutableList()
        runCatching { refreshAssignments(resolved) }
            .onSuccess { homework += cachedAssignments() }
        cache.put(KEY_HOMEWORK, MagisterJson.instance.encodeToString(homework.distinctBy { it.id }))
    }

    suspend fun prefetchAgendaAround(monday: LocalDate) {
        val resolved = cachedProfile()
        if (prefs.current.isDemo || resolved == null) return
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val from = monday.minusWeeks(1)
        val to = monday.plusWeeks(2)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val json = http.get(
            MagisterEndpoints.appointments(root, resolved.personId, from.format(fmt), to.format(fmt)),
        )
        val incoming = MagisterParsers.parseAppointments(json)
        val existing = cache.get(KEY_APPOINTMENTS)?.let {
            MagisterJson.instance.decodeFromString(ListSerializerAppointment, it)
        } ?: emptyList()
        val merged = (existing.associateBy { it.id } + incoming.associateBy { it.id })
            .values
            .sortedBy { it.startIso }
        cache.put(KEY_APPOINTMENTS, MagisterJson.instance.encodeToString(merged))
        val extraHomework = MagisterParsers.homeworkFromAppointments(incoming)
        val existingHomework = cache.get(KEY_HOMEWORK)?.let {
            MagisterJson.instance.decodeFromString(ListSerializerHomework, it)
        } ?: emptyList()
        val homework = (existingHomework.associateBy { it.id } + extraHomework.associateBy { it.id })
            .values
            .sortedBy { it.dueIso }
        cache.put(KEY_HOMEWORK, MagisterJson.instance.encodeToString(homework))
    }

    suspend fun refreshMessages(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        if (prefs.current.isDemo || resolved == null) {
            cache.put(KEY_MESSAGES, MagisterJson.instance.encodeToString(MockFixtures.messages()))
            return
        }
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        var lastError: Throwable? = null

        for (url in MagisterEndpoints.messageInboxUrls(root, resolved.personId)) {
            try {
                val json = http.get(url)
                cache.put(KEY_MESSAGES, MagisterJson.instance.encodeToString(MagisterParsers.parseMessages(json)))
                return
            } catch (error: Throwable) {
                lastError = error
            }
        }
        for (url in MagisterEndpoints.messageFolderListUrls(root)) {
            try {
                val folders = MagisterParsers.parseFolders(http.get(url))
                val inbox = folders.firstOrNull { folder ->
                    val name = folder.name.orEmpty()
                    name.contains("in", ignoreCase = true) ||
                        name.contains("postvak", ignoreCase = true) ||
                        folder.id == 1
                } ?: folders.firstOrNull()
                val href = inbox?.links?.get("berichten")?.href
                if (!href.isNullOrBlank()) {
                    val json = http.get(MagisterEndpoints.messages(root, href, 40, 0))
                    cache.put(KEY_MESSAGES, MagisterJson.instance.encodeToString(MagisterParsers.parseMessages(json)))
                    return
                }
            } catch (error: Throwable) {
                lastError = error
            }
        }
        cache.put(KEY_MESSAGES, MagisterJson.instance.encodeToString(emptyList<MessagePreview>()))
        throw lastError ?: DesktopApiException(403, "$root/api/berichten", "Geen toegang tot berichten.")
    }

    suspend fun messageDetail(id: Int): MessageDetail {
        if (prefs.current.isDemo) return MockFixtures.message(id) ?: error("Bericht $id ontbreekt in voorbeeld")
        val session = tokenStore.read() ?: error("Niet ingelogd")
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val cached = cache.get(KEY_MESSAGES)
            ?.let { MagisterJson.instance.decodeFromString(ListSerializerMessage, it) }
            ?.firstOrNull { it.id == id }
        val href = cached?.href ?: "api/berichten/berichten/$id"
        val json = http.get(MagisterEndpoints.message(root, href))
        val detail = MagisterParsers.parseMessageDetail(json, cached)
        val attachments = runCatching {
            MagisterParsers.parseAttachments(http.get(MagisterEndpoints.messageAttachments(root, id)))
        }.getOrDefault(emptyList())
        return detail.copy(attachments = attachments)
    }

    suspend fun refreshAbsences(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        if (prefs.current.isDemo || resolved == null) {
            cache.put(KEY_ABSENCES, MagisterJson.instance.encodeToString(MockFixtures.absences()))
            return
        }
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val from = LocalDate.now().minusMonths(4)
        val to = LocalDate.now().plusWeeks(1)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val json = http.get(MagisterEndpoints.absences(root, resolved.personId, from.format(fmt), to.format(fmt)))
        cache.put(KEY_ABSENCES, MagisterJson.instance.encodeToString(MagisterParsers.parseAbsences(json)))
    }

    suspend fun refreshStudyGuides(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        if (prefs.current.isDemo || resolved == null) {
            cache.put(KEY_GUIDES, MagisterJson.instance.encodeToString(MockFixtures.studyGuides()))
            return
        }
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val peildatum = LocalDate.now().toString()
        var last: Throwable? = null
        val guides = runCatching {
            MagisterParsers.parseStudyGuides(http.get(MagisterEndpoints.studyGuides(root, resolved.personId, peildatum)))
        }.onFailure { last = it }.getOrDefault(emptyList())
        val projects = runCatching {
            MagisterParsers.parseStudyGuides(
                http.get(MagisterEndpoints.studyProjects(root, resolved.personId, peildatum)),
                isProject = true,
            )
        }.onFailure { last = it }.getOrDefault(emptyList())
        cache.put(KEY_GUIDES, MagisterJson.instance.encodeToString((guides + projects).sortedBy { it.title }))
        if (guides.isEmpty() && projects.isEmpty() && last != null) throw last!!
    }

    suspend fun refreshAssignments(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        if (prefs.current.isDemo || resolved == null) {
            cache.put(KEY_ASSIGNMENTS, MagisterJson.instance.encodeToString(MockFixtures.assignments()))
            return
        }
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val from = LocalDate.now().minusWeeks(8)
        val to = LocalDate.now().plusWeeks(8)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val json = http.get(
            MagisterEndpoints.assignments(root, resolved.personId, from.format(fmt), to.format(fmt)),
        )
        cache.put(KEY_ASSIGNMENTS, MagisterJson.instance.encodeToString(MagisterParsers.parseAssignments(json)))
    }

    suspend fun markHomeworkDone(item: HomeworkItem, done: Boolean) {
        cache.setHomeworkDone(item.id, done)
        val appointmentId = item.appointmentId
        if (prefs.current.isDemo || appointmentId == null) return
        val session = tokenStore.read() ?: return
        val profile = cachedProfile() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val body = """{"Id":$appointmentId,"Afgerond":$done}"""
        runCatching {
            http.putJson(MagisterEndpoints.appointment(root, profile.personId, appointmentId), body)
        }
    }

    suspend fun markMessageRead(id: Int, read: Boolean = true) {
        if (prefs.current.isDemo) return
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val body = """{"berichten":[{"berichtId":$id,"operations":[{"op":"replace","path":"/isGelezen","value":$read}]}]}"""
        http.patchJson(MagisterEndpoints.messagesCollection(root), body)
        refreshMessages()
    }

    fun exportPayload(): GradeExportPayload {
        val profile = cachedProfile() ?: MockFixtures.profile()
        val grades = cache.get(KEY_GRADES)?.let {
            MagisterJson.instance.decodeFromString(ListSerializerGrade, it)
        } ?: MockFixtures.grades()
        return GradeExport.build(profile, grades)
    }

    fun exportJson(pretty: Boolean = true): String {
        val json = if (pretty) {
            Json { prettyPrint = true; encodeDefaults = true }
        } else {
            MagisterJson.instance
        }
        return json.encodeToString(GradeExportPayload.serializer(), exportPayload())
    }

    suspend fun exportToPlanGoon(): Result<Int> {
        val url = prefs.current.webhookUrl
        val key = prefs.webhookKey()
        if (url.isBlank()) {
            return Result.failure(IllegalStateException("geen-adres"))
        }
        return planGoon.post(url, key.orEmpty(), exportJson(pretty = false))
    }

    private fun cachedProfile(): StudentProfile? =
        cache.get(KEY_PROFILE)?.let {
            MagisterJson.instance.decodeFromString(StudentProfile.serializer(), it)
        }

    private fun cachedAssignments(): List<HomeworkItem> =
        cache.get(KEY_ASSIGNMENTS)?.let {
            MagisterJson.instance.decodeFromString(ListSerializerHomework, it)
        } ?: emptyList()

    private suspend fun firstSuccessful(urls: List<String>, warnings: MutableList<ApiFailure>): String? {
        var last: ApiFailure? = null
        for (url in urls) {
            try {
                return http.get(url)
            } catch (error: Throwable) {
                last = failureOf(error, url)
            }
        }
        last?.let { warnings += it }
        return null
    }

    private fun failureOf(error: Throwable, fallbackUrl: String): ApiFailure =
        if (error is DesktopApiException) {
            ApiFailure(error.path, error.code, error.body)
        } else {
            ApiFailure(
                DesktopApiException.shortPath(fallbackUrl),
                0,
                error.message ?: error.toString(),
            )
        }

    private fun profileFromToken(session: StoredSession, host: String = session.tenantHost): StudentProfile? {
        val token = session.idToken ?: return null
        val first = IdTokenClaims.givenName(token) ?: return null
        return StudentProfile(
            personId = session.personId ?: 0,
            firstName = first,
            lastName = IdTokenClaims.familyName(token).orEmpty(),
            className = null,
            study = null,
            enrollmentId = session.enrollmentId,
            schoolName = prefs.current.schoolName.ifBlank { schoolNameFor(host) },
            tenantHost = host,
        )
    }

    private fun schoolNameFor(host: String): String =
        KnownSchools.suggestions.firstOrNull { it.host == host }?.name ?: host

    private companion object {
        const val KEY_PROFILE = "profile"
        const val KEY_GRADES = "grades"
        const val KEY_LATEST = "grades-latest"
        const val KEY_APPOINTMENTS = "appointments"
        const val KEY_HOMEWORK = "homework"
        const val KEY_MESSAGES = "messages"
        const val KEY_ABSENCES = "absences"
        const val KEY_GUIDES = "studyguides"
        const val KEY_ASSIGNMENTS = "assignments"

        val ListSerializerGrade = ListSerializer(Grade.serializer())
        val ListSerializerAppointment = ListSerializer(Appointment.serializer())
        val ListSerializerHomework = ListSerializer(HomeworkItem.serializer())
        val ListSerializerMessage = ListSerializer(MessagePreview.serializer())
        val ListSerializerAbsence = ListSerializer(Absence.serializer())
        val ListSerializerGuide = ListSerializer(StudyGuide.serializer())
    }
}
