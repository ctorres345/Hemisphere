import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * ktlint and detekt for every module (docs/code-guidelines.md). Both run from `./gradlew check`.
 *
 * Detekt reads `config/detekt/detekt.yml`, then `config/detekt/<module>.yml` when it exists
 * (`core-fs.yml` for `:core:fs`), so a module can lift one rule: `core:fs` may use `java.io.File`.
 */
internal fun Project.configureQuality() {
    pluginManager.apply("io.gitlab.arturbosch.detekt")
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")

    val configDir = rootProject.layout.projectDirectory.dir("config/detekt")
    val moduleConfig = configDir.file(path.removePrefix(":").replace(':', '-') + ".yml").asFile
    val configFiles = listOfNotNull(configDir.file("detekt.yml").asFile, moduleConfig.takeIf { it.exists() })
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(configFiles)
    }
    // detekt 1.23.8 runs its own Kotlin compiler and refuses any other Kotlin version on its classpath.
    val detektKotlin = libs.version("detekt-kotlin")
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion(detektKotlin)
        }
    }
    tasks.withType<Detekt>().configureEach {
        jvmTarget = libs.version("jdk")
    }

    pluginManager.withPlugin("com.android.base") { registerTypedDetekt(configFiles) }

    extensions.configure<KtlintExtension> {
        version.set(libs.version("ktlint"))
    }
}

/**
 * The detekt plugin adds type resolution (`detektMain`) only for the Kotlin JVM and Kotlin Android
 * plugins. AGP 9 compiles Kotlin itself, so Android modules get this task instead. Without it, the
 * rules that need types (`ForbiddenMethodCall`, `InjectDispatcher`, `SuspendFunSwallowedCancellation`)
 * would silently never run.
 */
private fun Project.registerTypedDetekt(configFiles: List<java.io.File>) {
    val components = extensions.getByType(AndroidComponentsExtension::class.java)
    components.onVariants(components.selector().withName("debug")) { variant ->
        val task =
            tasks.register<Detekt>("detektDebugTyped") {
                description = "Runs detekt with type resolution on the debug variant."
                source(layout.projectDirectory.dir("src/main/kotlin"), layout.projectDirectory.dir("src/debug/kotlin"))
                classpath.setFrom(variant.compileClasspath, components.sdkComponents.bootClasspath)
                config.setFrom(configFiles)
                buildUponDefaultConfig = true
                reports.sarif.required.set(false)
            }
        tasks.named("check").configure { dependsOn(task) }
    }
}
