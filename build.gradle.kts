import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "2.3.10"
    id("org.jetbrains.compose") version "1.7.0"
    kotlin("plugin.compose") version "2.3.10"
}

group = "com.example.kifumanager"
version = "1.0.0"

kotlin {
    jvm("desktop")
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "KifuManager"
            packageVersion = "1.0.0"
        }
    }
}
