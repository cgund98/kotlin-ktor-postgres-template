plugins {
    id("buildlogic.kotlin-application-conventions")
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    implementation(libs.bundles.logging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)

    implementation(platform(libs.aws.bom))
    implementation(libs.aws.sqs)

    implementation(project(":modules:domain"))
    implementation(project(":modules:infrastructure"))
    implementation(project(":modules:core"))
}

application {
    mainClass.set("com.github.cgund98.template.app.worker.MainKt")
}

tasks.withType<JavaExec> {
    // Forces the app to see the project root as the "working directory"
    workingDir = rootProject.projectDir
}
