package nl.cita.magister.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AccountDto(
    @SerialName("Persoon") val persoon: PersonDto? = null,
    @SerialName("persoon") val persoonAlt: PersonDto? = null,
) {
    val person: PersonDto? get() = persoon ?: persoonAlt
}

@Serializable
data class PersonDto(
    @SerialName("Id") val id: Int? = null,
    @SerialName("id") val idAlt: Int? = null,
    @SerialName("Roepnaam") val firstName: String? = null,
    @SerialName("roepnaam") val firstNameAlt: String? = null,
    @SerialName("Tussenvoegsel") val infix: String? = null,
    @SerialName("tussenvoegsel") val infixAlt: String? = null,
    @SerialName("Achternaam") val lastName: String? = null,
    @SerialName("achternaam") val lastNameAlt: String? = null,
) {
    val resolvedId: Int? get() = id ?: idAlt
    val resolvedFirst: String get() = firstName ?: firstNameAlt ?: ""
    val resolvedLast: String
        get() = listOfNotNull(infix ?: infixAlt, lastName ?: lastNameAlt)
            .filter { it.isNotBlank() }
            .joinToString(" ")
}

@Serializable
data class ItemsDto<T>(
    @SerialName("Items") val items: List<T>? = null,
    @SerialName("items") val itemsAlt: List<T>? = null,
) {
    val all: List<T> get() = items ?: itemsAlt ?: emptyList()
}

@Serializable
data class EnrollmentDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("Id") val idAlt: Int? = null,
    @SerialName("start") val start: String? = null,
    @SerialName("einde") val end: String? = null,
    @SerialName("groep") val group: NamedDto? = null,
    @SerialName("studie") val study: NamedDto? = null,
) {
    val resolvedId: Int? get() = id ?: idAlt
}

@Serializable
data class NamedDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("omschrijving") val description: String? = null,
    @SerialName("Omschrijving") val descriptionAlt: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("Code") val codeAlt: String? = null,
    @SerialName("Naam") val name: String? = null,
    @SerialName("naam") val nameAlt: String? = null,
) {
    val resolvedName: String? get() = description ?: descriptionAlt ?: name ?: nameAlt
    val resolvedCode: String? get() = code ?: codeAlt
}

@Serializable
data class GradeDto(
    @SerialName("CijferId") val id: Int? = null,
    @SerialName("CijferStr") val value: String? = null,
    @SerialName("IsVoldoende") val isSufficient: Boolean? = null,
    @SerialName("IngevoerdDoor") val enteredBy: String? = null,
    @SerialName("DatumIngevoerd") val enteredAt: String? = null,
    @SerialName("TeltMee") val counts: Boolean? = null,
    @SerialName("Docent") val teacher: String? = null,
    @SerialName("Vak") val subject: SubjectDto? = null,
    @SerialName("CijferKolom") val column: GradeColumnDto? = null,
    @SerialName("CijferPeriode") val period: PeriodDto? = null,
    @SerialName("weegfactor") val weightAlt: Double? = null,
    @SerialName("Weegfactor") val weightPascal: Double? = null,
    @SerialName("Weging") val weightRow: Double? = null,
)

@Serializable
data class SubjectDto(
    @SerialName("Id") val id: Int? = null,
    @SerialName("Afkorting") val code: String? = null,
    @SerialName("Omschrijving") val name: String? = null,
    @SerialName("Naam") val nameAlt: String? = null,
)

@Serializable
data class GradeColumnDto(
    @SerialName("Id") val id: Int? = null,
    @SerialName("KolomNaam") val name: String? = null,
    @SerialName("KolomKop") val heading: String? = null,
    @SerialName("KolomOmschrijving") val description: String? = null,
    @SerialName("KolomSoort") val type: Int? = null,
    @SerialName("IsPTAKolom") val isPta: Boolean? = null,
    @SerialName("Weegfactor") val weight: Double? = null,
    @SerialName("Weging") val weightRow: Double? = null,
    @SerialName("IsHerkansing") val isRetake: Boolean? = null,
    @SerialName("IsHerkansingKolom") val isRetakeColumn: Boolean? = null,
    @SerialName("HerkansingVanKolomId") val retakeOf: Int? = null,
    @SerialName("IsHerkansingVanKolomId") val retakeOfAlt: Int? = null,
)

