dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    // The same catalogue as the app, so versions live in one place.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
