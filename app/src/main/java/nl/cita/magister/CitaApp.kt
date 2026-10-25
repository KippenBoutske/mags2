package nl.cita.magister

import android.app.Application
import android.content.Context
import nl.cita.magister.crash.CrashGuard
import nl.cita.magister.di.AppContainer
import nl.cita.magister.live.LiveLessonNotifier
import nl.cita.magister.live.LiveLessonScheduler
import nl.cita.magister.platform.AndroidHooks
import nl.cita.magister.platform.initAndroidPlatform
import nl.cita.magister.work.MagisterSyncWorker
import nl.cita.magister.work.MagsNotifier

class CitaApp : Application() {
    private val containerHolder = lazy { AppContainer.create() }
    val container: AppContainer get() = containerHolder.value

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        initAndroidPlatform(this)
        CrashGuard.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        initAndroidPlatform(this)
        if (CrashGuard.isCrashProcess(this)) return
        AndroidHooks.liveSync = { LiveLessonScheduler.sync(this) }
        AndroidHooks.liveCancel = { LiveLessonScheduler.cancel(this) }
        AndroidHooks.liveTick = { LiveLessonScheduler.sync(this) }
        runCatching { MagsNotifier.ensureChannel(this) }
        runCatching { LiveLessonNotifier.ensureChannel(this) }
        runCatching { MagisterSyncWorker.enqueue(this) }
        runCatching { LiveLessonScheduler.sync(this) }
    }
}
