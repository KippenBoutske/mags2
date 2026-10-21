package nl.cita.magister

import android.app.Application
import nl.cita.magister.di.AppContainer

class CitaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
