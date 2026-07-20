package com.sanu.carouselforge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        LayerEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        private const val DATABASE_NAME = "carousel_forge.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                // Local-only v1 with no cloud copy: if a schema/identity mismatch is ever
                // hit (e.g. an older on-device DB from a prior build), recreate the DB
                // instead of hard-crashing every query. Real releases still ship explicit
                // migrations above; this is only the safety net.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN width REAL NOT NULL DEFAULT 486",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN height REAL NOT NULL DEFAULT 486",
                )
            }
        }

        // Single consolidated migration adding every column later phases need, so the
        // schema version is bumped only once for the whole feature set.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE projects ADD COLUMN slideCount INTEGER NOT NULL DEFAULT 1",
                )
                database.execSQL(
                    "ALTER TABLE projects ADD COLUMN bgColorStart INTEGER NOT NULL " +
                        "DEFAULT 4294967295",
                )
                database.execSQL("ALTER TABLE projects ADD COLUMN bgColorEnd INTEGER")
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN textColor INTEGER NOT NULL " +
                        "DEFAULT 4278190080",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN textSizeSp REAL NOT NULL DEFAULT 32",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN fontWeight INTEGER NOT NULL DEFAULT 400",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN textAlign TEXT NOT NULL DEFAULT 'CENTER'",
                )
                database.execSQL("ALTER TABLE layers ADD COLUMN fontFamily TEXT")
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN alpha REAL NOT NULL DEFAULT 1",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN cornerRadius REAL NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN hasShadow INTEGER NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN cropLeft REAL NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN cropTop REAL NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN cropRight REAL NOT NULL DEFAULT 1",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN cropBottom REAL NOT NULL DEFAULT 1",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN brightness REAL NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN contrast REAL NOT NULL DEFAULT 1",
                )
                database.execSQL(
                    "ALTER TABLE layers ADD COLUMN saturation REAL NOT NULL DEFAULT 1",
                )
                database.execSQL("ALTER TABLE layers ADD COLUMN filterPreset TEXT")
                database.execSQL("ALTER TABLE layers ADD COLUMN shapeKind TEXT")
                database.execSQL("ALTER TABLE layers ADD COLUMN fillColor INTEGER")
            }
        }
    }
}
