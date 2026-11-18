import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Roborazzi screenshot tests for a Compose library (HEM-13, moved here in HEM-33): Robolectric renders
 * Compose on the JVM and Roborazzi compares the images with the goldens in `src/test/screenshots/`.
 *
 * The goldens are Linux renders, recorded by CI (the `android` workflow with `record-screenshots`).
 * macOS anti-aliases shape edges differently, so `check` compares them only on Linux, which is where
 * CI's gradle-check runs.
 */
class ScreenshotConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")
            extensions.configure<LibraryExtension> {
                // Robolectric reads the merged resources (strings, theme) for the screenshot tests.
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            dependencies {
                add("testImplementation", libs.library("robolectric"))
                add("testImplementation", libs.library("roborazzi"))
                add("testImplementation", libs.library("roborazzi-compose"))
                add("testImplementation", platform(libs.library("compose-bom")))
                add("testImplementation", libs.library("compose-ui-test-junit4"))
                // Registers the empty activity createComposeRule() starts, for Robolectric's merged test manifest.
                add("testImplementation", libs.library("compose-ui-test-manifest"))
            }
            // The golden images are test inputs, so changing one re-runs the tests instead of reusing a cached result.
            tasks.withType<Test>().configureEach {
                inputs
                    .files(fileTree("src/test/screenshots"))
                    .withPropertyName("screenshotGoldens")
                    .withPathSensitivity(PathSensitivity.RELATIVE)
            }
            if (System.getProperty("os.name").startsWith("Linux")) {
                tasks.named("check") { dependsOn("verifyRoborazziDebug") }
            }
        }
    }
}
