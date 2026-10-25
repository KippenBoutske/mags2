import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.jetbrains.lifecycle.viewmodel)
                implementation(libs.jetbrains.lifecycle.runtime.compose)
                val classifier = when {
                    org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
                    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "mac"
                    else -> "linux"
                }
                implementation("org.openjfx:javafx-base:21.0.5:$classifier")
                implementation("org.openjfx:javafx-graphics:21.0.5:$classifier")
                implementation("org.openjfx:javafx-controls:21.0.5:$classifier")
                implementation("org.openjfx:javafx-swing:21.0.5:$classifier")
                implementation("org.openjfx:javafx-web:21.0.5:$classifier")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "nl.cita.magister.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Mags"
            packageVersion = "0.2.0"
            description = "Mags — onofficiële Magister-client"
            vendor = "Jens"
            copyright = "Unofficial"
            windows {
                menuGroup = "Mags"
                upgradeUuid = "7c3e6d91-2a14-4f60-9b88-6e0d4c1a8f22"
                shortcut = true
                dirChooser = true
            }
        }
    }
}
