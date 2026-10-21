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
    const val SCOPE = "openid profile email offline_access"
    const val RESPONSE_TYPE = "id_token code"
    const val CODE_CHALLENGE_METHOD = "S256"

    /** Tenant-API-root, bijv. https://citadel.magister.net */
    fun tenantRoot(host: String): String = "https://${Tenant.normalize(host)}"

    fun wellKnownHostMeta(): String =
        "https://magister.net/.well-known/host-meta.json?rel=magister-api"

    fun account(root: String) = "$root/api/account"

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

    fun messages(root: String, folderHref: String, top: Int, skip: Int, query: String = "") =
        "$root/${folderHref.trimStart('/')}" +
            (if (folderHref.contains('?')) "&" else "?") +
            "top=$top&skip=$skip&trefwoorden=$query"

    fun message(root: String, href: String) = "$root/${href.trimStart('/')}"

    fun messagesCollection(root: String) = "$root/api/berichten/berichten"

    fun photo(root: String, personId: Int) =
        "$root/api/leerlingen/$personId/foto?redirect_type=body"
}
