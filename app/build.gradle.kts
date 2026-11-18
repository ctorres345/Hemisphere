plugins {
    alias(libs.plugins.hemisphere.android.application)
    alias(libs.plugins.hemisphere.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:input"))
    implementation(project(":core:platform"))
    implementation(project(":core:domain"))
    implementation(project(":core:fs"))
    implementation(project(":core:jobs"))
    implementation(project(":core:data"))
    implementation(project(":core:mvi"))
    implementation(project(":feature:browser"))
    implementation(project(":feature:jobs"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:trash"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.konsist)
}

// Konsist reads every module's sources from disk, so they are inputs of the tests that run it;
// otherwise the build cache could skip the layer rules after a source-only change.
tasks.withType<Test>().configureEach {
    inputs
        .files(
            rootProject.fileTree(rootDir) {
                include("*/src/**/*.kt", "*/*/src/**/*.kt")
                exclude("**/build/**")
            },
        ).withPropertyName("konsistSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// MergedManifestTest reads the merged manifest of the debug and release variants. The unit-test tasks depend on
// the manifest merge of both, so the test never reads a stale or missing file; each file is also a task input.
tasks.withType<Test>().configureEach {
    listOf("Debug", "Release").forEach { variant ->
        val merged =
            layout.buildDirectory.file(
                "intermediates/merged_manifests/${variant.lowercase()}/process${variant}Manifest/AndroidManifest.xml",
            )
        dependsOn("process${variant}Manifest")
        inputs.file(merged).withPropertyName("mergedManifest$variant").withPathSensitivity(PathSensitivity.NONE)
        systemProperty("hemisphere.mergedManifest.${variant.lowercase()}", merged.get().asFile.absolutePath)
    }
}
