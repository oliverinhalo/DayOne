package com.dayone.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Project::class, PhotoEntry::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun photoEntryDao(): PhotoEntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * v1 -> v2: adds the scheduling / capture columns introduced in 2.0.
         *
         * This is a real migration rather than a destructive fallback on purpose: an
         * existing install must keep every project, photo row and streak when it updates.
         * Every added column has a DEFAULT so existing rows stay valid.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN activeDaysMask INTEGER NOT NULL DEFAULT 127")
                db.execSQL("ALTER TABLE projects ADD COLUMN nagEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE projects ADD COLUMN nagIntervalMinutes INTEGER NOT NULL DEFAULT 45")
                db.execSQL("ALTER TABLE projects ADD COLUMN nagUntilMinuteOfDay INTEGER NOT NULL DEFAULT 1320")
                db.execSQL("ALTER TABLE projects ADD COLUMN secondReminderMinuteOfDay INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN sortIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN autoCropFace INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE projects ADD COLUMN headFraction REAL NOT NULL DEFAULT 0.42")

                db.execSQL("ALTER TABLE photo_entries ADD COLUMN note TEXT")
                db.execSQL("ALTER TABLE photo_entries ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dayone.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
