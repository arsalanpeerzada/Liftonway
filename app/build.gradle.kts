import groovy.json.JsonSlurper
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "dev.liftonway.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.arsalanpeerzada.liftonway"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-prototype"

        // Push needs the Firebase app's identifiers. They come from google-services.json (not committed);
        // without it the app builds and runs with push switched off.
        val firebase = firebaseConfig(applicationId!!)
        listOf("APP_ID", "API_KEY", "PROJECT_ID", "SENDER_ID").forEach { key ->
            buildConfigField("String", "FIREBASE_$key", "\"${firebase[key].orEmpty()}\"")
        }
    }

    // The Play upload key comes from keystore.properties (never committed) or LIFTONWAY_* environment variables
    // on a build server. Without one, release builds fall back to the debug key: they install for testing,
    // and Play rejects them, so a debug-signed build can't be published by mistake.
    val upload = uploadKey()
    signingConfigs {
        if (upload != null) {
            create("upload") {
                storeFile = rootProject.file(upload.getProperty("storeFile"))
                storePassword = upload.getProperty("storePassword")
                keyAlias = upload.getProperty("keyAlias")
                keyPassword = upload.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug").also {
                logger.warn("No upload key configured: release builds are signed with the debug key and can't go to Play.")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:findride"))
    implementation(project(":feature:publish"))
    implementation(project(":feature:trips"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:wallet"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    ksp(libs.hilt.compiler)

    // Installs the generated baseline profile so release builds start and scroll pre-compiled.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
}

/** keystore.properties at the project root, or the same four values from the environment. */
fun uploadKey(): Properties? {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) return Properties().apply { file.inputStream().use(::load) }
    val env = mapOf(
        "storeFile" to "LIFTONWAY_KEYSTORE_FILE",
        "storePassword" to "LIFTONWAY_KEYSTORE_PASSWORD",
        "keyAlias" to "LIFTONWAY_KEY_ALIAS",
        "keyPassword" to "LIFTONWAY_KEY_PASSWORD",
    ).mapValues { System.getenv(it.value) }
    if (env.values.any { it.isNullOrBlank() }) return null
    return Properties().apply { env.forEach { (k, v) -> setProperty(k, v) } }
}

/**
 * Reads the four values Firebase needs from app/google-services.json, for the client registered under
 * [packageName]. Empty when the file is missing. This replaces the google-services Gradle plugin.
 */
fun firebaseConfig(packageName: String): Map<String, String> {
    val file = project.file("google-services.json")
    if (!file.exists()) return emptyMap()
    @Suppress("UNCHECKED_CAST")
    val json = JsonSlurper().parse(file) as Map<String, Any?>
    val project = json["project_info"] as Map<String, Any?>
    val client = (json["client"] as List<Map<String, Any?>>).firstOrNull {
        ((it["client_info"] as Map<String, Any?>)["android_client_info"] as Map<String, Any?>)["package_name"] == packageName
    } ?: error("google-services.json has no Android app registered as $packageName")
    return mapOf(
        "APP_ID" to (client["client_info"] as Map<String, Any?>)["mobilesdk_app_id"] as String,
        "API_KEY" to ((client["api_key"] as List<Map<String, Any?>>).first()["current_key"] as String),
        "PROJECT_ID" to project["project_id"] as String,
        "SENDER_ID" to project["project_number"] as String,
    )
}
