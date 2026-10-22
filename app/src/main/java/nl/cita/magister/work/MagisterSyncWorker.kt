package nl.cita.magister.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.cita.magister.CitaApp
import nl.cita.magister.agenda.NextLesson
import nl.cita.magister.notify.ChangeDetector
import nl.cita.magister.widget.NextLessonStore
import nl.cita.magister.widget.NextLessonWidgetReceiver
import java.util.concurrent.TimeUnit

class MagisterSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? CitaApp ?: return Result.retry()
        val repo = app.container.repository
        val prefs = app.container.prefs
        if (!repo.isLoggedIn() && !repo.isDemo()) return Result.success()
        runCatching { repo.refreshAll() }
        val grades = repo.cachedGrades()
        val lessons = repo.cachedAppointments()
        val gradeIds = grades.map { it.id }.toSet()
        val lessonKeys = lessons.map { ChangeDetector.appointmentKey(it) }.toSet()
        if (prefs.notificationsNow() && prefs.snapshotReady()) {
            MagsNotifier.grades(applicationContext, ChangeDetector.newGrades(prefs.seenGradeIds(), grades))
            MagsNotifier.schedule(applicationContext, ChangeDetector.scheduleDiff(prefs.seenAppointmentKeys(), lessons))
        }
        prefs.rememberSnapshot(gradeIds, lessonKeys)
        NextLessonStore.write(applicationContext, NextLesson.pick(lessons))
        NextLessonWidgetReceiver.updateAll(applicationContext)
        nl.cita.magister.live.LiveLessonScheduler.sync(applicationContext)
        return Result.success()
    }

    companion object {
        const val UNIQUE = "mags-sync"

        fun enqueue(context: Context) {
            MagsNotifier.ensureChannel(context)
            val request = PeriodicWorkRequestBuilder<MagisterSyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            val manager = WorkManager.getInstance(context)
            manager.enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, request)
            manager.enqueueUniqueWork(
                "$UNIQUE-once",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<MagisterSyncWorker>().build(),
            )
        }
    }
}
