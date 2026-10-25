package nl.cita.magister.data

/**
 * HTTP-refresh-gedrag leeft nu in `:core` (`nl.cita.magister.data.HttpClientRefreshTest`)
 * zodat dezelfde Ktor-client op Android en desktop getest wordt.
 */
class HttpClientRefreshTest {
    @org.junit.Test
    fun seeCoreModule() {
        // Bewust leeg: de echte cases staan in :core:jvmTest.
    }
}
