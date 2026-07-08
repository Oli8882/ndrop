package com.olii.ndrop.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.olii.ndrop.nfc.TagType

/**
 * NDrop — Data Models
 * Expanded to support all 13 features.
 * Signature: Olii-8882
 */

@Entity(tableName = "drops")
data class Drop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val collectionName: String = "Uncategorized",
    val timestamp: Long = System.currentTimeMillis(),
    val emoji: String = "📍",
    val photoPath: String = "",         // Feature 5: local file path to attached photo
    val isFavorite: Boolean = false     // Future use
)

/**
 * Parking spot — now supports multiple cars (Feature 12).
 * id=1 is always "My Car" (default). Additional cars use id=2,3...
 */
@Entity(tableName = "parking_spot")
data class ParkingSpot(
    @PrimaryKey val id: Int = 1,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val floorNote: String = "",         // Feature 1: "Level 3", "Zone B4"
    val carLabel: String = "My Car",    // Feature 12: "Tesla", "Wife's Car"
    val tagUid: String = ""             // Feature 12: which tag maps to this car
)

/**
 * Registered NFC tag — now tracks scan count (Feature 13).
 */
@Entity(tableName = "registered_tags")
data class RegisteredTag(
    @PrimaryKey val uid: String,
    val tagType: TagType,
    val label: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val scanCount: Int = 0              // Feature 13: NTag213 scan counter
)

/**
 * User streak — tracks daily Discovery activity (Feature 5).
 */
@Entity(tableName = "streak")
data class Streak(
    @PrimaryKey val id: Int = 1,        // Singleton
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastDropDate: Long = 0L         // Epoch day — compared daily
)

/**
 * Smart suggestion — stores pattern data for Feature 7.
 */
@Entity(tableName = "scan_patterns")
data class ScanPattern(
    @PrimaryKey val uid: String,        // Tag UID
    val avgHourOfDay: Float = 0f,       // Average hour tag is scanned
    val scanDates: String = "",         // CSV of epoch days (capped to last 30, for display)
    val totalScans: Int = 0             // True lifetime scan count — weights avgHourOfDay
                                         // correctly even after scanDates is capped
)
