package nl.cita.magister.api

/**
 * Normaliseert een Magister-tenant hostname.
 *
 * `citadelcollege.magister.net` resolved niet; Citadel College leeft op
 * `citadel.magister.net`. Die alias blijft als valkuil-correctie, geen default.
 */
object Tenant {
    const val CITADEL_HOST = "citadel.magister.net"
    const val CITADEL_DISPLAY = "Citadel College"

    private val STOP_WORDS = setOf(
        "college", "school", "scholengemeenschap", "lyceum", "gymnasium",
        "voor", "de", "het", "van", "en", "vavo", "sg", "osv", "locatie",
        "campus", "mavo", "havo", "vwo", "roc",
    )

    fun normalize(raw: String): String {
        var value = raw.trim().lowercase()
        value = value.removePrefix("https://").removePrefix("http://")
        value = value.substringBefore('/').substringBefore('?')
        if (value.isBlank()) return ""
        if (!value.contains('.')) {
            value = "$value.magister.net"
        }
        if (value == "citadelcollege.magister.net") {
            return CITADEL_HOST
        }
        return value
    }

    fun looksLikeHost(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || ' ' in trimmed) return false
        if (!trimmed.contains('.') && !trimmed.startsWith("http", ignoreCase = true)) return false
        val host = normalize(trimmed)
        return host.endsWith(".magister.net") && host != ".magister.net"
    }

    fun slugCandidates(displayName: String): List<String> {
        val words = displayName.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 && it !in STOP_WORDS }
        if (words.isEmpty()) return emptyList()
        val first = words.first()
        return listOf(
            first,
            words.take(2).joinToString(""),
            words.joinToString(""),
            "${first}college",
        ).distinct()
    }
}

data class School(
    val name: String,
    val host: String,
    val city: String = "",
    val tenantId: String? = null,
)

object KnownSchools {
    val suggestions: List<School> = listOf(
        School("Citadel College", Tenant.CITADEL_HOST, "Nijmegen / Lent"),
        School("Canisius College", "canisius.magister.net", "Nijmegen"),
        School("Kandinsky College", "kandinsky.magister.net", "Nijmegen"),
        School("NSG Groenewoud", "nsggroenewoud.magister.net", "Nijmegen"),
        School("Stedelijk Gymnasium Nijmegen", "sgnijmegen.magister.net", "Nijmegen"),
        School("SSGN", "ssgn.magister.net", "Nijmegen"),
        School("Mondial College", "mondial.magister.net", "Nijmegen"),
        School("Pax Christi College", "paxchristicollege.magister.net", "Druten"),
        School("Maaswaal College", "maaswaalcollege.magister.net", "Wijchen"),
    )

    fun search(query: String): List<School> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        val hits = suggestions.filter {
            it.name.lowercase().contains(q) ||
                it.host.contains(q) ||
                it.city.lowercase().contains(q)
        }
        return hits.ifEmpty {
            if (Tenant.looksLikeHost(query)) {
                listOf(School(query.trim(), Tenant.normalize(query)))
            } else {
                emptyList()
            }
        }
    }
}
