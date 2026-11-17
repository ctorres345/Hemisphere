import ModuleGraphRules.Edge
import ModuleGraphRules.Scope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleGraphRulesTest {
    private val all = ModuleGraphRules.allowed.keys
    private val required = ModuleGraphRules.required.map { Edge(it.from, it.scope, it.to, it.scope.declaration) }

    private fun violations(vararg edges: Edge, compose: List<String> = emptyList()) =
        ModuleGraphRules.violations(all, required + edges, compose)

    @Test
    fun `the architecture graph passes`() {
        val edges =
            arrayOf(
                Edge(":feature:browser", Scope.IMPLEMENTATION, ":core:domain", "implementation"),
                Edge(":feature:browser", Scope.IMPLEMENTATION, ":core:platform", "implementation"),
                Edge(":feature:browser", Scope.TEST, ":core:testing", "testImplementation"),
                Edge(":core:domain", Scope.IMPLEMENTATION, ":core:fs", "implementation"),
                Edge(":core:testing", Scope.API, ":core:jobs", "api"),
                Edge(":app", Scope.IMPLEMENTATION, ":core:fs", "implementation"),
            )
        assertEquals(emptyList<String>(), violations(*edges))
    }

    @Test
    fun `a feature on a feature fails`() {
        val errors = violations(Edge(":feature:browser", Scope.IMPLEMENTATION, ":feature:trash", "implementation"))
        assertEquals(1, errors.size)
        assertTrue(errors.single().startsWith(":feature:browser -> :feature:trash"))
    }

    @Test
    fun `a feature on the data layer fails`() {
        assertEquals(1, violations(Edge(":feature:trash", Scope.IMPLEMENTATION, ":core:fs", "implementation")).size)
    }

    @Test
    fun `core testing outside a test source set fails`() {
        assertEquals(1, violations(Edge(":feature:trash", Scope.IMPLEMENTATION, ":core:testing", "implementation")).size)
    }

    @Test
    fun `core domain exposing the data layer as api fails`() {
        assertEquals(1, violations(Edge(":core:domain", Scope.API, ":core:jobs", "api")).size)
    }

    @Test
    fun `core domain must expose core model as api`() {
        val errors = ModuleGraphRules.violations(all, required.filterNot { it.from == ":core:domain" && it.to == ":core:model" }, emptyList())
        assertEquals(listOf(":core:domain must declare :core:model as api (docs/architecture.md §2)."), errors)
    }

    @Test
    fun `removing any required edge fails with that edge named`() {
        required.forEach { missing ->
            val errors = ModuleGraphRules.violations(all, required - missing, emptyList())
            assertEquals(
                listOf("${missing.from} must declare ${missing.to} as ${missing.configuration} (docs/architecture.md §2)."),
                errors,
            )
        }
    }

    @Test
    fun `a required edge declared with another scope still fails`() {
        val asImplementation = Edge(":core:fs", Scope.IMPLEMENTATION, ":core:model", "implementation")
        val edges = required.filterNot { it.from == ":core:fs" && it.to == ":core:model" } + asImplementation
        assertEquals(1, ModuleGraphRules.violations(all, edges, emptyList()).size)
    }

    @Test
    fun `a required edge declared only for one variant still fails`() {
        val debugOnly = Edge(":core:fs", Scope.API, ":core:model", "debugApi")
        val edges = required.filterNot { it.from == ":core:fs" && it.to == ":core:model" } + debugOnly
        assertEquals(1, ModuleGraphRules.violations(all, edges, emptyList()).size)
    }

    @Test
    fun `the required set is the edges section 2 states`() {
        val model = listOf(":core:fs", ":core:jobs", ":core:data", ":core:platform", ":core:domain")
        val dataLayer = listOf(":core:fs", ":core:jobs", ":core:data")
        val expected =
            model.map { ModuleGraphRules.Required(it, Scope.API, ":core:model") } +
                dataLayer.map { ModuleGraphRules.Required(":core:domain", Scope.IMPLEMENTATION, it) } +
                dataLayer.map { ModuleGraphRules.Required(":core:testing", Scope.API, it) }
        assertEquals(expected.toSet(), ModuleGraphRules.required)
    }

    @Test
    fun `an edge the table only allows is not required`() {
        // feature:jobs lists core:mvi in the table but does not use it yet.
        assertTrue(ModuleGraphRules.required.none { it.from.startsWith(":feature:") || it.from == ":app" })
    }

    @Test
    fun `required edges are allowed edges`() {
        ModuleGraphRules.required.forEach { assertTrue("$it", it.to in ModuleGraphRules.allowed.getValue(it.from).orEmpty()) }
    }

    @Test
    fun `compose in a no-compose module fails`() {
        assertEquals(1, violations(compose = listOf(":core:fs (Compose compiler plugin)")).size)
    }

    @Test
    fun `an unknown module fails`() {
        val errors = ModuleGraphRules.violations(all + ":core:archive", required, emptyList())
        assertTrue(errors.single().startsWith(":core:archive is not in the module table"))
    }

    @Test
    fun `configurations map to scopes`() {
        assertEquals(Scope.API, ModuleGraphRules.scopeOf("api"))
        assertEquals(Scope.API, ModuleGraphRules.scopeOf("debugApi"))
        assertEquals(Scope.IMPLEMENTATION, ModuleGraphRules.scopeOf("implementation"))
        assertEquals(Scope.IMPLEMENTATION, ModuleGraphRules.scopeOf("releaseCompileOnly"))
        assertEquals(Scope.TEST, ModuleGraphRules.scopeOf("testImplementation"))
        assertEquals(Scope.TEST, ModuleGraphRules.scopeOf("androidTestImplementation"))
        assertEquals(null, ModuleGraphRules.scopeOf("lintChecks"))
        assertEquals(null, ModuleGraphRules.scopeOf("detekt"))
    }

    @Test
    fun `core testing is not added to itself or to a module it is built from`() {
        // Pinned: widening what core:testing may use silently takes core:testing away from more modules.
        assertEquals(
            setOf(":core:testing", ":core:model", ":core:fs", ":core:jobs", ":core:data", ":core:platform"),
            ModuleGraphRules.withoutSharedTesting,
        )
    }

    @Test
    fun `every other module gets core testing`() {
        assertEquals(
            setOf(
                ":app",
                ":core:designsystem",
                ":core:domain",
                ":core:input",
                ":core:mvi",
                ":feature:browser",
                ":feature:jobs",
                ":feature:settings",
                ":feature:trash",
            ),
            all - ModuleGraphRules.withoutSharedTesting,
        )
    }

    @Test
    fun `a module core testing is built from fails when its tests use core testing`() {
        val errors = violations(Edge(":core:fs", Scope.TEST, ":core:testing", "testImplementation"))
        assertEquals(1, errors.size)
        assertTrue(errors.single().startsWith(":core:fs -> :core:testing"))
    }
}
