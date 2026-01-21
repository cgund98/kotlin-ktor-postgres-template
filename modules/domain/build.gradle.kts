plugins {
    id("buildlogic.kotlin-library-conventions")
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.bundles.logging)

    implementation(project(":modules:infrastructure"))
    api(libs.kotlinx.datetime)

    // jOOQ is used directly in domain module (JooqUserRepository)
    implementation(libs.jooq)

    // Coroutines for suspend functions in JooqUserRepository
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
