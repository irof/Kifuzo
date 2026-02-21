import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "2.3.10"
    id("org.jetbrains.compose") version "1.7.0"
    kotlin("plugin.compose") version "2.3.10"
    id("com.diffplug.spotless") version "8.2.1"
}

group = "dev.irof.kifuzo"
version = "1.0.0"

kotlin {
    jvmToolchain(25)
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
        mainClass = "dev.irof.kifuzo.MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Kifuzo"
            packageVersion = "1.0.0"
        }
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_standard_blank-line-between-when-conditions" to "disabled",
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable"
            )
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.register("verify") {
    group = "verification"
    description = "Runs Spotless check and all tests."
    dependsOn("spotlessCheck", "desktopTest")
}
