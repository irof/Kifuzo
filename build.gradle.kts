import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "2.3.10"
    id("org.jetbrains.compose") version "1.10.1"
    kotlin("plugin.compose") version "2.3.10"
    id("com.diffplug.spotless") version "8.3.0"
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
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
                implementation("io.github.oshai:kotlin-logging-jvm:8.0.01")
                implementation("ch.qos.logback:logback-classic:1.5.32")
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(compose.desktop.uiTestJUnit4)
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

// Kover によるカバレッジ検証の設定
// UI関連のコードが未テストで全体の数値を下げるため、検証対象から除外します。
// これにより、コアロジック(Logic/Models)のカバレッジ低下を確実に検出できるようにします。
kover {
    reports {
        filters {
            excludes {
                packages("dev.irof.kifuzo.ui.*", "dev.irof.kifuzo.viewmodel.*")
            }
        }
        verify {
            rule {
                bound {
                    // 行カバレッジ 80% 以上を目標とします。
                    // 現時点の実測値（UI/ViewModel除外で約80.5%）に合わせています。
                    minValue.set(80)
                }
            }
        }
    }
}

tasks.register("verify") {
    group = "verification"
    description = "Runs Spotless check, detekt, tests and coverage verification."
    dependsOn("spotlessCheck", "detekt", "koverVerify", "koverHtmlReport")
}
