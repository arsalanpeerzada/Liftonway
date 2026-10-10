plugins {
    alias(libs.plugins.liftonway.android.library)
    alias(libs.plugins.liftonway.android.compose)
}

android {
    namespace = "dev.liftonway.core.designsystem"
}

dependencies {
    api(project(":core:model"))
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.lottie.compose)
    api(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
