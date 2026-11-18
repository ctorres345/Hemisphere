plugins {
    alias(libs.plugins.hemisphere.android.library)
    alias(libs.plugins.hemisphere.hilt)
    alias(libs.plugins.androidx.room)
}

val schemaDir = "$projectDir/schemas"

android {
    // Robolectric reads the exported Room schemas as test assets for MigrationTestHelper.
    testOptions.unitTests.isIncludeAndroidResources = true
    sourceSets
        .getByName("test")
        .assets.directories
        .add(schemaDir)
}

// `state.db` stays at version 1 until the MVP ships: later tickets edit the entities and 1.json in
// place. The first post-MVP change adds version 2 with a migration (HEM-43).
room {
    schemaDirectory(schemaDir)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(project(":core:fs"))
    implementation(project(":core:data"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // The data layer tests against InMemoryProvider without core:testing (architecture §2).
    testImplementation(project(":core:fs-testing"))
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
}
