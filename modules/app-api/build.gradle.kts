plugins {
    id("buildlogic.kotlin-application-conventions")
    id("buildlogic.linting-conventions")
    application
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)

    implementation(libs.bundles.ktor.server)

    // Also add your internal domain module so the API can use your business logic
    implementation(project(":modules:domain"))
    implementation(project(":modules:presentation"))
    implementation(project(":modules:infrastructure"))
    implementation(project(":modules:core"))
}

application {
    mainClass.set("com.github.cgund98.template.app.api.MainKt")
}

tasks.withType<JavaExec> {
    // Forces the app to see the project root as the "working directory"
    workingDir = rootProject.projectDir
}
