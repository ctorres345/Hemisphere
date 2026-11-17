import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/** The `app` module. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.configure<ApplicationExtension> {
                namespace = "io.github.ctorres345.hemisphere"
                compileSdk = libs.version("android-compileSdk").toInt()
                defaultConfig {
                    applicationId = "io.github.ctorres345.hemisphere"
                    minSdk = libs.version("android-minSdk").toInt()
                    targetSdk = libs.version("android-targetSdk").toInt()
                }
                compileOptions {
                    sourceCompatibility = javaVersion()
                    targetCompatibility = javaVersion()
                }
                buildFeatures.compose = true
                // F-Droid rebuilds the APK and compares it with ours (ADR 0003). The dependency-info block
                // is signed with a Google key, so it would never match and F-Droid flags it.
                dependenciesInfo {
                    includeInApk = false
                    includeInBundle = false
                }
                lint { configureHemisphereLint() }
            }
            configureKotlin()
            configureQuality()
            configureComposeDependencies()
            registerNoInternetPermissionCheck()
        }
    }
}

/** ADR 0008: no INTERNET permission in v1, even one a library merges in. Runs from `./gradlew check`. */
private fun Project.registerNoInternetPermissionCheck() {
    extensions.configure<ApplicationAndroidComponentsExtension> {
        onVariants { variant ->
            val name = variant.name.replaceFirstChar { it.uppercase() }
            val task = tasks.register<NoInternetPermissionTask>("check${name}NoInternetPermission") {
                mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                marker.set(layout.buildDirectory.file("reports/manifest/${variant.name}-no-internet.txt"))
            }
            tasks.named("check").configure { dependsOn(task) }
        }
    }
}
