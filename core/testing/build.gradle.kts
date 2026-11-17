plugins {
    alias(libs.plugins.hemisphere.android.library)
}

dependencies {
    api(project(":core:fs"))
    api(project(":core:jobs"))
    api(project(":core:data"))
}
