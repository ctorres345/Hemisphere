plugins {
    alias(libs.plugins.hemisphere.android.library)
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:platform"))
}
