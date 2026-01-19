plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.linting-conventions")
}

dependencies {
    implementation(libs.bundles.exposed)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.bundles.postgres)

    implementation(project(":modules:core"))

    testImplementation("io.mockk:mockk:1.13.12")
}
