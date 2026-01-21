plugins {
    id("buildlogic.kotlin-library-conventions")
}

// Define integrationTest source set
sourceSets {
    create("integrationTest") {
        kotlin.srcDir("src/integrationTest/kotlin")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations {
    @Suppress("UnusedPrivateProperty")
    val integrationTestImplementation by getting {
        extendsFrom(configurations["testImplementation"])
    }

    @Suppress("UnusedPrivateProperty")
    val integrationTestRuntimeOnly by getting {
        extendsFrom(configurations["testRuntimeOnly"])
    }
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

    testImplementation(libs.bundles.test)

    // Integration test dependencies
    "integrationTestImplementation"(libs.bundles.test)
    "integrationTestImplementation"(libs.testcontainers.postgresql)
    "integrationTestImplementation"(libs.flyway.core)
    "integrationTestImplementation"(libs.flyway.database.postgresql)
    "integrationTestImplementation"(libs.koin.test)
    "integrationTestImplementation"(project(":modules:infrastructure"))
}

// Create integrationTest task
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests against a Postgres testcontainer"
    group = "verification"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath

    useJUnitPlatform()

    // Don't run integration tests by default with ./gradlew test
    shouldRunAfter("test")

    // Parallel test execution
    maxParallelForks =
        Runtime
            .getRuntime()
            .availableProcessors()
            .div(2)
            .coerceAtLeast(1)

    // Fork a new JVM for each test class
    setForkEvery(100)

    // Increase memory for tests
    minHeapSize = "256m"
    maxHeapSize = "1g"
}
