plugins {
    alias(libs.plugins.hemisphere.android.library.compose)
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))

    // Builds real KeyEvent and MotionEvent objects for the tracker tests (HEM-74).
    testImplementation(libs.robolectric)
}
