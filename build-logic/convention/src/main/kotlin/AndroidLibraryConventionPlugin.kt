import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** An Android library without Compose: the data and domain layers. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> {
                namespace = hemisphereNamespace
                compileSdk = libs.version("android-compileSdk").toInt()
                defaultConfig.minSdk = libs.version("android-minSdk").toInt()
                compileOptions {
                    sourceCompatibility = javaVersion()
                    targetCompatibility = javaVersion()
                }
                lint { configureHemisphereLint() }
            }
            configureKotlin()
            configureQuality()
        }
    }
}
