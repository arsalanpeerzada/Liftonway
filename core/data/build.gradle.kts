plugins {
    alias(libs.plugins.liftonway.android.library)
    alias(libs.plugins.liftonway.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// `-Pliftonway.fake=true` builds the offline demo with in-memory data instead of calling the backend.
// `-Pliftonway.apiUrl=...` points debug builds at another server; the default is the host machine as the emulator sees it.
// `-Pliftonway.releaseApiUrl=https://...` is the deployed API for release builds. It must be HTTPS.
val useFake = providers.gradleProperty("liftonway.fake").map(String::toBoolean).getOrElse(false)
val apiUrl = providers.gradleProperty("liftonway.apiUrl").getOrElse("http://10.0.2.2:8080/api/v1/")
val releaseApiUrl = providers.gradleProperty("liftonway.releaseApiUrl").getOrElse("https://api.liftonway.invalid/api/v1/")
require(releaseApiUrl.startsWith("https://")) { "liftonway.releaseApiUrl must use HTTPS" }

android {
    namespace = "dev.liftonway.core.data"
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("boolean", "USE_FAKE_BACKEND", useFake.toString())
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
    }
    buildTypes {
        // Until a server is deployed this is a reserved, unreachable name, so a release build can never
        // quietly talk to a developer's machine.
        release { buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"") }
    }
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
