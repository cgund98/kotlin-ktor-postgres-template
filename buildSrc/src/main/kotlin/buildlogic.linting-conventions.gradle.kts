// No complex task configuration, no imports, no fancy lazy properties.
// Just the basic application of the tools.
plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

// Simple configuration using strings to avoid 'Unresolved Reference' errors
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.3.0")
    outputToConsole.set(true)
}

configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("gradle-config/detekt.yml"))
}
