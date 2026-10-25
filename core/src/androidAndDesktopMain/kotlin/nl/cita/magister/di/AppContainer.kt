package nl.cita.magister.di

import nl.cita.magister.auth.PendingAuthStore
import nl.cita.magister.auth.SecretStore
import nl.cita.magister.auth.TokenStore
import nl.cita.magister.data.api.MagisterHttpClient
import nl.cita.magister.data.api.PlanGoonClient
import nl.cita.magister.data.db.CitaDatabase
import nl.cita.magister.data.prefs.UserPrefs
import nl.cita.magister.data.repo.MagisterRepository
import nl.cita.magister.platform.appDataDir
import nl.cita.magister.platform.platformEncryptedStore
import nl.cita.magister.platform.platformPlainStore

class AppContainer(
    val prefs: UserPrefs,
    val tokenStore: TokenStore,
    val secretStore: SecretStore,
    val pendingAuth: PendingAuthStore,
    val database: CitaDatabase,
    val http: MagisterHttpClient,
    val planGoon: PlanGoonClient,
    val repository: MagisterRepository,
) {
    companion object {
        fun create(): AppContainer {
            val prefs = UserPrefs(platformPlainStore("mags_user"))
            val tokenStore = TokenStore(platformEncryptedStore("mags_tokens"))
            val secretStore = SecretStore(platformEncryptedStore("mags_secrets"))
            val pendingAuth = PendingAuthStore(platformPlainStore("mags_pending_auth"))
            val database = CitaDatabase.create(appDataDir())
            val http = MagisterHttpClient(tokenStore)
            val planGoon = PlanGoonClient()
            return AppContainer(
                prefs = prefs,
                tokenStore = tokenStore,
                secretStore = secretStore,
                pendingAuth = pendingAuth,
                database = database,
                http = http,
                planGoon = planGoon,
                repository = MagisterRepository(
                    prefs = prefs,
                    tokenStore = tokenStore,
                    secrets = secretStore,
                    http = http,
                    planGoon = planGoon,
                    database = database,
                ),
            )
        }
    }
}
