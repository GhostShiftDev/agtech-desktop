import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.compose") version "1.9.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

group = "com.agtech"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Compose Desktop (incluye UI, Foundation, runtime para el SO actual)
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Coroutines para Swing (Dispatchers.Main en desktop)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // Cliente HTTP (mismo stack que la app Android)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
}

compose.desktop {
    application {
        mainClass = "com.agtech.nas.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "AGTech"
            packageVersion = "1.0.0"
            description = "AGTech NAS - cliente de escritorio"
            vendor = "AGTech"

            windows {
                iconFile.set(project.file("icons/icon.ico"))
                menuGroup = "AGTech"
                // Identificador estable para que las actualizaciones reemplacen la instalación
                upgradeUuid = "b6c1f0a2-7d3e-4a55-9b8e-2c4f6a1d0e37"
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
                menuGroup = "AGTech"
                appCategory = "Utility"
                debMaintainer = "soporte@agtech.local"
            }
        }
    }
}
