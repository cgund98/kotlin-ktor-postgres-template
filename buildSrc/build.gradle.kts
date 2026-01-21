plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation("org.jooq:jooq-codegen:3.20.10")
    implementation("org.jooq:jooq-meta:3.20.10")
}
