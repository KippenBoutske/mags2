import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}

compose.desktop {
    application {
        mainClass = "nl.cita.magister.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "Mags"
            packageVersion = "0.1.20"
            description = "Onofficiële desktop-app voor Magister"
            vendor = "Mags"

            modules(
                "java.instrument",
                "java.logging",
                "java.naming",
                "java.net.http",
                "java.prefs",
                "jdk.crypto.ec",
                "jdk.unsupported",
            )

            windows {
                menuGroup = "Mags"
                shortcut = true
                perUserInstall = true
                upgradeUuid = "8d3e4f21-6c9a-4b10-9e73-2f5a1c8b0d42"
            }

            linux {
                packageName = "mags-desktop"
                debMaintainer = "mags@local"
            }
        }
    }
}
