plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.linting-conventions")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.bundles.exposed)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.bundles.postgres)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.aws.bom))
    implementation(libs.aws.sns)
    implementation(libs.aws.sqs)

    implementation(project(":modules:core"))

    testImplementation("io.mockk:mockk:1.13.12")
}
