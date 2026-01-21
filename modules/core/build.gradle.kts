plugins {
    id("buildlogic.kotlin-library-conventions")
}

dependencies {
    implementation(libs.dotenv.kotlin)

    implementation(libs.kotlinx.serialization.json)
}
