package nl.cita.magister.model

data class ApiFailure(
    val path: String,
    val code: Int,
    val body: String,
) {
    fun friendly(): String = when (code) {
        401 -> "Sessie verlopen. Log opnieuw in."
        403 -> "Magister weigert deze data."
        404 -> "Niet gevonden."
        else -> "Kon niet laden."
    }

    fun summary(): String = "$path HTTP $code"

    fun details(): String = body.trim().ifBlank { "(geen body)" }.take(800)
}

data class SessionHydration(
    val profile: StudentProfile?,
    val warnings: List<ApiFailure> = emptyList(),
)
