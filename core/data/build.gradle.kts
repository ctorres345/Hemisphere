plugins {
    alias(libs.plugins.hemisphere.android.library)
    alias(libs.plugins.hemisphere.hilt)
    alias(libs.plugins.androidx.room)
}

android {
    // Robolectric reads the exported Room schemas as test assets for MigrationTestHelper.
    testOptions.unitTests.isIncludeAndroidResources = true
    val schemaDir = "$projectDir/schemas"
    sourceSets
        .getByName("test")
        .assets.directories
        .add(schemaDir)
}

// Exported schemas are committed; a version bump adds a file and a migration test (architecture §8).
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(project(":core:fs"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
}
