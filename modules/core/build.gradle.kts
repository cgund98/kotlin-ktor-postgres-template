plugins {
    id("buildlogic.kotlin-library-conventions")
    id("buildlogic.linting-conventions")
}

dependencies {
    implementation(libs.dotenv.kotlin)
}
