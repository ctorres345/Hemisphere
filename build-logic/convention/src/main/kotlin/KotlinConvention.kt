import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * One JDK for every module, locally and in CI (ADR 0003: pinned toolchain), and the shared test
 * libraries: JUnit, coroutines-test and `core:testing` (architecture §10).
 */
internal fun Project.configureKotlin() {
    extensions.configure<KotlinProjectExtension> {
        jvmToolchain(libs.version("jdk").toInt())
    }
    dependencies {
        add("testImplementation", libs.library("junit"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        // core:testing is an Android library, so only Android modules can take it.
        val isAndroid = pluginManager.hasPlugin("com.android.base")
        if (isAndroid && path !in ModuleGraphRules.withoutSharedTesting) {
            add("testImplementation", project(":core:testing"))
        }
    }
}
