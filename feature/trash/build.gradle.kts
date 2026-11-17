plugins {
    alias(libs.plugins.hemisphere.android.library.compose)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:input"))
    testImplementation(project(":core:testing"))
}
