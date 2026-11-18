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

    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}

// `check` (CI and the review gate) compares every screenshot test with its golden image.
tasks.named("check") { dependsOn("verifyRoborazziDebug") }
