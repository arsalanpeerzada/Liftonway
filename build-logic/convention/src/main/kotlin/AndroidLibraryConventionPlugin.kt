import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** An Android library: SDK levels, Java 17, and JUnit with resources available to unit tests. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            compileSdk = Android.COMPILE_SDK
            defaultConfig.minSdk = Android.MIN_SDK
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            // Robolectric Compose UI tests need the module's resources.
            testOptions.unitTests.isIncludeAndroidResources = true
        }
        dependencies {
            add("testImplementation", libs.lib("junit"))
        }
    }
}
