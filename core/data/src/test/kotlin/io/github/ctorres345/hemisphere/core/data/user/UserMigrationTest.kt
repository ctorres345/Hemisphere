package io.github.ctorres345.hemisphere.core.data.user

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
 * The `user.db` migration harness (architecture §8). A new schema version adds a test here that
 * creates the old version, runs the migration with `runMigrationsAndValidate` and checks the rows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            UserDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun `version 1 opens from the exported schema`() {
        val db = helper.createDatabase("user-migration-test", 1)
        db.execSQL(
            "INSERT INTO folder_view (entry_key, path_lookup, sort_field, descending) " +
                "VALUES ('k', 'p', 'Size', 1)",
        )
        db.query("SELECT sort_field FROM folder_view").use {
            it.moveToFirst()
            assertEquals("Size", it.getString(0))
        }
        db.close()

        // The committed schema opens under the compiled entities.
        val room =
            Room
                .databaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    UserDatabase::class.java,
                    "user-migration-test",
                ).build()
        room.openHelper.writableDatabase
        room.close()
    }
}
