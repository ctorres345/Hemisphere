/**
 * The allowed module graph from docs/architecture.md §2, and the check that compares the real graph
 * against it. A new module (for example `core:archive`) is added here, to [allowed] and, when §2
 * states one of its edges as a rule, to [required], in the same PR as the module.
 */
internal object ModuleGraphRules {
    private const val MODEL = ":core:model"
    private const val TESTING = ":core:testing"
    private const val FS_TESTING = ":core:fs-testing"
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
            ":core:jobs" to setOf(MODEL, ":core:fs", ":core:data", FS_TESTING),
            ":core:data" to setOf(MODEL, ":core:fs", FS_TESTING),
            FS_TESTING to setOf(MODEL, ":core:fs"),
            TESTING to setOf(MODEL, FS_TESTING) + dataLayer,
            ":feature:browser" to featureDeps,
            ":feature:jobs" to featureDeps - ":core:platform",
            ":feature:settings" to featureDeps - ":core:platform",
            ":feature:trash" to featureDeps - ":core:platform",
        )

    /**
     * `core:testing` and every module it is built from (`core:fs-testing` is one). The convention plugins add `core:testing` to
     * every other module's tests; adding it here would put a module's own classes on its test
     * classpath twice, or make `core:testing` depend on itself.
     */
    val withoutSharedTesting: Set<String> =
        generateSequence(setOf(TESTING)) { found ->
            (found + found.flatMap { allowed[it].orEmpty() }).takeIf { it != found }
        }.last()

    /** Modules that must stay free of Compose (architecture §2) and are tested on the JVM. */
    val noCompose = setOf(MODEL, ":core:domain", MVI, FS_TESTING) + dataLayer

    /** `core:domain` exposes only `core:model` as `api`, so the data layer stays off a feature's classpath. */
    private val apiOnly = mapOf(":core:domain" to setOf(MODEL))

    enum class Scope(val declaration: String) { API("api"), IMPLEMENTATION("implementation"), TEST("testImplementation") }

    data class Edge(val from: String, val scope: Scope, val to: String, val configuration: String)

    /** An edge §2 states as a rule, so it must be declared, not just allowed. */
    data class Required(val from: String, val scope: Scope, val to: String)

    /**
     * The edges §2 requires. The table's "Depends on" column lists what a module may use, so a
     * module that does not use a listed module yet is fine; these come from the rules under it:
     * every data-layer module, `core:platform` and `core:domain` expose `core:model` as `api`;
     * `core:domain` takes the data layer as `implementation`; `core:testing` exposes it, and
     * `core:fs-testing`, as `api`.
     * A module added to `dataLayer` therefore needs those edges in the same PR. Only the plain
     * configuration counts: `debugApi` or `compileOnly` does not meet the rule.
     */
    val required: Set<Required> =
        (dataLayer + ":core:platform" + ":core:domain").map { Required(it, Scope.API, MODEL) }.toSet() +
            dataLayer.map { Required(":core:domain", Scope.IMPLEMENTATION, it) } +
            (dataLayer + FS_TESTING).map { Required(TESTING, Scope.API, it) }

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
                edge.to == TESTING && edge.from in withoutSharedTesting ->
                    errors += "$where: core:testing is built from ${edge.from}, so its tests cannot use it."
                edge.to == TESTING -> Unit
                edge.to == FS_TESTING && edge.scope != Scope.TEST && edge.from != TESTING ->
                    errors += "$where: core:fs-testing is for test source sets only (testImplementation); only core:testing re-exports it."
                rule != null && edge.to !in rule ->
                    errors += "$where: not allowed by docs/architecture.md §2."
                edge.scope == Scope.API && apiOnly[edge.from]?.contains(edge.to) == false ->
                    errors += "$where: ${edge.from} may expose only ${apiOnly.getValue(edge.from)} as api."
            }
        }
        required
            .filter { need ->
                need.from in projects &&
                    edges.none { it.from == need.from && it.to == need.to && it.configuration == need.scope.declaration }
            }
            .forEach { errors += "${it.from} must declare ${it.to} as ${it.scope.declaration} (docs/architecture.md §2)." }
        composeUsers.distinct().sorted().forEach { line ->
            errors += "$line: Compose is not allowed in ${noCompose.sorted().joinToString()}."
        }
        return errors
    }
}