@Serializable
data class PeriodDto(
    @SerialName("Id") val id: Int? = null,
    @SerialName("Naam") val name: String? = null,
)

@Serializable
data class GradeColumnInfoDto(
    @SerialName("Weging") val weight: Double? = null,
    @SerialName("weegfactor") val weightAlt: Double? = null,
    @SerialName("Weegfactor") val weightPascal: Double? = null,
    @SerialName("KolomOmschrijving") val description: String? = null,
    @SerialName("KolomKop") val heading: String? = null,
    @SerialName("KolomNaam") val name: String? = null,
    @SerialName("KolomSoort") val type: Int? = null,
    @SerialName("TeltMee") val counts: Boolean? = null,
    @SerialName("IsHerkansing") val isRetake: Boolean? = null,
    @SerialName("IsHerkansingKolom") val isRetakeColumn: Boolean? = null,
    @SerialName("HerkansingVanKolomId") val retakeOf: Int? = null,
    @SerialName("IsHerkansingVanKolomId") val retakeOfAlt: Int? = null,
    @SerialName("WerktMeeInVoortschrijdendGemiddelde") val countsInRunning: Boolean? = null,
) {
    val resolvedWeight: Double? get() = weight ?: weightPascal ?: weightAlt
    val resolvedRetakeOf: Int? get() = retakeOf ?: retakeOfAlt
}

@Serializable
data class RecentGradeDto(
    @SerialName("kolomId") val columnId: Int? = null,
    @SerialName("KolomId") val columnIdAlt: Int? = null,
    @SerialName("waarde") val value: String? = null,
    @SerialName("Waarde") val valueAlt: String? = null,
    @SerialName("weegfactor") val weight: Double? = null,
    @SerialName("Weegfactor") val weightAlt: Double? = null,
    @SerialName("isVoldoende") val isSufficient: Boolean? = null,
    @SerialName("IsVoldoende") val isSufficientAlt: Boolean? = null,
    @SerialName("ingevoerdOp") val enteredAt: String? = null,
    @SerialName("IngevoerdOp") val enteredAtAlt: String? = null,
    @SerialName("omschrijving") val description: String? = null,
    @SerialName("Omschrijving") val descriptionAlt: String? = null,
    @SerialName("vak") val subject: RecentSubjectDto? = null,
    @SerialName("Vak") val subjectAlt: RecentSubjectDto? = null,
) {
    val resolvedColumnId: Int? get() = columnId ?: columnIdAlt
    val resolvedValue: String? get() = value ?: valueAlt
    val resolvedWeight: Double? get() = weight ?: weightAlt
    val resolvedSufficient: Boolean? get() = isSufficient ?: isSufficientAlt
    val resolvedEnteredAt: String? get() = enteredAt ?: enteredAtAlt
    val resolvedDescription: String? get() = description ?: descriptionAlt
    val resolvedSubject: RecentSubjectDto? get() = subject ?: subjectAlt
}

@Serializable
data class RecentSubjectDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("Id") val idAlt: Int? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("Afkorting") val codeAlt: String? = null,
    @SerialName("omschrijving") val name: String? = null,
    @SerialName("Omschrijving") val nameAlt: String? = null,
) {
    val resolvedId: Int? get() = id ?: idAlt
    val resolvedCode: String? get() = code ?: codeAlt
    val resolvedName: String? get() = name ?: nameAlt
}

@Serializable
data class AppointmentDto(
    @SerialName("Id") val id: Int? = null,
    @SerialName("Start") val start: String? = null,
    @SerialName("Einde") val end: String? = null,
    @SerialName("LesuurVan") val periodFrom: Int? = null,
    @SerialName("LesuurTotMet") val periodTo: Int? = null,
    @SerialName("Omschrijving") val title: String? = null,
    @SerialName("Lokatie") val location: String? = null,
    @SerialName("Inhoud") val content: String? = null,
    @SerialName("InfoType") val infoType: Int? = null,
    @SerialName("Status") val status: Int? = null,
    @SerialName("Afgerond") val completed: Boolean? = null,
    @SerialName("HeeftBijlagen") val hasAttachments: Boolean? = null,
    @SerialName("Vakken") val subjects: List<NamedDto>? = null,
    @SerialName("Docenten") val teachers: List<TeacherDto>? = null,
    @SerialName("Lokalen") val rooms: List<NamedDto>? = null,
)

