import org.gradle.api.Plugin
import org.gradle.api.Project

/** A pure Kotlin/JVM module, such as `core:model`. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlin()
            configureQuality()
        }
    }
}
