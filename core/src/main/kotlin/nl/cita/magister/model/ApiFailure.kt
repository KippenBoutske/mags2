package nl.cita.magister.model

data class ApiFailure(
    val path: String,
    val code: Int,
    val body: String,
) {
    fun friendly(): String = when {
        code == 401 -> "Sessie verlopen. Log opnieuw in."
        code == 403 && looksLike(MESSAGE_HINTS) ->
            "Berichten zijn voor dit account niet beschikbaar."
        code == 403 && looksLike(GRADE_HINTS) ->
            "Cijfers zijn nu niet beschikbaar."
        code == 403 && looksLike(AGENDA_HINTS) ->
            "Rooster is nu niet beschikbaar."
        code == 403 && looksLike(HOMEWORK_HINTS) ->
            "Huiswerk is nu niet beschikbaar."
        code == 403 -> "Magister weigert deze data."
        code == 404 -> "Niet gevonden."
        else -> "Kon niet laden."
    }

    private fun looksLike(tokens: Array<out String>): Boolean =
        tokens.any { token ->
            path.contains(token, ignoreCase = true) || body.contains(token, ignoreCase = true)
        }

    companion object {
        val GRADE_HINTS = arrayOf("cijfer")
        val AGENDA_HINTS = arrayOf("afspraak", "afspraken", "opdracht", "opdrachten")
        val HOMEWORK_HINTS = arrayOf("opdracht", "opdrachten")
        val MESSAGE_HINTS = arrayOf("bericht", "postvakin", "mappen")
    }

    fun summary(): String = "$path HTTP $code"

    fun details(): String = body.trim().ifBlank { "(geen body)" }.take(800)
}

data class SessionHydration(
    val profile: StudentProfile?,
    val warnings: List<ApiFailure> = emptyList(),
)

data class SectionErrors(
    val grades: ApiFailure? = null,
    val agenda: ApiFailure? = null,
    val messages: ApiFailure? = null,
) {
    companion object {
        fun from(warnings: List<ApiFailure>): SectionErrors = SectionErrors(
            grades = warnings.firstOrNull { warning ->
                ApiFailure.GRADE_HINTS.any { warning.path.contains(it, ignoreCase = true) }
            },
            agenda = warnings.firstOrNull { warning ->
                ApiFailure.AGENDA_HINTS.any { warning.path.contains(it, ignoreCase = true) }
            },
            messages = warnings.firstOrNull { warning ->
                ApiFailure.MESSAGE_HINTS.any { warning.path.contains(it, ignoreCase = true) }
            },
        )
    }
}
