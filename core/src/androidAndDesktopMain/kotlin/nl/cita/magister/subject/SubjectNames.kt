package nl.cita.magister.subject

import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.SubjectAverage
import java.util.Locale

/**
 * Magister/Zermelo-vakcodes en lesgroepcodes → leesbare vaknaam.
 * Liever `Vak.Omschrijving` uit de API; anders de VO-tabel.
 */
object SubjectNames {
    private val nl = Locale("nl", "NL")

    private val names: Map<String, String> = mapOf(
        "nat" to "Natuurkunde",
        "na" to "Natuurkunde",
        "natk" to "Natuurkunde",
        "nask" to "Nask",
        "nask1" to "Nask 1",
        "nask2" to "Nask 2",
        "schk" to "Scheikunde",
        "sk" to "Scheikunde",
        "schei" to "Scheikunde",
        "biol" to "Biologie",
        "bi" to "Biologie",
        "bio" to "Biologie",
        "wisb" to "Wiskunde B",
        "wisa" to "Wiskunde A",
        "wisc" to "Wiskunde C",
        "wisd" to "Wiskunde D",
        "wis" to "Wiskunde",
        "wi" to "Wiskunde",
        "wisk" to "Wiskunde",
        "ne" to "Nederlands",
        "ned" to "Nederlands",
        "netl" to "Nederlands",
        "entl" to "Engels",
        "en" to "Engels",
        "eng" to "Engels",
        "et" to "Engels",
        "fa" to "Frans",
        "fr" to "Frans",
        "fatl" to "Frans",
        "du" to "Duits",
        "dui" to "Duits",
        "dutl" to "Duits",
        "sp" to "Spaans",
        "spa" to "Spaans",
        "sptl" to "Spaans",
        "la" to "Latijn",
        "lat" to "Latijn",
        "grc" to "Grieks",
        "gr" to "Grieks",
        "ec" to "Economie",
        "econ" to "Economie",
        "ek" to "Economie",
        "bec" to "Bedrijfseconomie",
        "becon" to "Bedrijfseconomie",
        "mo" to "Management & Organisatie",
        "m&o" to "Management & Organisatie",
        "gs" to "Geschiedenis",
        "ges" to "Geschiedenis",
        "gesch" to "Geschiedenis",
        "ak" to "Aardrijkskunde",
        "aard" to "Aardrijkskunde",
        "lo" to "Lichamelijke opvoeding",
        "pe" to "Lichamelijke opvoeding",
        "lob" to "Loopbaanoriëntatie",
        "ckv" to "CKV",
        "kv" to "CKV",
        "kcv" to "KCV",
        "anw" to "ANW",
        "maat" to "Maatschappijleer",
        "ma" to "Maatschappijleer",
        "ml" to "Maatschappijleer",
        "maw" to "Maatschappijwetenschappen",
        "in" to "Informatica",
        "inf" to "Informatica",
        "info" to "Informatica",
        "ict" to "Informatica",
        "pws" to "Profielwerkstuk",
        "mentor" to "Mentoruur",
        "ment" to "Mentoruur",
        "mnt" to "Mentoruur",
        "nlt" to "NLT",
        "mu" to "Muziek",
        "muz" to "Muziek",
        "te" to "Tekenen",
        "tek" to "Tekenen",
        "be" to "Beeldende vorming",
        "bv" to "Beeldende vorming",
        "dr" to "Drama",
        "da" to "Dans",
        "fi" to "Filosofie",
        "fil" to "Filosofie",
        "rek" to "Rekenen",
        "lv" to "Levensbeschouwing",
        "god" to "Godsdienst",
        "rlv" to "Levensbeschouwing",
        "rkv" to "Godsdienst",
    )

    fun display(
        title: String? = null,
        subjects: List<String> = emptyList(),
        code: String? = null,
    ): String {
        subjects.firstOrNull { looksLikeName(it) }?.let { return pretty(it) }
        title?.takeIf { looksLikeName(it) }?.let { return pretty(it) }
        lookup(code)?.let { return it }
        subjects.firstNotNullOfOrNull { lookup(it) }?.let { return it }
        val parsed = parseTitle(title)
        lookup(parsed.subjectCode)?.let { return it }
        parsed.subjectCode?.takeIf { looksLikeName(it) }?.let { return pretty(it) }
        lookup(title)?.let { return it }
        return pretty(parsed.subjectCode ?: title?.trim()?.takeIf { it.isNotEmpty() } ?: "Les")
    }

