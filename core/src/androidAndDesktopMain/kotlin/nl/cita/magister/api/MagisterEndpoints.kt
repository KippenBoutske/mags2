package nl.cita.magister.api

/**
 * Alle Magister-URLs op één plek. Endpoint-wijzigingen hoeven alleen hier.
 *
 * Bronnen (onofficieel, kunnen wijzigen):
 * - accounts.magister.net OpenID-discovery (geverifieerd 2026-10-21)
 * - MagisterAPIKt / Otarium (M6LOAPP + m6loapp://oauth2redirect/)
 * - Publieke REST-paden gebruikt door bestaande open-source clients
 */
object MagisterEndpoints {
    const val ACCOUNTS_HOST = "https://accounts.magister.net"
    const val AUTHORIZE = "$ACCOUNTS_HOST/connect/authorize"
    const val TOKEN = "$ACCOUNTS_HOST/connect/token"
    const val USERINFO = "$ACCOUNTS_HOST/connect/userinfo"
    const val END_SESSION = "$ACCOUNTS_HOST/connect/endsession"
    const val OPENID_CONFIGURATION = "$ACCOUNTS_HOST/.well-known/openid-configuration"

    /** Officieel-app client-id; Magister accepteert geen eigen public clients. */
    const val CLIENT_ID = "M6LOAPP"
    const val REDIRECT_URI = "m6loapp://oauth2redirect/"
    /**
     * Exacte M6LOAPP-scope uit werkende clients (niet raden).
     * `opp.read` / `grades.read` / `email` gaf 0.1.4 `invalid_scope`.
     * Bron: https://github.com/DiscipulusApp/Discipulus/blob/9e15bafcce451167a3d0eb4b5a49a044b64949bd/lib/api/authentication.dart
     * Zelfde string: Friday magister-core, WalburgApp-V2.
     */
    const val SCOPE = "openid profile offline_access magister.mobile magister.ecs"
    const val RESPONSE_TYPE = "id_token code"
    const val CODE_CHALLENGE_METHOD = "S256"

    /** Tenant-API-root, bijv. https://citadel.magister.net */
    fun tenantRoot(host: String): String = "https://${Tenant.normalize(host)}"

    fun wellKnownHostMeta(): String =
        "https://magister.net/.well-known/host-meta.json?rel=magister-api"

    fun tenantSearch(query: String): String {
        val key = java.net.URLEncoder.encode(query, Charsets.UTF_8).replace("+", "%20")
        return "$ACCOUNTS_HOST/challenges/tenant/search?key=$key"
    }

    fun version(host: String) = "${tenantRoot(host)}/api/versie"

    fun account(root: String) = "$root/api/account"

    fun accountNoCache(root: String) = "$root/api/account?noCache=0"

    fun profile(root: String, personId: Int) = "$root/api/personen/$personId/profiel"

    fun enrollments(root: String, personId: Int) =
        "$root/api/leerlingen/$personId/aanmeldingen?begin=1970-01-01"

    fun enrollmentsFallback(root: String, personId: Int) =
        "$root/api/personen/$personId/aanmeldingen"

    fun gradesOverview(
        root: String,
        personId: Int,
        enrollmentId: Int,
        onlyPta: Boolean = false,
        peildatum: String,
    ) = "$root/api/personen/$personId/aanmeldingen/$enrollmentId/cijfers/" +
        "cijferoverzichtvooraanmelding?actievePerioden=false" +
        "&alleenBerekendeKolommen=false&alleenPTAKolommen=$onlyPta&peildatum=$peildatum"

    fun gradeColumnInfo(root: String, personId: Int, enrollmentId: Int, columnId: Int) =
        "$root/api/personen/$personId/aanmeldingen/$enrollmentId/cijfers/extracijferkolominfo/$columnId"

    fun recentGrades(root: String, personId: Int, top: Int = 50, skip: Int = 0) =
        "$root/api/personen/$personId/cijfers/laatste?top=$top&skip=$skip"

    fun gradePeriods(root: String, personId: Int, enrollmentId: Int) =
        "$root/api/personen/$personId/aanmeldingen/$enrollmentId/cijfers/cijferperiodenvooraanmelding"

    fun appointments(root: String, personId: Int, from: String, to: String, status: Int = 0) =
        "$root/api/personen/$personId/afspraken?status=$status&van=$from&tot=$to"

