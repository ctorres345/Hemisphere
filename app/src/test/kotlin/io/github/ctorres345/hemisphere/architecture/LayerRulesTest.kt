package io.github.ctorres345.hemisphere.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The layer rules from docs/code-guidelines.md that Gradle cannot check (ADR 0009). Gradle's
 * module-graph check covers which module may depend on which; these check the code inside.
 */
class LayerRulesTest {
    private val production = Konsist.scopeFromProduction()
    private val domain = production.slice { "/core/domain/src/main/" in it.path }
    private val features = production.slice { "/feature/" in it.path }
    private val tests = Konsist.scopeFromTest()

    @Test
    fun `core domain imports nothing from android or Compose`() {
        domain.files.assertFalse(strict = false) { file ->
            file.imports.any { it.name.startsWith("android.") || it.name.startsWith("androidx.compose.") }
        }
    }

    @Test
    fun `the file-system provider interfaces use no Android types`() {
        val contract = production.files.filter { "/core/fs/src/main/" in it.path && it.name in providerContractFiles }
        assertEquals(providerContractFiles, contract.map { it.name }.toSet())
        contract.assertFalse(strict = false) { file ->
            file.imports.any { it.name.startsWith("android.") || it.name.startsWith("androidx.") } ||
                androidName.containsMatchIn(
                    file.text
                        .lines()
                        .filterNot { it.trimStart().startsWith("*") }
                        .joinToString("\n"),
                )
        }
    }

    @Test
    fun `a use case is a VerbNounUseCase with one public operator invoke and no mutable fields`() {
        domain.classes().withNameEndingWith("UseCase").assertTrue(strict = false) { useCase ->
            val publicFunctions = useCase.functions().filter { it.hasPublicOrDefaultModifier }
            useCaseName.matches(useCase.name) &&
                publicFunctions.size == 1 &&
                publicFunctions.single().let { it.name == "invoke" && it.hasOperatorModifier } &&
                useCase.properties().none { it.isVar }
        }
    }

    @Test
    fun `a ViewModel takes only use cases, SavedStateHandle and assisted values`() {
        // The abstract MviViewModel base takes the initial state and reducer from its subclasses.
        screenViewModels().assertTrue(strict = false) { viewModel ->
            viewModel.primaryConstructor?.parameters.orEmpty().all { parameter ->
                parameter.type.name.endsWith("UseCase") ||
                    parameter.type.name == "SavedStateHandle" ||
                    parameter.hasAnnotationWithName("Assisted")
            }
        }
    }

    @Test
    fun `a ViewModel has no public function but onIntent`() {
        production.classes().withNameEndingWith("ViewModel").assertTrue(strict = false) { viewModel ->
            viewModel.functions().filter { it.hasPublicOrDefaultModifier }.all { it.name == "onIntent" }
        }
    }

    @Test
    fun `every screen ViewModel extends the shared MviViewModel base`() {
        screenViewModels().assertTrue(strict = false) { viewModel ->
            viewModel.parents().any { it.name.substringBefore('<').trim() == "MviViewModel" } &&
                viewModel.containingFile.imports.any { it.name == MVI_BASE }
        }
    }

    @Test
    fun `every screen ViewModel has a reducer test and a ViewModel test`() {
        val tested = testClassesWithTests()
        screenViewModels().assertTrue(strict = false) { viewModel ->
            val screen = viewModel.name.removeSuffix("ViewModel")
            "${screen}ReducerTest" in tested && "${screen}ViewModelTest" in tested
        }
    }

    @Test
    fun `every use case has a test`() {
        val tested = testClassesWithTests()
        domain.classes().withNameEndingWith("UseCase").assertTrue(strict = false) { useCase ->
            "${useCase.name}Test" in tested
        }
    }

    /** Names of test classes that hold at least one test, so an empty placeholder class does not count. */
    private fun testClassesWithTests(): Set<String> =
        tests
            .classes()
            .filter { testClass -> testClass.functions().any { it.hasAnnotationWithName("Test") } }
            .map { it.name }
            .toSet()

    @Test
    fun `tests use fakes, not a mocking library`() {
        tests.files.assertFalse(strict = false) { file ->
            file.imports.any { it.name.startsWith("io.mockk") || it.name.startsWith("org.mockito") }
        }
    }

    @Test
    fun `screen ViewModels come from Hilt, not a hand-written container`() {
        production.classes().assertFalse(strict = false) { it.name == "AppContainer" }
        production.files.assertFalse(strict = false) { file ->
            file.imports.any { it.name in handWiredViewModelImports }
        }
        screenViewModels().assertTrue(strict = false) { it.hasAnnotationWithName("HiltViewModel") }
    }

    @Test
    fun `intent types carry the ViewModel prefix`() {
        production.classesAndInterfaces().assertFalse(strict = false) { it.name == "Intent" }
    }

    @Test
    fun `features never read raw gamepad key codes`() {
        features.files.assertFalse(strict = false) { it.text.contains("KEYCODE_BUTTON_") }
    }

    private fun screenViewModels() =
        production.classes().withNameEndingWith("ViewModel").filterNot {
            it.hasAbstractModifier
        }

    private companion object {
        /** These stay plain Kotlin (ADR 0006); `ShareUriProvider` is the Android side. */
        val providerContractFiles = setOf("FileSystemProvider", "Streams", "VolumeRepository")

        /** A fully qualified Android name in code (KDoc lines are skipped), which imports alone would miss. */
        val androidName = Regex("""\bandroidx?\.[a-z]""")

        /** Hand wiring, or plain `viewModel()`, which cannot build an `@HiltViewModel` and crashes at runtime. */
        val handWiredViewModelImports =
            setOf("androidx.lifecycle.viewmodel.viewModelFactory", "androidx.lifecycle.viewmodel.compose.viewModel")

        const val MVI_BASE = "io.github.ctorres345.hemisphere.core.mvi.MviViewModel"

        /** A verb, at least one noun word, then UseCase: `GetListingUseCase`, `EnqueueCopyUseCase`. */
        val useCaseName = Regex("""[A-Z][a-z]+(?:[A-Z][a-z0-9]*)+UseCase""")
    }
}
