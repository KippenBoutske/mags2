package nl.cita.magister.wear.tile

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import nl.cita.magister.grades.GradeCalculator
import nl.cita.magister.subject.displaySubject
import nl.cita.magister.wear.WearMainActivity
import nl.cita.magister.wear.WearScheduleHelper
import nl.cita.magister.wear.data.WearSnapshotStore
import nl.cita.magister.wear.ui.WearPalette
import nl.cita.magister.wear.ui.resolveWearPalette
import java.time.Instant

private const val RESOURCES_VERSION = "2"
private const val FRESHNESS_MS = 60_000L

class NextLessonTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> = CallbackToFutureAdapter.getFuture { completer ->
        val tile = buildTile(this, requestParams)
        completer.set(tile)
        "mags-next-lesson-tile"
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = CallbackToFutureAdapter.getFuture { completer ->
        completer.set(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build(),
        )
        "mags-next-lesson-resources"
    }

    private fun buildTile(
        context: Context,
        requestParams: RequestBuilders.TileRequest,
    ): TileBuilders.Tile {
        val palette = resolveWearPalette(context)
        val payload = WearSnapshotStore.payload.value ?: WearSnapshotStore.load(context)
        val now = Instant.now()
        val focus = payload?.let { WearScheduleHelper.currentOrNextLesson(it.appointments, now) }
        val latestGrade = payload?.latestGrades?.firstOrNull()

        val openAppClickable = ModifiersBuilders.Clickable.Builder()
            .setId("open-mags")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(context.packageName)
                            .setClassName(WearMainActivity::class.java.name)
                            .build(),
                    )
                    .build(),
            )
            .build()

        val primaryLabelText: String
        val titleText: String
        val secondaryLine: String
        val chipText: String

        if (payload == null) {
            primaryLabelText = "MAGS"
            titleText = "Open Mags"
            secondaryLine = "Synchroniseer vanaf telefoon"
            chipText = "Openen"
        } else if (focus == null) {
            val today = now.atZone(WearScheduleHelper.defaultZone).toLocalDate()
            val todayCount = WearScheduleHelper.lessonsForDate(payload.appointments, today).size
            primaryLabelText = "VANDAAG"
            titleText = if (todayCount == 0) "Vrij vandaag" else "Klaar voor vandaag"
            secondaryLine = latestGrade?.let {
                "Laatste: ${it.displaySubject()} ${GradeCalculator.formatGradeValue(it.numericValue, it.value)}"
            } ?: "Geen lessen meer"
            chipText = "Rooster"
        } else {
            val countdown = WearScheduleHelper.countdownLabel(focus)
            val room = focus.lesson.classroom ?: focus.lesson.location
            primaryLabelText = if (focus.isCurrent) {
                "NU · $countdown"
            } else {
                "VOLGENDE · $countdown"
            }
            titleText = focus.lesson.displaySubject()
            secondaryLine = listOfNotNull(
                if (focus.isCurrent) "tot ${focus.endLabel}" else "${focus.startLabel}–${focus.endLabel}",
                room?.let { "Lokaal $it" },
            ).joinToString(" · ")
            chipText = latestGrade?.let {
                "${it.displaySubject().take(10)} ${GradeCalculator.formatGradeValue(it.numericValue, it.value)}"
            } ?: "Rooster"
        }

        val layout = PrimaryLayout.Builder(requestParams.deviceConfiguration)
            .setResponsiveContentInsetEnabled(true)
            .setPrimaryLabelTextContent(
                Text.Builder(context, primaryLabelText)
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(argb(palette.primaryArgb()))
                    .setMaxLines(1)
                    .build(),
            )
            .setContent(
                buildCenterContent(
                    context = context,
                    title = titleText,
                    subtitle = secondaryLine,
                    palette = palette,
                    clickable = openAppClickable,
                ),
            )
            .setPrimaryChipContent(
                CompactChip.Builder(context, chipText, openAppClickable, requestParams.deviceConfiguration)
                    .setChipColors(
                        ChipColors(
                            argb(palette.primaryContainerArgb()),
                            argb(palette.onSurfaceArgb()),
                        ),
                    )
                    .build(),
            )
            .build()

        // Box MUST use expand() width & height; wrap() collapses PrimaryLayout to 0x0.
        val root = LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(palette.backgroundArgb()))
                            .build(),
                    )
                    .setClickable(openAppClickable)
                    .build(),
            )
            .addContent(layout)
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(root)
                            .build(),
                    )
                    .build(),
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_MS)
            .setTileTimeline(timeline)
            .build()
    }

    private fun buildCenterContent(
        context: Context,
        title: String,
        subtitle: String,
        palette: WearPalette,
        clickable: ModifiersBuilders.Clickable,
    ): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(clickable)
                    .build(),
            )
            .addContent(
                Text.Builder(context, title)
                    .setTypography(Typography.TYPOGRAPHY_TITLE2)
                    .setColor(argb(palette.onSurfaceArgb()))
                    .setMaxLines(2)
                    .build(),
            )
            .addContent(
                LayoutElementBuilders.Spacer.Builder()
                    .setHeight(dp(4f))
                    .build(),
            )
            .addContent(
                Text.Builder(context, subtitle)
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(argb(palette.mutedArgb()))
                    .setMaxLines(2)
                    .build(),
            )
            .build()
    }

    companion object {
        fun requestUpdate(context: Context) {
            runCatching {
                getUpdater(context.applicationContext).requestUpdate(NextLessonTileService::class.java)
            }
        }
    }
}
