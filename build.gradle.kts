import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "2.3.10"
    id("org.jetbrains.compose") version "1.10.1"
    kotlin("plugin.compose") version "2.3.10"
    id("com.diffplug.spotless") version "8.2.1"
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$projectDir/config/detekt/detekt.yml"))
    source.setFrom(files("src/desktopMain/kotlin", "src/desktopTest/kotlin"))
    ignoreFailures = false
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget = "25"
    reports {
        html.required.set(true)
        markdown.required.set(true)
        checkstyle.required.set(true)
        sarif.required.set(false)
    }
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
                implementation(compose.materialIconsExtended)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
                implementation("io.github.oshai:kotlin-logging-jvm:8.0.01")
                implementation("ch.qos.logback:logback-classic:1.5.32")
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
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable,Test",
                "ktlint_standard_no-wildcard-imports" to "disabled"
            )
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.register("verify") {
    group = "verification"
    description = "Runs Spotless check, detekt, tests and coverage."
    dependsOn("spotlessCheck", "detekt", "koverHtmlReport")
}
