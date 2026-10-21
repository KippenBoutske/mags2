package nl.cita.magister.di

import android.content.Context
import nl.cita.magister.auth.PendingAuthStore
import nl.cita.magister.auth.SecretStore
import nl.cita.magister.auth.TokenStore
import nl.cita.magister.data.api.MagisterHttpClient
import nl.cita.magister.data.api.PlanGoonClient
import nl.cita.magister.data.db.CitaDatabase
import nl.cita.magister.data.prefs.UserPrefs
import nl.cita.magister.data.repo.MagisterRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val prefs = UserPrefs(appContext)
    val tokenStore = TokenStore(appContext)
    val secretStore = SecretStore(appContext)
    val pendingAuth = PendingAuthStore(appContext)
    val database = CitaDatabase.create(appContext)
    val http = MagisterHttpClient(tokenStore)
    val planGoon = PlanGoonClient()
    val repository = MagisterRepository(
        prefs = prefs,
        tokenStore = tokenStore,
        secrets = secretStore,
        http = http,
        planGoon = planGoon,
        database = database,
    )
}