@Serializable
data class TeacherDto(
    @SerialName("Id") val id: Int? = null,
    @SerialName("Naam") val name: String? = null,
    @SerialName("Docentcode") val code: String? = null,
)

@Serializable
data class AssignmentDto(
    @SerialName("Id") val id: Int? = null,
    @SerialName("Titel") val title: String? = null,
    @SerialName("Omschrijving") val description: String? = null,
    @SerialName("InleverenVoor") val due: String? = null,
    @SerialName("Afgesloten") val closed: Boolean? = null,
    @SerialName("Vak") val subject: SubjectDto? = null,
    @SerialName("Links") val links: List<RelLinkDto>? = null,
)

@Serializable
data class RelLinkDto(
    @SerialName("Rel") val rel: String? = null,
    @SerialName("Href") val href: String? = null,
    @SerialName("rel") val relAlt: String? = null,
    @SerialName("href") val hrefAlt: String? = null,
) {
    val resolvedRel: String? get() = rel ?: relAlt
    val resolvedHref: String? get() = href ?: hrefAlt
}

@Serializable
data class AbsenceDto(
    @SerialName("Id") val id: Int? = null,
    @SerialName("Omschrijving") val description: String? = null,
    @SerialName("Code") val code: String? = null,
    @SerialName("Start") val start: String? = null,
    @SerialName("Eind") val end: String? = null,
    @SerialName("Geoorloofd") val excused: Boolean? = null,
    @SerialName("Lesuur") val period: Int? = null,
    @SerialName("Afspraak") val appointment: AppointmentDto? = null,
)

@Serializable
data class StudyGuideDto(
    @SerialName("Id") val id: Int? = null,
    @SerialName("Titel") val title: String? = null,
    @SerialName("VakCodes") val subjectCodes: List<String>? = null,
    @SerialName("Van") val from: String? = null,
    @SerialName("TotEnMet") val to: String? = null,
    @SerialName("Links") val links: List<RelLinkDto>? = null,
)

@Serializable
data class AttachmentDto(
    @SerialName("naam") val name: String? = null,
    @SerialName("Naam") val nameAlt: String? = null,
    @SerialName("contentType") val contentType: String? = null,
    @SerialName("grootte") val size: Long? = null,
    @SerialName("links") val links: Map<String, LinkDto>? = null,
)

@Serializable
data class MessageFolderDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("naam") val name: String? = null,
    @SerialName("ongelezen") val unread: Int? = null,
    @SerialName("links") val links: Map<String, LinkDto>? = null,
)

@Serializable
data class LinkDto(
    @SerialName("href") val href: String? = null,
)

@Serializable
data class MessageDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("onderwerp") val subject: String? = null,
    @SerialName("afzender") val sender: SenderDto? = null,
    @SerialName("verzondenOp") val sentAt: String? = null,
    @SerialName("isGelezen") val isRead: Boolean? = null,
    @SerialName("heeftPrioriteit") val priority: Boolean? = null,
    @SerialName("heeftBijlagen") val hasAttachments: Boolean? = null,
    @SerialName("inhoud") val body: String? = null,
    @SerialName("links") val links: Map<String, LinkDto>? = null,
    val extra: JsonElement? = null,
)

@Serializable
data class SenderDto(
    @SerialName("naam") val name: String? = null,
    @SerialName("id") val id: Int? = null,
)

@Serializable
data class SendMessageRequest(
    @SerialName("ontvangers") val to: List<ReceiverDto>,
    @SerialName("cc") val cc: List<ReceiverDto> = emptyList(),
    @SerialName("bcc") val bcc: List<ReceiverDto> = emptyList(),
    @SerialName("heeftPrioriteit") val priority: Boolean = false,
    @SerialName("inhoud") val body: String,
    @SerialName("onderwerp") val subject: String,
    @SerialName("verzendOptie") val sendOption: String = "standaard",
)

@Serializable
data class ReceiverDto(
    @SerialName("id") val id: Int,
    @SerialName("type") val type: String = "persoon",
)
