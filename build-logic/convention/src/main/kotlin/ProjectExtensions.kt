import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

/** `:core:fs` becomes `io.github.ctorres345.hemisphere.core.fs` (architecture §2: packages follow modules). */
internal val Project.hemisphereNamespace: String
    get() = "io.github.ctorres345.hemisphere" + path.replace(':', '.')
