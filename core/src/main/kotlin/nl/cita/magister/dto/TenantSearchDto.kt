package nl.cita.magister.dto

import kotlinx.serialization.Serializable

@Serializable
data class TenantSearchHit(
    val displayName: String,
    val id: String,
)
