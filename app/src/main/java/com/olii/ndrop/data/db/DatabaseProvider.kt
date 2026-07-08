package com.olii.ndrop.data.db

import android.content.Context
import androidx.room.Room

/**
 * NDrop — DatabaseProvider
 *
 * A plain Kotlin singleton that holds the single Room instance.
 * Used by components that can't use Hilt injection — specifically
 * the ParkingWidget (a plain AppWidgetProvider/RemoteViews) and any
 * BroadcastReceivers.
 *
 * Hilt's AppModule calls getInstance() too, so both paths share
 * the exact same database object — no double-open, no WAL conflicts.
 *
 * Signature: Olii-8882
 */
object DatabaseProvider {

    @Volatile
    private var instance: NDropDatabase? = null

    fun getInstance(context: Context): NDropDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NDropDatabase::class.java,
                "ndrop.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }

    // Migration: v1 → v2
    // Adds new columns to existing tables + creates new tables
    private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
            // New columns on drops
            database.execSQL("ALTER TABLE drops ADD COLUMN photoPath TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE drops ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            // New columns on parking_spot
            database.execSQL("ALTER TABLE parking_spot ADD COLUMN carLabel TEXT NOT NULL DEFAULT 'My Car'")
            database.execSQL("ALTER TABLE parking_spot ADD COLUMN tagUid TEXT NOT NULL DEFAULT ''")
            // New column on registered_tags
            database.execSQL("ALTER TABLE registered_tags ADD COLUMN scanCount INTEGER NOT NULL DEFAULT 0")
            // New tables
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS streak (
                    id INTEGER PRIMARY KEY NOT NULL,
                    currentStreak INTEGER NOT NULL DEFAULT 0,
                    longestStreak INTEGER NOT NULL DEFAULT 0,
                    lastDropDate INTEGER NOT NULL DEFAULT 0
                )
            """)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS scan_patterns (
                    uid TEXT PRIMARY KEY NOT NULL,
                    avgHourOfDay REAL NOT NULL DEFAULT 0,
                    scanDates TEXT NOT NULL DEFAULT ''
                )
            """)
        }
    }

    // Migration: v2 → v3
    // Adds a true lifetime scan counter so the rolling-average weighting in
    // DropRepository.recordScanPattern() stays correct after scanDates is
    // capped to the last 30 days (previously it re-derived the weight from
    // scanDates.size, which silently plateaued at 30 forever).
    private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE scan_patterns ADD COLUMN totalScans INTEGER NOT NULL DEFAULT 0")
            // Backfill from the existing (possibly already-capped) date list so
            // existing patterns don't see a sudden jump in average sensitivity.
            database.execSQL("""
                UPDATE scan_patterns SET totalScans =
                    CASE WHEN scanDates = '' THEN 0
                         ELSE (LENGTH(scanDates) - LENGTH(REPLACE(scanDates, ',', '')) + 1)
                    END
            """)
        }
    }
}
