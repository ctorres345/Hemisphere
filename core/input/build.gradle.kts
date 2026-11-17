plugins {
    alias(libs.plugins.hemisphere.android.library.compose)
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
}
