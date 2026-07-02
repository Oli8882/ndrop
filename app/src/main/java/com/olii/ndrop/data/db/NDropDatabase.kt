package com.olii.ndrop.data.db

import androidx.room.*
import com.olii.ndrop.data.model.*
import com.olii.ndrop.nfc.TagType

/**
 * NDrop — Room Database v2
 * Added: Streak, ScanPattern tables; new columns on Drop, ParkingSpot, RegisteredTag.
 * Signature: Olii-8882
 */

class TagTypeConverter {
    @TypeConverter fun fromTagType(type: TagType): String = type.name
    @TypeConverter fun toTagType(name: String): TagType = TagType.valueOf(name)
}

@Database(
    entities = [Drop::class, ParkingSpot::class, RegisteredTag::class, Streak::class, ScanPattern::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(TagTypeConverter::class)
abstract class NDropDatabase : RoomDatabase() {
    abstract fun dropDao(): DropDao
    abstract fun parkingDao(): ParkingDao
    abstract fun tagDao(): TagDao
    abstract fun streakDao(): StreakDao
    abstract fun scanPatternDao(): ScanPatternDao
}
