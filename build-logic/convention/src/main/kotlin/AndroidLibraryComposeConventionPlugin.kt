import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** An Android library with Compose: `core:designsystem`, `core:input` and every `feature:*` module. */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("hemisphere.android.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.configure<LibraryExtension> {
                buildFeatures.compose = true
            }
            configureComposeDependencies()
        }
    }
}

internal fun Project.configureComposeDependencies() {
    dependencies {
        val bom = platform(libs.library("compose-bom"))
        add("implementation", bom)
        add("implementation", libs.library("compose-ui-tooling-preview"))
        add("debugImplementation", libs.library("compose-ui-tooling"))
        // slack/compose-lints: the Compose rules in docs/code-guidelines.md (stateless composables, Modifier order).
        add("lintChecks", libs.library("compose-lints"))
    }
}
