plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.linting-conventions")
}

dependencies {
    implementation(libs.dotenv.kotlin)

    implementation(libs.kotlinx.serialization.json)
}
