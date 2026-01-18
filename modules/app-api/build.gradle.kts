plugins {
    id("buildlogic.kotlin-application-conventions")
    application
}

dependencies {
    // Pulls in all the Ktor libraries defined in your bundle
    implementation(libs.bundles.ktor.server)

    // Also add your internal domain module so the API can use your business logic
    implementation(project(":modules:domain"))
}

application {
    mainClass.set("com.github.cgund98.template.app.api.MainKt")
}