    fun teacherAbbrev(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (isGroupCode(value)) return null
        if (value.length <= 5 && !value.contains(' ')) return value
        val last = value.substringAfterLast(' ').trim()
        if (last.length in 2..5 && last.all { it.isLetter() }) return last.lowercase(nl)
        return last.take(4).lowercase(nl).takeIf { it.isNotBlank() }
    }

    fun groupCode(title: String?, subjects: List<String> = emptyList()): String? {
        parseTitle(title).groupCode?.let { return it }
        return subjects.firstOrNull { isGroupCode(it) }
    }

    fun lookup(raw: String?): String? {
        val key = codeKey(raw) ?: return null
        names[key]?.let { return it }
        // Prefix-match short codes only (`wisb`, `nask1`). Full names like
        // "Wiskunde B" → `wiskundeb` must not collapse to `wis` → "Wiskunde".
        if (key.length in 3..8) {
            names.keys
                .filter { it.length >= 3 && key.startsWith(it) }
                .maxByOrNull { it.length }
                ?.let { return names[it] }
        }
        return null
    }

    internal fun parseTitle(title: String?): ParsedTitle {
        val raw = title?.trim().orEmpty()
        if (raw.isEmpty()) return ParsedTitle()
        val parts = raw.split(Regex("""\s+[-–—]\s+""")).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size >= 2) {
            val group = parts.lastOrNull { isGroupCode(it) }
            val remaining = parts.filter { it != group }
            val subject = remaining.firstOrNull()
            val teacher = remaining.drop(1).firstOrNull { it.length <= 6 && !it.contains(' ') }
            return ParsedTitle(subject, teacher, group)
        }
        if (isGroupCode(raw)) return ParsedTitle(subjectCode = codeKey(raw), groupCode = raw)
        return ParsedTitle(subjectCode = raw)
    }

    internal fun isGroupCode(value: String): Boolean {
        val t = value.trim()
        return t.matches(Regex("""\d+[a-zA-Z]?\.[A-Za-z]{2,}\d*""")) ||
            t.matches(Regex("""[A-Za-z]{2,}\.\d+[a-zA-Z]?"""))
    }

    internal fun looksLikeName(value: String?): Boolean {
        val t = value?.trim().orEmpty()
        if (t.length < 4 || isGroupCode(t) || t.contains(" - ")) return false
        if (lookup(t) != null && t.length <= 5 && !t.contains(' ')) return false
        return t.any { it.isWhitespace() } || t.any { it.isLowerCase() && it.isLetter() }
    }

    private fun codeKey(raw: String?): String? {
        val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val core = if ('.' in t && isGroupCode(t)) t.substringAfterLast('.') else t
        val stripped = core.lowercase(nl).replace(Regex("\\d+$"), "")
        val key = stripped.replace(Regex("[^a-z+&]"), "")
        return key.takeIf { it.isNotEmpty() }
    }

    private fun pretty(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "Les"
        if (trimmed.any { it.isWhitespace() }) return trimmed
        lookup(trimmed)?.let { return it }
        if (trimmed.length <= 4 && trimmed.all { it.isLetter() && it.isUpperCase() }) return trimmed
        return trimmed
    }

    data class ParsedTitle(
        val subjectCode: String? = null,
        val teacherCode: String? = null,
        val groupCode: String? = null,
    )
}

fun Appointment.displaySubject(): String = SubjectNames.display(title = title, subjects = subjects)

fun Appointment.teacherCode(): String? =
    teachers.firstNotNullOfOrNull { SubjectNames.teacherAbbrev(it) }
        ?: SubjectNames.parseTitle(title).teacherCode?.let { SubjectNames.teacherAbbrev(it) }

fun Grade.displaySubject(): String = SubjectNames.display(title = subjectName, code = subjectCode)

fun SubjectAverage.displaySubject(): String = SubjectNames.display(title = subjectName, code = subjectCode)

fun HomeworkItem.displaySubject(): String = SubjectNames.display(title = subject)
