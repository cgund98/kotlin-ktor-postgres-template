plugins {
    id("buildlogic.kotlin-library-conventions")
}

dependencies {
    implementation(libs.bundles.exposed)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.bundles.logging)

    implementation(project(":modules:infrastructure"))
    api(libs.kotlinx.datetime)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
