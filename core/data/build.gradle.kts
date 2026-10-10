plugins {
    alias(libs.plugins.liftonway.android.library)
    alias(libs.plugins.liftonway.hilt)
}

android {
    namespace = "dev.liftonway.core.data"
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.kotlinx.coroutines.test)
}
