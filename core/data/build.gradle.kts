plugins {
    alias(libs.plugins.hemisphere.android.library)
    alias(libs.plugins.hemisphere.hilt)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(project(":core:fs"))
    implementation(libs.androidx.datastore.preferences)
}
