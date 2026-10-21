package nl.cita.magister.api

/**
 * Normaliseert een Magister-tenant hostname.
 *
 * Geverifieerd 2026-10-21: `citadelcollege.magister.net` resolved niet.
 * De live tenant van Citadel College (Lent/Nijmegen) is `citadel.magister.net`.
 */
object Tenant {
    const val CITADEL_HOST = "citadel.magister.net"
    const val CITADEL_DISPLAY = "Citadel College"

    fun normalize(raw: String): String {
        var value = raw.trim().lowercase()
        value = value.removePrefix("https://").removePrefix("http://")
        value = value.substringBefore('/').substringBefore('?')
        if (value.isBlank()) return CITADEL_HOST
        if (!value.contains('.')) {
            value = "$value.magister.net"
        }
        if (value == "citadelcollege.magister.net") {
            return CITADEL_HOST
        }
        return value
    }

    fun looksLikeHost(raw: String): Boolean {
        val host = normalize(raw)
        return host.endsWith(".magister.net") && host != ".magister.net"
    }
}

data class School(
    val name: String,
    val host: String,
    val city: String,
)

object KnownSchools {
    val citadel = School(Tenant.CITADEL_DISPLAY, Tenant.CITADEL_HOST, "Nijmegen / Lent")

    val suggestions: List<School> = listOf(
        citadel,
        School("Canisius College", "canisius.magister.net", "Nijmegen"),
        School("Kandinsky College", "kandinsky.magister.net", "Nijmegen"),
        School("NSG Groenewoud", "nsg.magister.net", "Nijmegen"),
        School("Stedelijk Gymnasium Nijmegen", "sgnijmegen.magister.net", "Nijmegen"),
        School("SSGN", "ssgn.magister.net", "Nijmegen"),
        School("Mondial College", "mondial.magister.net", "Nijmegen"),
        School("Pax Christi College", "paxchristicollege.magister.net", "Druten"),
        School("Maaswaal College", "maaswaalcollege.magister.net", "Wijchen"),
    )

    fun search(query: String): List<School> {
        val q = query.trim().lowercase()
        if (q.length < 2) return listOf(citadel)
        return suggestions.filter {
            it.name.lowercase().contains(q) ||
                it.host.contains(q) ||
                it.city.lowercase().contains(q)
        }.ifEmpty {
            if (Tenant.looksLikeHost(query)) {
                listOf(School(query, Tenant.normalize(query), ""))
            } else {
                emptyList()
            }
        }
    }
}
