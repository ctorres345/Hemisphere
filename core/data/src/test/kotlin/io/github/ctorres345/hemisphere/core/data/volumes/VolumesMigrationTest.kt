package io.github.ctorres345.hemisphere.core.data.volumes

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The migration harness (architecture §8). A new schema version adds a test here that creates the
 * old version, runs the migration with `runMigrationsAndValidate` and checks the rows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VolumesMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            VolumesDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun `version 1 opens from the exported schema`() {
        val db = helper.createDatabase("migration-test", 1)
        db.execSQL("INSERT INTO grants (volume_id, tree_uri) VALUES ('v', 'u')")
        db.query("SELECT tree_uri FROM grants").use {
            it.moveToFirst()
            assertEquals("u", it.getString(0))
        }
        db.close()

        // The committed schema opens under the compiled entities. A changed entity without a new
        // version is caught by CI, which fails when a committed schemas/*.json changes.
        val room =
            Room
                .databaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    VolumesDatabase::class.java,
                    "migration-test",
                ).build()
        room.openHelper.writableDatabase
        room.close()
    }
}
