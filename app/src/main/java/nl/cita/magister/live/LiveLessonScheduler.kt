package nl.cita.magister.live

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.cita.magister.CitaApp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object LiveLessonScheduler {
    private const val REQ_ALARM = 4301
    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun sync(context: Context) {
        val app = context.applicationContext
        scope.launch { runCatching { tick(app) } }
    }

    suspend fun tick(context: Context, refreshIfNeeded: Boolean = false) {
        val app = context.applicationContext as? CitaApp ?: return
        val prefs = app.container.prefs
        val repo = app.container.repository
        if (!prefs.liveUpdateNow() || (!repo.isLoggedIn() && !repo.isDemo())) {
            cancel(app)
            return
        }
        val today = LocalDate.now(zone).toString()
        if (prefs.liveHiddenDateNow() == today) {
            LiveLessonNotifier.cancel(app)
            val wake = LiveLesson.nextWake(repo.cachedAppointments())
            if (wake != null) schedule(app, wake) else cancelAlarm(app)
            return
        }
        if (refreshIfNeeded && prefs.liveRefreshDateNow() != today) {
            runCatching { repo.refreshAll() }
            prefs.setLiveRefreshDate(today)
        }
        val lessons = repo.cachedAppointments()
        val snap = LiveLesson.snapshot(lessons)
        if (snap == null) {
            LiveLessonNotifier.cancel(app)
        } else {
            LiveLessonNotifier.post(app, snap)
        }
        val wake = snap?.let { Instant.ofEpochMilli(it.nextAlarmMillis) }
            ?: LiveLesson.nextWake(lessons)
        if (wake != null) schedule(app, wake) else cancelAlarm(app)
    }

    suspend fun hideToday(context: Context) {
        val app = context.applicationContext as? CitaApp ?: return
        app.container.prefs.setLiveHiddenDate(LocalDate.now(zone).toString())
        LiveLessonNotifier.cancel(app)
        val wake = LiveLesson.nextWake(app.container.repository.cachedAppointments())
        if (wake != null) schedule(app, wake) else cancelAlarm(app)
    }

    fun cancel(context: Context) {
        LiveLessonNotifier.cancel(context)
        cancelAlarm(context)
    }

    private fun schedule(context: Context, at: Instant) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val millis = at.toEpochMilli().coerceAtLeast(System.currentTimeMillis() + 5_000L)
        val pi = alarmIntent(context)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun cancelAlarm(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(alarmIntent(context))
    }

    private fun alarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, LiveLessonReceiver::class.java).setAction(LiveLessonReceiver.ACTION_TICK)
        return PendingIntent.getBroadcast(
            context,
            REQ_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
