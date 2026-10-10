pluginManagement {
    // Convention plugins: shared module set-up (SDK levels, Java, Compose, Hilt, tests).
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LiftOnWay"

include(
    ":app",
    ":core:model",
    ":core:data",
    ":core:designsystem",
    ":feature:onboarding",
    ":feature:auth",
    ":feature:home",
    ":feature:findride",
    ":feature:publish",
    ":feature:trips",
    ":feature:notifications",
    ":feature:profile",
    ":feature:wallet",
)
