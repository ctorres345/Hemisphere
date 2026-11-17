import ModuleGraphRules.Edge
import ModuleGraphRules.Scope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleGraphRulesTest {
    private val all = ModuleGraphRules.allowed.keys
    private val required = listOf(Edge(":core:domain", Scope.API, ":core:model", "api"))

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
        val errors = ModuleGraphRules.violations(all, emptyList(), emptyList())
        assertEquals(listOf(":core:domain must declare :core:model as api (docs/architecture.md §2)."), errors)
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
}
