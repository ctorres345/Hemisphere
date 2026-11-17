plugins {
    alias(libs.plugins.hemisphere.android.library)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(project(":core:platform"))
}
