import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/**
 * Applied to the root project. Adds `checkModuleGraph`, run by `./gradlew check`, which fails on any
 * dependency docs/architecture.md §2 does not allow. It also runs build-logic's own tests.
 */
class ModuleGraphConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) { "hemisphere.module.graph goes on the root project only." }
        target.pluginManager.apply("base")
        val task =
            target.tasks.register<ModuleGraphCheckTask>("checkModuleGraph") {
                group = "verification"
                description = "Fails on a module dependency that docs/architecture.md §2 does not allow."
                report.set(target.layout.buildDirectory.file("reports/module-graph.txt"))
            }
        target.tasks.named("check").configure {
            dependsOn(task)
            dependsOn(target.gradle.includedBuild("build-logic").task(":convention:check"))
        }
        target.gradle.projectsEvaluated {
            val modules = target.subprojects.filter { it.buildFile.exists() }
            val edges = modules.flatMap { it.declaredEdges() }
            val compose = modules.filter { it.path in ModuleGraphRules.noCompose }.flatMap { it.composeUses() }
            val errors = ModuleGraphRules.violations(modules.map { it.path }, edges, compose)
            task.configure { violations.set(errors) }
        }
    }
}

private fun Project.declaredEdges(): List<ModuleGraphRules.Edge> =
    configurations.flatMap { configuration ->
        val scope = ModuleGraphRules.scopeOf(configuration.name) ?: return@flatMap emptyList()
        configuration.dependencies.withType(ProjectDependency::class.java).map {
            ModuleGraphRules.Edge(path, scope, it.path, configuration.name)
        }
    }

private fun Project.composeUses(): List<String> {
    val plugin = if (pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.compose")) listOf("$path (Compose compiler plugin)") else emptyList()
    val libraries =
        configurations.flatMap { configuration ->
            configuration.dependencies.withType(ExternalDependency::class.java)
                // activity-compose, lifecycle-viewmodel-compose and the like pull Compose in from other groups.
                .filter { it.group?.startsWith("androidx.compose") == true || "compose" in it.name }
                .map { "$path -> ${it.group}:${it.name} (${configuration.name})" }
        }
    return plugin + libraries
}

abstract class ModuleGraphCheckTask : DefaultTask() {
    /** Computed from the evaluated projects; the task only reports them. */
    @get:Input
    abstract val violations: ListProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val errors = violations.get()
        val text = if (errors.isEmpty()) "Module graph matches docs/architecture.md §2.\n" else errors.joinToString("\n", postfix = "\n")
        report.get().asFile.writeText(text)
        if (errors.isNotEmpty()) {
            throw GradleException("Module graph check failed:\n" + errors.joinToString("\n") { "  - $it" })
        }
    }
}
