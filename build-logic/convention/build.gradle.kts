plugins {
    `kotlin-dsl`
}

group = "dev.liftonway.buildlogic"

dependencies {
    // Only the Android plugin's API is needed to compile; the plugins themselves come from the root build.
    compileOnly(libs.android.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "liftonway.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "liftonway.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "liftonway.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("androidFeature") {
            id = "liftonway.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
    }
}
