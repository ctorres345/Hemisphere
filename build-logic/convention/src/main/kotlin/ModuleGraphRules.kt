/**
 * The allowed module graph from docs/architecture.md §2, and the check that compares the real graph
 * against it. A new module (for example `core:archive`) is added here in the same PR as the module.
 */
internal object ModuleGraphRules {
    private const val MODEL = ":core:model"
    private const val TESTING = ":core:testing"
    private val dataLayer = setOf(":core:fs", ":core:jobs", ":core:data")
    private const val MVI = ":core:mvi"
    private val featureDeps = setOf(":core:domain", ":core:designsystem", ":core:input", ":core:platform", MVI)

    /** Module path to the modules its production code may depend on. `null` means any module (`app`). */
    val allowed: Map<String, Set<String>?> =
        mapOf(
            ":app" to null,
            MODEL to emptySet(),
            ":core:designsystem" to emptySet(),
            ":core:input" to setOf(MODEL, ":core:designsystem", ":core:data"),
            ":core:platform" to setOf(MODEL),
            MVI to emptySet(),
            ":core:domain" to setOf(MODEL) + dataLayer,
            ":core:fs" to setOf(MODEL, ":core:platform"),
            ":core:jobs" to setOf(MODEL, ":core:fs", ":core:data"),
            ":core:data" to setOf(MODEL, ":core:fs"),
            TESTING to setOf(MODEL) + dataLayer,
            ":feature:browser" to featureDeps,
            ":feature:jobs" to featureDeps - ":core:platform",
            ":feature:settings" to featureDeps - ":core:platform",
            ":feature:trash" to featureDeps - ":core:platform",
        )

    /** Modules that must stay free of Compose (architecture §2) and are tested on the JVM. */
    val noCompose = setOf(MODEL, ":core:domain", MVI) + dataLayer

    /** `core:domain` exposes only `core:model` as `api`, so the data layer stays off a feature's classpath. */
    private val apiOnly = mapOf(":core:domain" to setOf(MODEL))

    enum class Scope { API, IMPLEMENTATION, TEST }

    data class Edge(val from: String, val scope: Scope, val to: String, val configuration: String)

    /**
     * The scope of a declarable configuration such as `implementation`, `debugApi` or `testImplementation`,
     * or `null` for one the graph ignores (`lintChecks`, `detekt`, `ktlint`).
     */
    fun scopeOf(configuration: String): Scope? {
        val lower = configuration.lowercase()
        val isDeclarable = listOf("api", "implementation", "compileonly", "runtimeonly").any { lower.endsWith(it) }
        return when {
            !isDeclarable -> null
            "test" in lower -> Scope.TEST
            lower.endsWith("api") -> Scope.API
            else -> Scope.IMPLEMENTATION
        }
    }

    /** Every broken rule as one line, empty when the graph matches the architecture. */
    fun violations(
        projects: Collection<String>,
        edges: Collection<Edge>,
        composeUsers: Collection<String>,
    ): List<String> {
        val errors = mutableListOf<String>()
        (projects - allowed.keys).sorted().forEach {
            errors += "$it is not in the module table. Add it to docs/architecture.md §2 and ModuleGraphRules."
        }
        (allowed.keys - projects.toSet()).sorted().forEach {
            errors += "$it is in the module table but not in settings.gradle.kts."
        }
        edges.filter { it.from != it.to }.forEach { edge ->
            val rule = allowed[edge.from]
            val where = "${edge.from} -> ${edge.to} (${edge.configuration})"
            when {
                edge.to == TESTING && edge.scope != Scope.TEST ->
                    errors += "$where: core:testing is for test source sets only (testImplementation)."
                edge.to == TESTING -> Unit
                rule != null && edge.to !in rule ->
                    errors += "$where: not allowed by docs/architecture.md §2."
                edge.scope == Scope.API && apiOnly[edge.from]?.contains(edge.to) == false ->
                    errors += "$where: ${edge.from} may expose only ${apiOnly.getValue(edge.from)} as api."
            }
        }
        apiOnly.forEach { (module, api) ->
            api.filter { target -> edges.none { it.from == module && it.to == target && it.scope == Scope.API } }
                .forEach { errors += "$module must declare $it as api (docs/architecture.md §2)." }
        }
        composeUsers.distinct().sorted().forEach { line ->
            errors += "$line: Compose is not allowed in ${noCompose.sorted().joinToString()}."
        }
        return errors
    }
}
