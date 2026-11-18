package io.github.ctorres345.hemisphere.core.jobs

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ctorres345.hemisphere.core.jobs.db.StateDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The migration harness for `state.db` (architecture §8). The first post-MVP schema change adds
 * version 2 and a test here that creates version 1, runs the migration with
 * `runMigrationsAndValidate` and checks the rows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StateMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            StateDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun `version 1 has all five tables and opens under the entities`() {
        val db = helper.createDatabase("migration-test", 1)
        val tables =
            db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use {
                generateSequence { if (it.moveToNext()) it.getString(0) else null }.toSet()
            }
        assertEquals(true, tables.containsAll(setOf("jobs", "records", "conflicts", "trash_index", "undo")))
        db.close()

        val room =
            Room
                .databaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    StateDatabase::class.java,
                    "migration-test",
                ).build()
        room.openHelper.writableDatabase
        room.close()
    }
}
