package com.paulchibamba.teleprompter.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScriptEntity::class, PresetEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class PrompterDatabase : RoomDatabase() {

    abstract fun scriptDao(): ScriptDao

    abstract fun presetDao(): PresetDao

    companion object {
        const val NAME = "prompter.db"

        /**
         * Adds the `presets` table (docs/SPEC.md §3.3). The statement is copied from the schema
         * Room exported for version 2 (`app/schemas/…/2.json`) so a migrated database is
         * byte-identical to a freshly created one — Room's own schema validation on open is what
         * catches it if the two ever diverge.
         *
         * The built-in presets are not inserted here. [RoomPresetRepository] seeds them on first
         * read, which covers this migration and a fresh install with the same code.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `presets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`typographyJson` TEXT NOT NULL, " +
                        "`layoutJson` TEXT NOT NULL, " +
                        "`scrollJson` TEXT NOT NULL, " +
                        "`isBuiltIn` INTEGER NOT NULL)",
                )
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun build(context: Context): PrompterDatabase =
            Room.databaseBuilder(context.applicationContext, PrompterDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
