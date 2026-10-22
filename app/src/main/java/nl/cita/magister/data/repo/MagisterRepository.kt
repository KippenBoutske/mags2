package nl.cita.magister.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import nl.cita.magister.api.KnownSchools
import nl.cita.magister.api.MagisterEndpoints
import nl.cita.magister.api.School
import nl.cita.magister.api.Tenant
import nl.cita.magister.auth.IdTokenClaims
import nl.cita.magister.auth.SecretStore
import nl.cita.magister.auth.StoredSession
import nl.cita.magister.auth.TokenStore
import nl.cita.magister.data.api.MagisterApiException
import nl.cita.magister.data.api.MagisterHttpClient
import nl.cita.magister.data.api.PlanGoonClient
import nl.cita.magister.data.db.CacheBlob
import nl.cita.magister.data.db.CitaDatabase
import nl.cita.magister.data.db.LocalHomeworkState
import nl.cita.magister.data.prefs.UserPrefs
import nl.cita.magister.export.GradeExport
import nl.cita.magister.export.GradeExportPayload
import nl.cita.magister.dto.SendMessageRequest
import nl.cita.magister.dto.ReceiverDto
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

class MagisterRepository(
    private val prefs: UserPrefs,
    private val tokenStore: TokenStore,
    private val secrets: SecretStore,
    private val http: MagisterHttpClient,
    private val planGoon: PlanGoonClient,
    private val database: CitaDatabase,
) {
    private val cache get() = database.cacheDao()
    private val homeworkDao get() = database.homeworkDao()

    fun session(): StoredSession? = tokenStore.read()
    fun isLoggedIn(): Boolean = tokenStore.read() != null

    suspend fun isDemo(): Boolean = prefs.isDemo()

    suspend fun cachedGrades(): List<Grade> =
        cache.get(KEY_GRADES)?.json?.let {
            MagisterJson.instance.decodeFromString(ListSerializerGrade, it)
        }.orEmpty()

    suspend fun cachedAppointments(): List<Appointment> =
        cache.get(KEY_APPOINTMENTS)?.json?.let {
            MagisterJson.instance.decodeFromString(ListSerializerAppointment, it)
        }.orEmpty()

    fun observeProfile(): Flow<StudentProfile?> = cache.observe(KEY_PROFILE).map { blob ->
        blob?.json?.let { MagisterJson.instance.decodeFromString(StudentProfile.serializer(), it) }
    }

    fun observeGrades(): Flow<List<Grade>> = cache.observe(KEY_GRADES).map { blob ->
        blob?.json?.let { MagisterJson.instance.decodeFromString(ListSerializerGrade, it) } ?: emptyList()
    }

    fun observeAverages(): Flow<List<SubjectAverage>> = observeGrades().map { GradeCalculator.averagesBySubject(it) }

    fun observeAppointments(): Flow<List<Appointment>> = cache.observe(KEY_APPOINTMENTS).map { blob ->
        blob?.json?.let { MagisterJson.instance.decodeFromString(ListSerializerAppointment, it) } ?: emptyList()
    }

    fun observeHomework(): Flow<List<HomeworkItem>> = combine(
        cache.observe(KEY_HOMEWORK),
        homeworkDao.observeAll(),
    ) { blob, local ->
        val items = blob?.json?.let { MagisterJson.instance.decodeFromString(ListSerializerHomework, it) }
            ?: emptyList()
        val overrides = local.associate { it.homeworkId to it.done }
        items.map { item -> item.copy(isDone = overrides[item.id] ?: item.isDone) }
    }

    fun observeMessages(): Flow<List<MessagePreview>> = cache.observe(KEY_MESSAGES).map { blob ->
        blob?.json?.let { MagisterJson.instance.decodeFromString(ListSerializerMessage, it) } ?: emptyList()
    }

    fun observeAbsences(): Flow<List<Absence>> = cache.observe(KEY_ABSENCES).map { blob ->
        blob?.json?.let { MagisterJson.instance.decodeFromString(ListSerializerAbsence, it) } ?: emptyList()
    }

    fun observeStudyGuides(): Flow<List<StudyGuide>> = cache.observe(KEY_GUIDES).map { blob ->
        blob?.json?.let { MagisterJson.instance.decodeFromString(ListSerializerGuide, it) } ?: emptyList()
    }

    fun observeAssignments(): Flow<List<HomeworkItem>> = cache.observe(KEY_ASSIGNMENTS).map { blob ->
        blob?.json?.let { MagisterJson.instance.decodeFromString(ListSerializerHomework, it) } ?: emptyList()
    }

    suspend fun completeLogin(code: String, verifier: String, tenantHost: String): SessionHydration {
        val tokens = http.exchangeCode(code, verifier)
        val fromToken = tokens.idToken?.let { IdTokenClaims.tenantHost(it) }.orEmpty()
        val host = listOf(fromToken, Tenant.normalize(tenantHost))
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val school = prefs.schoolNameNow().ifBlank { schoolNameFor(host) }
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
            prefs.setTenant(host, school, prefs.tenantIdNow().ifBlank { null })
        }
        return refreshAll()
    }

    suspend fun searchSchools(query: String): List<School> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
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

    private var demoFixture: DemoFixture = DemoFixture.Default

    suspend fun enterDemo(fixture: DemoFixture = DemoFixture.Default) {
        demoFixture = fixture
        prefs.setDemo(true)
        prefs.setTenant(nl.cita.magister.api.Tenant.CITADEL_HOST, nl.cita.magister.api.Tenant.CITADEL_DISPLAY, null)
        tokenStore.clear()
        writeCache(KEY_PROFILE, MagisterJson.instance.encodeToString(MockFixtures.profile()))
        writeCache(KEY_GRADES, MagisterJson.instance.encodeToString(MockFixtures.grades(fixture)))
        writeCache(KEY_APPOINTMENTS, MagisterJson.instance.encodeToString(MockFixtures.appointments(fixture)))
        writeCache(KEY_HOMEWORK, MagisterJson.instance.encodeToString(MockFixtures.homework(fixture)))
        writeCache(KEY_MESSAGES, MagisterJson.instance.encodeToString(MockFixtures.messages()))
        writeCache(KEY_ABSENCES, MagisterJson.instance.encodeToString(MockFixtures.absences()))
        writeCache(KEY_GUIDES, MagisterJson.instance.encodeToString(MockFixtures.studyGuides()))
        writeCache(KEY_ASSIGNMENTS, MagisterJson.instance.encodeToString(MockFixtures.assignments()))
    }

    suspend fun logout() {
        tokenStore.clear()
        prefs.setDemo(false)
        cache.clear()
    }

    suspend fun refreshAll(): SessionHydration {
        if (prefs.isDemo() || tokenStore.read() == null) {
            enterDemo(demoFixture)
            return SessionHydration(MockFixtures.profile())
        }
        val session = tokenStore.read()!!
        val warnings = mutableListOf<ApiFailure>()
        val host = session.tenantHost.ifBlank { prefs.tenantHostNow() }
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
                schoolName = prefs.schoolNameNow().ifBlank { schoolNameFor(host) },
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
            writeCache(KEY_PROFILE, MagisterJson.instance.encodeToString(profile))
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
        if (error is MagisterApiException) {
            ApiFailure(error.path, error.code, error.body)
        } else {
            ApiFailure(
                MagisterApiException.shortPath(fallbackUrl),
                0,
                error.message ?: error.toString(),
            )
        }

    private suspend fun profileFromToken(session: StoredSession, host: String = session.tenantHost): StudentProfile? {
        val token = session.idToken ?: return null
        val first = IdTokenClaims.givenName(token) ?: return null
        return StudentProfile(
            personId = session.personId ?: 0,
            firstName = first,
            lastName = IdTokenClaims.familyName(token).orEmpty(),
            className = null,
            study = null,
            enrollmentId = session.enrollmentId,
            schoolName = prefs.schoolNameNow().ifBlank { schoolNameFor(host) },
            tenantHost = host,
        )
    }

    suspend fun refreshGrades(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        val enrollmentId = resolved?.enrollmentId
        if (prefs.isDemo() || resolved == null || enrollmentId == null) {
            writeCache(KEY_GRADES, MagisterJson.instance.encodeToString(MockFixtures.grades()))
            return
        }
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val peildatum = LocalDate.now().toString()
        val json = try {
            http.get(MagisterEndpoints.gradesOverview(root, resolved.personId, enrollmentId, false, peildatum))
        } catch (error: Throwable) {
            val forbidden = error is MagisterApiException && error.code in listOf(403, 404)
            if (!forbidden) throw error
            http.get(MagisterEndpoints.recentGrades(root, resolved.personId))
        }
        val parsed = runCatching { MagisterParsers.parseGrades(json) }.getOrElse {
            MagisterParsers.parseRecentGrades(json)
        }
        writeCache(KEY_GRADES, MagisterJson.instance.encodeToString(parsed))
    }

    suspend fun refreshAgenda(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        if (prefs.isDemo() || resolved == null) {
            writeCache(KEY_APPOINTMENTS, MagisterJson.instance.encodeToString(MockFixtures.appointments()))
            writeCache(KEY_HOMEWORK, MagisterJson.instance.encodeToString(MockFixtures.homework()))
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
        writeCache(KEY_APPOINTMENTS, MagisterJson.instance.encodeToString(appointments))
        val homework = MagisterParsers.homeworkFromAppointments(appointments).toMutableList()
        runCatching { refreshAssignments(resolved) }
            .onSuccess { homework += cachedAssignments() }
        writeCache(KEY_HOMEWORK, MagisterJson.instance.encodeToString(homework.distinctBy { it.id }))
    }

    /** Fetch a window around a visible week and merge it into the cached agenda. */
    suspend fun prefetchAgendaAround(monday: LocalDate) {
        val resolved = cachedProfile()
        if (prefs.isDemo() || resolved == null) return
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val from = monday.minusWeeks(1)
        val to = monday.plusWeeks(2)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val json = http.get(
            MagisterEndpoints.appointments(root, resolved.personId, from.format(fmt), to.format(fmt)),
        )
        val incoming = MagisterParsers.parseAppointments(json)
        val existing = cache.get(KEY_APPOINTMENTS)?.json?.let {
            MagisterJson.instance.decodeFromString(ListSerializerAppointment, it)
        } ?: emptyList()
        val merged = (existing.associateBy { it.id } + incoming.associateBy { it.id })
            .values
            .sortedBy { it.startIso }
        writeCache(KEY_APPOINTMENTS, MagisterJson.instance.encodeToString(merged))
        val extraHomework = MagisterParsers.homeworkFromAppointments(incoming)
        val existingHomework = cache.get(KEY_HOMEWORK)?.json?.let {
            MagisterJson.instance.decodeFromString(ListSerializerHomework, it)
        } ?: emptyList()
        val homework = (existingHomework.associateBy { it.id } + extraHomework.associateBy { it.id })
            .values
            .sortedBy { it.dueIso }
        writeCache(KEY_HOMEWORK, MagisterJson.instance.encodeToString(homework))
    }

    suspend fun refreshMessages(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        if (prefs.isDemo() || resolved == null) {
            writeCache(KEY_MESSAGES, MagisterJson.instance.encodeToString(MockFixtures.messages()))
            return
        }
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val personId = resolved.personId
        var lastError: Throwable? = null

        // Official web + Walburg inbox does not need folder listing (Citadel 403 on /alle).
        for (url in MagisterEndpoints.messageInboxUrls(root, personId)) {
            try {
                val json = http.get(url)
                val parsed = MagisterParsers.parseMessages(json)
                writeCache(KEY_MESSAGES, MagisterJson.instance.encodeToString(parsed))
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
                    writeCache(KEY_MESSAGES, MagisterJson.instance.encodeToString(MagisterParsers.parseMessages(json)))
                    return
                }
            } catch (error: Throwable) {
                lastError = error
            }
        }
        writeCache(KEY_MESSAGES, MagisterJson.instance.encodeToString(emptyList<MessagePreview>()))
        throw lastError ?: MagisterApiException(403, "$root/api/berichten", "Geen toegang tot berichten.")
    }

    suspend fun messageDetail(id: Int): MessageDetail {
        if (prefs.isDemo()) return MockFixtures.message(id) ?: error("Bericht $id ontbreekt in mock")
        val session = tokenStore.read() ?: error("Niet ingelogd")
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val cached = cache.get(KEY_MESSAGES)?.json
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
        if (prefs.isDemo() || resolved == null) {
            writeCache(KEY_ABSENCES, MagisterJson.instance.encodeToString(MockFixtures.absences()))
            return
        }
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val from = LocalDate.now().minusMonths(4)
        val to = LocalDate.now().plusWeeks(1)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val json = http.get(MagisterEndpoints.absences(root, resolved.personId, from.format(fmt), to.format(fmt)))
        writeCache(KEY_ABSENCES, MagisterJson.instance.encodeToString(MagisterParsers.parseAbsences(json)))
    }

    suspend fun refreshStudyGuides(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        if (prefs.isDemo() || resolved == null) {
            writeCache(KEY_GUIDES, MagisterJson.instance.encodeToString(MockFixtures.studyGuides()))
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
        writeCache(KEY_GUIDES, MagisterJson.instance.encodeToString((guides + projects).sortedBy { it.title }))
        if (guides.isEmpty() && projects.isEmpty() && last != null) throw last!!
    }

    suspend fun refreshAssignments(profile: StudentProfile? = null) {
        val resolved = profile ?: cachedProfile()
        if (prefs.isDemo() || resolved == null) {
            writeCache(KEY_ASSIGNMENTS, MagisterJson.instance.encodeToString(MockFixtures.assignments()))
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
        writeCache(KEY_ASSIGNMENTS, MagisterJson.instance.encodeToString(MagisterParsers.parseAssignments(json)))
    }

    private suspend fun cachedAssignments(): List<HomeworkItem> =
        cache.get(KEY_ASSIGNMENTS)?.json?.let {
            MagisterJson.instance.decodeFromString(ListSerializerHomework, it)
        } ?: emptyList()

    suspend fun markHomeworkDone(item: HomeworkItem, done: Boolean) {
        homeworkDao.upsert(LocalHomeworkState(item.id, done))
        val appointmentId = item.appointmentId
        if (prefs.isDemo() || appointmentId == null) return
        val session = tokenStore.read() ?: return
        val profile = cachedProfile() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val body = """{"Id":$appointmentId,"Afgerond":$done}"""
        runCatching {
            http.putJson(MagisterEndpoints.appointment(root, profile.personId, appointmentId), body)
        }
    }

    suspend fun markMessageRead(id: Int, read: Boolean = true) {
        if (prefs.isDemo()) return
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val body = """{"berichten":[{"berichtId":$id,"operations":[{"op":"replace","path":"/isGelezen","value":$read}]}]}"""
        http.patchJson(MagisterEndpoints.messagesCollection(root), body)
        refreshMessages()
    }

    fun webhookConfigured(): Boolean =
        secrets.hasWebhookKey()

    suspend fun webhookUrl(): String = prefs.webhookUrlNow()

    suspend fun saveWebhook(url: String, key: String?) {
        prefs.setWebhookUrl(url)
        if (key != null) secrets.setWebhookKey(key)
    }

    suspend fun exportPayload(): GradeExportPayload {
        val profile = cachedProfile() ?: MockFixtures.profile()
        val grades = cache.get(KEY_GRADES)?.json?.let {
            MagisterJson.instance.decodeFromString(ListSerializerGrade, it)
        } ?: MockFixtures.grades()
        return GradeExport.build(profile, grades)
    }

    suspend fun exportJson(pretty: Boolean = true): String {
        val json = if (pretty) {
            kotlinx.serialization.json.Json { prettyPrint = true; encodeDefaults = true }
        } else {
            MagisterJson.instance
        }
        return json.encodeToString(GradeExportPayload.serializer(), exportPayload())
    }

    suspend fun exportToPlanGoon(): Result<Int> {
        val url = prefs.webhookUrlNow()
        val key = secrets.webhookKey()
        if (url.isBlank()) {
            return Result.failure(IllegalStateException("geen-adres"))
        }
        val body = exportJson(pretty = false)
        return planGoon.post(url, key.orEmpty(), body)
    }

    suspend fun sendMessage(subject: String, body: String, recipientId: Int) {
        if (prefs.isDemo()) return
        val session = tokenStore.read() ?: return
        val root = MagisterEndpoints.tenantRoot(session.tenantHost)
        val payload = MagisterJson.instance.encodeToString(
            SendMessageRequest(
                to = listOf(ReceiverDto(recipientId)),
                body = body,
                subject = subject,
            ),
        )
        http.postJson(MagisterEndpoints.messagesCollection(root), payload)
        refreshMessages()
    }

    private suspend fun cachedProfile(): StudentProfile? =
        cache.get(KEY_PROFILE)?.json?.let {
            MagisterJson.instance.decodeFromString(StudentProfile.serializer(), it)
        }

    private suspend fun writeCache(key: String, json: String) {
        cache.put(CacheBlob(key, json, System.currentTimeMillis()))
    }

    private fun schoolNameFor(host: String): String =
        nl.cita.magister.api.KnownSchools.suggestions.firstOrNull { it.host == host }?.name ?: host

    private companion object {
        const val KEY_PROFILE = "profile"
        const val KEY_GRADES = "grades"
        const val KEY_APPOINTMENTS = "appointments"
        const val KEY_HOMEWORK = "homework"
        const val KEY_MESSAGES = "messages"
        const val KEY_ABSENCES = "absences"
        const val KEY_GUIDES = "studyguides"
        const val KEY_ASSIGNMENTS = "assignments"

        val ListSerializerGrade = kotlinx.serialization.builtins.ListSerializer(Grade.serializer())
        val ListSerializerAppointment = kotlinx.serialization.builtins.ListSerializer(Appointment.serializer())
        val ListSerializerHomework = kotlinx.serialization.builtins.ListSerializer(HomeworkItem.serializer())
        val ListSerializerMessage = kotlinx.serialization.builtins.ListSerializer(MessagePreview.serializer())
        val ListSerializerAbsence = kotlinx.serialization.builtins.ListSerializer(Absence.serializer())
        val ListSerializerGuide = kotlinx.serialization.builtins.ListSerializer(StudyGuide.serializer())
    }
}
