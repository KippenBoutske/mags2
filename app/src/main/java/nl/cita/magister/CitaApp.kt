package nl.cita.magister

import android.app.Application
import android.content.Context
import nl.cita.magister.crash.CrashGuard
import nl.cita.magister.di.AppContainer

class CitaApp : Application() {
    private val containerHolder = lazy { AppContainer(this) }
    val container: AppContainer get() = containerHolder.value

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        CrashGuard.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (CrashGuard.isCrashProcess(this)) return
        // AppContainer blijft lazy: MasterKey/prefs gebeuren pas bij eerste scherm,
        // nadat de crash-handler staat.
    }
}
