package nl.cita.magister.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.material3.ColorProviders
import nl.cita.magister.ui.theme.WarmDarkScheme
import nl.cita.magister.ui.theme.WarmLightScheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import nl.cita.magister.MainActivity
import nl.cita.magister.ui.formatTime
import nl.cita.magister.ui.formatWhen

class NextLessonWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lesson = NextLessonStore.read(context)
        provideContent {
            GlanceTheme(
                colors = ColorProviders(
                    light = WarmLightScheme,
                    dark = WarmDarkScheme,
                ),
            ) {
                NextLessonContent(lesson)
            }
        }
    }
}

@Composable
private fun NextLessonContent(lesson: NextLessonSnapshot?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Volgende les",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))
        if (lesson == null) {
            Text(
                "Nog geen les in de cache.",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp),
            )
        } else {
            Text(
                lesson.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                listOfNotNull(formatWhen(lesson.startIso).ifBlank { formatTime(lesson.startIso) }, lesson.room)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
        }
    }
}

class NextLessonWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextLessonWidget()

    companion object {
        suspend fun updateAll(context: Context) {
            NextLessonWidget().updateAll(context)
        }
    }
}
