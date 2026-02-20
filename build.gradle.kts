import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "2.3.10"
    id("org.jetbrains.compose") version "1.7.0"
    kotlin("plugin.compose") version "2.3.10"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "dev.irof.kfv"
version = "1.0.0"

kotlin {
    jvm("desktop")
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.irof.kfv.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "KifuManager"
            packageVersion = "1.0.0"
        }
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("1.5.0").editorConfigOverride(
            mapOf(
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_function-naming" to "disabled"
            )
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
