plugins {
    alias(libs.plugins.hemisphere.android.library.compose)
    alias(libs.plugins.hemisphere.hilt)
    alias(libs.plugins.hemisphere.screenshot)
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
}