    fun appointment(root: String, personId: Int, appointmentId: Int) =
        "$root/api/personen/$personId/afspraken/$appointmentId"

    fun assignments(
        root: String,
        personId: Int,
        from: String,
        to: String,
        skip: Int = 0,
        top: Int = 50,
    ) = "$root/api/personen/$personId/opdrachten?skip=$skip&top=$top&startdatum=$from&einddatum=$to"

    fun messageFolders(root: String) = "$root/api/berichten/mappen/alle"

    /**
     * Berichten-fallbackketen. Geen nieuwe scopes.
     *
     * M6LOAPP blijft `openid profile offline_access magister.mobile magister.ecs`.
     * Officiële web-OIDC (`M6-{host}` + `grades.read` e.d.) leest berichten, maar
     * die scopes gaven eerder `invalid_scope` op M6LOAPP. WalburgApp-V2 (2026)
     * haalt de inbox met dezelfde M6LOAPP-scopes via `postvakin`.
     *
     * Inbox (eerst, maplijst is op Citadel 403 met M6LOAPP):
     * 1. `berichten/postvakin/berichten` — officiële web-inbox
     *    (ghuron/skills `magister.mjs`: Angular-app `/magister-berichten/`,
     *    GET `/api/berichten/postvakin/berichten`) + WalburgApp-V2
     *    `src/services/magister.ts` + magistergo `Message.go`
     * 2. `berichten/postvakin` — Discipulus-3DS alias
     * 3. `berichten/mappen/1/berichten` — Discipulus-3DS stap 1, inbox-id 1
     * 4. `berichten/berichten?mapId=` / collectie
     * 5. `personen/{id}/berichten` — oudere magistergo-vorm
     *
     * Maplijst daarna (Discipulus `lib/api/routes/messages.dart` 2026,
     * Otarium/MagisterAPIKt `MessageFlow.kt`, officiële web-folders):
     * `berichten/mappen` en `berichten/mappen/alle`, daarna folder-href.
     */
    fun messageFolderListUrls(root: String): List<String> = listOf(
        "$root/api/berichten/mappen",
        "$root/api/berichten/mappen/alle",
    )

    fun messageInboxUrls(root: String, personId: Int, top: Int = 40, skip: Int = 0): List<String> = listOf(
        "$root/api/berichten/postvakin/berichten?top=$top&skip=$skip",
        "$root/api/berichten/postvakin?top=$top&skip=$skip",
        "$root/api/berichten/mappen/1/berichten?top=$top&skip=$skip",
        "$root/api/berichten/berichten?mapId=1&top=$top&skip=$skip",
        "$root/api/berichten/berichten?top=$top&skip=$skip",
        "$root/api/personen/$personId/berichten?mapId=1&skip=$skip&top=$top",
        "$root/api/berichten/berichten?mapId=7&top=$top&skip=$skip",
        "$root/api/personen/$personId/berichten?mapId=7&skip=$skip&top=$top",
    )

    fun messages(root: String, folderHref: String, top: Int, skip: Int, query: String = "") =
        "$root/${folderHref.trimStart('/')}" +
            (if (folderHref.contains('?')) "&" else "?") +
            "top=$top&skip=$skip&trefwoorden=$query"

    fun message(root: String, href: String) = "$root/${href.trimStart('/')}"

    fun messagesCollection(root: String) = "$root/api/berichten/berichten"

    fun photo(root: String, personId: Int) =
        "$root/api/leerlingen/$personId/foto?redirect_type=body"

    /** Otarium/MagisterAPIKt AbsenceFlow + officiële web (ghuron/skills). */
    fun absences(root: String, personId: Int, from: String, to: String) =
        "$root/api/personen/$personId/absenties?van=$from&tot=$to"

    /** Otarium/MagisterAPIKt StudyGuideFlow. */
    fun studyGuides(root: String, personId: Int, peildatum: String) =
        "$root/api/leerlingen/$personId/studiewijzers?peildatum=$peildatum"

    fun studyProjects(root: String, personId: Int, peildatum: String) =
        "$root/api/leerlingen/$personId/projecten?peildatum=$peildatum"

    fun assignment(root: String, href: String) = "$root/${href.trimStart('/')}"

    fun messageAttachments(root: String, messageId: Int) =
        "$root/api/berichten/berichten/$messageId/bijlagen"
}
