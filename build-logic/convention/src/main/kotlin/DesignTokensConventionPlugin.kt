import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Generates `ThemeTokens.kt` from the repo's `tokens/` folder into the module's Kotlin sources, in
 * the module's namespace, for every variant. Applied by `core:designsystem` (docs/design-system.md §11).
 */
class DesignTokensConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val generate =
                tasks.register<GenerateThemeTokensTask>("generateThemeTokens") {
                    tokensDir.set(rootProject.layout.projectDirectory.dir("tokens"))
                    packageName.set(hemisphereNamespace)
                }
            extensions.configure<LibraryAndroidComponentsExtension> {
                onVariants { variant ->
                    variant.sources.kotlin?.addGeneratedSourceDirectory(generate, GenerateThemeTokensTask::outputDir)
                }
            }
        }
    }
}

@CacheableTask
abstract class GenerateThemeTokensTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokensDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val source = ThemeTokensCodegen.generate(tokensDir.get().asFile, packageName.get())
        val dir = outputDir.get().asFile.resolve(packageName.get().replace('.', '/'))
        outputDir.get().asFile.deleteRecursively()
        dir.mkdirs()
        dir.resolve("ThemeTokens.kt").writeText(source)
    }
}
