package nl.cita.magister.api

/**
 * Alle HTTP-headerwaarden die Mags zelf zet. OkHttp weigert non-ASCII (0x80+)
 * in headers — een ë in de User-Agent liet inloggen meteen falen.
 */
object CitaHttp {
    const val USER_AGENT = "Mags/0.2.0 (unofficial Magister client)"
    const val ACCEPT_JSON = "application/json"
    const val CONTENT_TYPE_JSON = "application/json"
    const val API_CLIENT_ID = "M6LOAPP"

    /**
     * Elke statische headerwaarde die de app meestuurt (Magister + Plan Goon).
     * Authorization is dynamisch; zie [bearer].
     */
    val staticHeaderValues: List<String> = listOf(
        USER_AGENT,
        ACCEPT_JSON,
        CONTENT_TYPE_JSON,
        API_CLIENT_ID,
    )

    val staticHeaders: List<Pair<String, String>> = listOf(
        "User-Agent" to USER_AGENT,
        "Accept" to ACCEPT_JSON,
        "Content-Type" to CONTENT_TYPE_JSON,
        "X-API-Client-ID" to API_CLIENT_ID,
    )

    fun bearer(token: String): String = "Bearer ${requireAscii("Authorization", token)}"

    fun requireAscii(headerName: String, value: String): String {
        value.forEachIndexed { index, ch ->
            val code = ch.code
            if (code < 0x20 || code > 0x7E) {
                throw IllegalArgumentException(
                    "Non-ASCII 0x${code.toString(16)} at $index in $headerName value: $value",
                )
            }
        }
        return value
    }

    fun isAscii(value: String): Boolean = value.all { it.code in 0x20..0x7E }
}
