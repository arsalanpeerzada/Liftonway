plugins {
    alias(libs.plugins.liftonway.android.library)
}

android {
    namespace = "dev.liftonway.core.model"
}

dependencies {
    // Compose's @Immutable keeps model lists stable, so screens skip recomposition when nothing changed.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
}
