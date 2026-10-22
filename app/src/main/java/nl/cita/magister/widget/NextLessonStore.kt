package nl.cita.magister.widget

import android.content.Context
import kotlinx.serialization.Serializable
import nl.cita.magister.model.Appointment
import nl.cita.magister.parse.MagisterJson
import nl.cita.magister.subject.displaySubject

@Serializable
data class NextLessonSnapshot(
    val title: String,
    val startIso: String,
    val room: String?,
)

object NextLessonStore {
    private const val PREFS = "mags_widget"
    private const val KEY = "next_lesson"

    fun write(context: Context, lesson: Appointment?) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (lesson == null) {
            prefs.edit().remove(KEY).apply()
            return
        }
        val snap = NextLessonSnapshot(
            title = lesson.displaySubject(),
            startIso = lesson.startIso,
            room = lesson.classroom ?: lesson.location,
        )
        prefs.edit().putString(KEY, MagisterJson.instance.encodeToString(NextLessonSnapshot.serializer(), snap)).apply()
    }

    fun read(context: Context): NextLessonSnapshot? {
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return null
        return runCatching {
            MagisterJson.instance.decodeFromString(NextLessonSnapshot.serializer(), raw)
        }.getOrNull()
    }
}
