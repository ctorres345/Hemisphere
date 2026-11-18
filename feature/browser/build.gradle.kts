plugins {
    alias(libs.plugins.hemisphere.android.library.compose)
    alias(libs.plugins.hemisphere.hilt)
    alias(libs.plugins.roborazzi)
}

android {
    // Robolectric reads the merged resources (strings, theme) for the screenshot tests.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:input"))
    implementation(project(":core:platform"))
    implementation(project(":core:mvi"))
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}

// The golden images are test inputs, so changing one re-runs the tests instead of reusing a cached result.
tasks.withType<Test>().configureEach {
    inputs
        .files(fileTree("src/test/screenshots"))
        .withPropertyName("screenshotGoldens")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// The goldens are Linux renders, recorded by CI (see BrowserScreenshotTest). macOS draws text a few pixels
// differently, so `check` compares them only on Linux, which is where CI's gradle-check runs.
if (System.getProperty("os.name").startsWith("Linux")) {
    tasks.named("check") { dependsOn("verifyRoborazziDebug") }
}
