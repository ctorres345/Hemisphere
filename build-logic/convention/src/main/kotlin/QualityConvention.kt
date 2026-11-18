import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
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
    // On JVM modules the plugin adds `detektMain` (with types) but leaves it out of `check`.
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        tasks.named("check").configure { dependsOn("detektMain") }
        limitPlainDetektToTests()
    }

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
                source(listOf("main", "debug").flatMap { listOf("src/$it/kotlin", "src/$it/java") }.map(layout.projectDirectory::dir))
                classpath.setFrom(variant.compileClasspath, components.sdkComponents.bootClasspath)
                config.setFrom(configFiles)
                buildUponDefaultConfig = true
                reports.sarif.required.set(false)
            }
        tasks.named("check").configure { dependsOn(task) }
        registerDetektFixture(configFiles, variant.compileClasspath, components.sdkComponents.bootClasspath)
    }
    limitPlainDetektToTests()
}

/**
 * Proves the forbidden-value rules fire. A module may keep `src/detektFixture/kotlin/`: code that is
 * never compiled, where each line ending in `// expect: RuleId` must be reported by detekt with the
 * module's config. `check` fails when one is not, so a typo or a dropped rule cannot pass quietly.
 */
private fun Project.registerDetektFixture(
    configFiles: List<java.io.File>,
    compileClasspath: org.gradle.api.file.FileCollection,
    bootClasspath: org.gradle.api.provider.Provider<List<org.gradle.api.file.RegularFile>>,
) {
    val fixtureDir = layout.projectDirectory.dir("src/detektFixture/kotlin")
    if (!fixtureDir.asFile.isDirectory) return
    val report = layout.buildDirectory.file("reports/detekt/fixture.xml")
    val detekt =
        tasks.register<Detekt>("detektFixture") {
            description = "Runs detekt on src/detektFixture, where every marked line must be reported."
            source(fixtureDir)
            classpath.setFrom(compileClasspath, bootClasspath)
            config.setFrom(configFiles)
            buildUponDefaultConfig = true
            ignoreFailures = true
            reports.xml.required.set(true)
            reports.xml.outputLocation.set(report)
            reports.html.required.set(false)
            reports.sarif.required.set(false)
            reports.txt.required.set(false)
            reports.md.required.set(false)
        }
    val verify =
        tasks.register("verifyDetektFixture") {
            description = "Fails unless detekt reported every `// expect:` line in src/detektFixture."
            dependsOn(detekt)
            val sources = fixtureDir.asFileTree
            inputs.files(sources)
            inputs.file(report)
            doLast { checkDetektFixture(sources.files, report.get().asFile) }
        }
    tasks.named("check").configure { dependsOn(verify) }
}

private val EXPECT = Regex("""//\s*expect:\s*(\w+)\s*$""")

private fun checkDetektFixture(
    sources: Set<java.io.File>,
    report: java.io.File,
) {
    val xml = report.readText()
    val missing =
        sources.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val rule = EXPECT.find(line)?.groupValues?.get(1) ?: return@mapIndexedNotNull null
                val reported =
                    Regex("""<error line="${index + 1}"[^>]*source="detekt\.$rule"""").containsMatchIn(
                        xml.substringAfter("name=\"${file.absolutePath}\"").substringBefore("</file>"),
                    )
                if (reported) null else "${file.name}:${index + 1} expected $rule"
            }
        }
    check(missing.isEmpty()) { "detekt did not report:\n" + missing.joinToString("\n") }
}

/** The plain `detekt` task would analyse main sources a second time without types; keep it on tests. */
private fun Project.limitPlainDetektToTests() {
    tasks.named<Detekt>("detekt").configure {
        setSource(files("src/test/kotlin", "src/test/java"))
    }
}
