package com.olii.ndrop.data.db

import androidx.room.*
import com.olii.ndrop.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * NDrop — Room DAOs (expanded for all 13 features)
 * Signature: Olii-8882
 */

@Dao
interface DropDao {
    @Query("SELECT * FROM drops ORDER BY timestamp DESC")
    fun getAllDrops(): Flow<List<Drop>>

    @Query("SELECT * FROM drops WHERE collectionName = :collection ORDER BY timestamp DESC")
    fun getDropsByCollection(collection: String): Flow<List<Drop>>

    @Query("SELECT DISTINCT collectionName FROM drops ORDER BY collectionName ASC")
    fun getAllCollections(): Flow<List<String>>

    // Feature 6: Nearby drops within bounding box (fast, no trig in SQL)
    @Query("""
        SELECT * FROM drops 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLng AND :maxLng
        ORDER BY timestamp DESC
    """)
    suspend fun getDropsNearby(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<Drop>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrop(drop: Drop): Long

    @Update
    suspend fun updateDrop(drop: Drop)

    @Delete
    suspend fun deleteDrop(drop: Drop)

    @Query("SELECT COUNT(*) FROM drops")
    suspend fun getDropCount(): Int
}

@Dao
interface ParkingDao {
    // Feature 12: multiple cars can exist; the "active" one shown on Home/AR-compass
    // is whichever was most recently parked.
    @Query("SELECT * FROM parking_spot ORDER BY timestamp DESC LIMIT 1")
    fun getParkingSpot(): Flow<ParkingSpot?>

    @Query("SELECT * FROM parking_spot ORDER BY timestamp DESC LIMIT 1")
    suspend fun getParkingSpotOnce(): ParkingSpot?

    // Feature 12: All cars
    @Query("SELECT * FROM parking_spot ORDER BY id ASC")
    fun getAllParkingSpots(): Flow<List<ParkingSpot>>

    @Query("SELECT * FROM parking_spot WHERE tagUid = :uid LIMIT 1")
    suspend fun getParkingSpotByTag(uid: String): ParkingSpot?

    @Query("SELECT MAX(id) FROM parking_spot")
    suspend fun getMaxParkingId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertParkingSpot(spot: ParkingSpot)

    @Query("DELETE FROM parking_spot WHERE id = :id")
    suspend fun clearParkingSpot(id: Int = 1)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM registered_tags ORDER BY registeredAt DESC")
    fun getAllTags(): Flow<List<RegisteredTag>>

    @Query("SELECT * FROM registered_tags WHERE uid = :uid")
    suspend fun getTagByUid(uid: String): RegisteredTag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(tag: RegisteredTag)

    // Feature 13: Increment scan count atomically
    @Query("UPDATE registered_tags SET scanCount = scanCount + 1 WHERE uid = :uid")
    suspend fun incrementScanCount(uid: String)

    @Delete
    suspend fun deleteTag(tag: RegisteredTag)
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak WHERE id = 1")
    fun getStreak(): Flow<Streak?>

    @Query("SELECT * FROM streak WHERE id = 1")
    suspend fun getStreakOnce(): Streak?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStreak(streak: Streak)
}

@Dao
interface ScanPatternDao {
    @Query("SELECT * FROM scan_patterns WHERE uid = :uid")
    suspend fun getPattern(uid: String): ScanPattern?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPattern(pattern: ScanPattern)
}
