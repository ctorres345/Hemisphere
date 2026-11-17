import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Project

internal fun Project.javaVersion(): JavaVersion = JavaVersion.toVersion(libs.version("jdk"))

/** SDK, Java level and lint shared by the app and every Android library. */
internal fun Project.configureAndroidCommon(android: CommonExtension) {
    android.compileSdk = libs.version("android-compileSdk").toInt()
    android.compileOptions.sourceCompatibility = javaVersion()
    android.compileOptions.targetCompatibility = javaVersion()
    android.lint.configureHemisphereLint()
}

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
