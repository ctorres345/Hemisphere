import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/** One JDK for every module, locally and in CI (ADR 0003: pinned toolchain). */
internal fun Project.configureKotlin() {
    extensions.configure<KotlinProjectExtension> {
        jvmToolchain(libs.version("jdk").toInt())
    }
    dependencies {
        add("testImplementation", libs.library("junit"))
    }
}
