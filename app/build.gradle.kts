plugins {
    alias(libs.plugins.hemisphere.android.application)
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
