plugins {
    alias(libs.plugins.hemisphere.android.library)
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:fs"))
    implementation(project(":core:data"))
    // The data layer tests against InMemoryProvider without core:testing (architecture §2).
    testImplementation(project(":core:fs-testing"))
}
