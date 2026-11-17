import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Project

internal fun Project.javaVersion(): JavaVersion = JavaVersion.toVersion(libs.version("jdk"))

/** Android Lint is part of `./gradlew check`; a warning fails the build. */
internal fun Lint.configureHemisphereLint() {
    abortOnError = true
    warningsAsErrors = true
    checkDependencies = false
    // Dependabot owns version bumps; these would fail the build on every new release.
    disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    // A library's own lint jar built for another lint version is skipped with this error. It reports
    // the library, not our code, so it is shown but does not fail the build.
    informational += "ObsoleteLintCustomCheck"
}
