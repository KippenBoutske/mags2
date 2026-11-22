package nl.cita.magister.desktop.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object DesktopPaths {
    val isWindows: Boolean
        get() = System.getProperty("os.name").orEmpty().lowercase().contains("win")

    val isMac: Boolean
        get() = System.getProperty("os.name").orEmpty().lowercase().contains("mac")

    fun defaultDataDir(): Path {
        val home = System.getProperty("user.home").orEmpty()
        val base = when {
            isWindows -> {
                val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
                if (appData != null) Paths.get(appData, "Mags") else Paths.get(home, "AppData", "Roaming", "Mags")
            }
            isMac -> Paths.get(home, "Library", "Application Support", "Mags")
            else -> {
                val xdg = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
                if (xdg != null) Paths.get(xdg, "mags") else Paths.get(home, ".config", "mags")
            }
        }
        Files.createDirectories(base)
        return base
    }
}
