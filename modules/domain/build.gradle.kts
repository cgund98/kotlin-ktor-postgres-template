plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.linting-conventions")
}

dependencies {
    implementation(libs.bundles.exposed)

    implementation(project(":modules:infrastructure"))
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
