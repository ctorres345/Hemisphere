import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class NoInternetPermissionTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val marker: RegularFileProperty

    @TaskAction
    fun check() {
        val manifest = mergedManifest.get().asFile.readText()
        if (ManifestRules.requestsInternet(manifest)) {
            throw GradleException(
                "The merged manifest requests android.permission.INTERNET, which ADR 0008 forbids in v1. " +
                    "Find the library that adds it (build/intermediates/.../manifest-merger report) and remove it, " +
                    "or strip it with tools:node=\"remove\".",
            )
        }
        marker.get().asFile.writeText("No INTERNET permission.\n")
    }
}

internal object ManifestRules {
    private val internet = Regex("""<uses-permission[^>]*android:name\s*=\s*"android\.permission\.INTERNET"""")

    fun requestsInternet(manifest: String): Boolean = internet.containsMatchIn(manifest)
}
