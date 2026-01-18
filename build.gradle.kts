plugins {
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}

// 2. Proactively apply it to all current and future modules
subprojects {
    if (File(projectDir, "src").exists()) {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "io.gitlab.arturbosch.detekt")

        configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set("1.3.0") // The engine version
            verbose.set(true)

            android.set(false)

            outputToConsole.set(true)
            coloredOutput.set(true)

            reporters {
                reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
            }

            // Ignores generated code
            filter {
                exclude { it.file.path.contains("build/") }
                exclude { it.file.path.contains("generated/") }
            }
        }

        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true // Start with standard rules
            allRules = false // Don't be too aggressive at start
            config.setFrom(files("$rootDir/gradle-config/detekt.yml"))
        }
    }
}
