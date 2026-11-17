import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Hilt for an Android module (architecture §9): the Hilt plugin, KSP, and the runtime and compiler.
 * Apply it to the app, to each data-layer module with a Hilt `@Module`, to `core:domain` (use cases
 * have `@Inject` constructors) and to features with a `@HiltViewModel`.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")
            dependencies {
                add("implementation", libs.library("hilt-android"))
                add("ksp", libs.library("hilt-compiler"))
            }
        }
    }
}
