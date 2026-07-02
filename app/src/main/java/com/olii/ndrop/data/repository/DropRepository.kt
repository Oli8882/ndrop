package com.olii.ndrop.data.repository

import android.location.Location
import com.olii.ndrop.data.db.*
import com.olii.ndrop.data.model.*
import com.olii.ndrop.nfc.TagType
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * NDrop — DropRepository (expanded for all 13 features)
 * Signature: Olii-8882
 */
@Singleton
class DropRepository @Inject constructor(
    private val dropDao: DropDao,
    private val parkingDao: ParkingDao,
    private val tagDao: TagDao,
    private val streakDao: StreakDao,
    private val scanPatternDao: ScanPatternDao
) {

    // ── Parking ───────────────────────────────────────────────────────────────

    val parkingSpot: Flow<ParkingSpot?> = parkingDao.getParkingSpot()
    val allParkingSpots: Flow<List<ParkingSpot>> = parkingDao.getAllParkingSpots()

    suspend fun saveParkingSpot(
        lat: Double, lng: Double,
        floorNote: String = "",
        carId: Int = 1,
        carLabel: String = "My Car",
        tagUid: String = ""
    ) {
        parkingDao.upsertParkingSpot(
            ParkingSpot(id = carId, latitude = lat, longitude = lng,
                        floorNote = floorNote, carLabel = carLabel, tagUid = tagUid)
        )
    }

    suspend fun clearParkingSpot(id: Int = 1) = parkingDao.clearParkingSpot(id)
    suspend fun getParkingSpotOnce(): ParkingSpot? = parkingDao.getParkingSpotOnce()
    suspend fun getParkingSpotByTag(uid: String): ParkingSpot? = parkingDao.getParkingSpotByTag(uid)

    // ── Discovery Drops ───────────────────────────────────────────────────────

    val allDrops: Flow<List<Drop>> = dropDao.getAllDrops()
    val allCollections: Flow<List<String>> = dropDao.getAllCollections()

    fun getDropsByCollection(collection: String): Flow<List<Drop>> =
        dropDao.getDropsByCollection(collection)

    suspend fun addDrop(
        lat: Double, lng: Double,
        collection: String = "Uncategorized",
        photoPath: String = ""
    ): Drop {
        val count = dropDao.getDropCount()
        val drop = Drop(
            latitude = lat, longitude = lng,
            title = "Discovery #${count + 1}",
            collectionName = collection,
            photoPath = photoPath
        )
        val id = dropDao.insertDrop(drop)
        updateStreak()
        return drop.copy(id = id)
    }

    suspend fun updateDrop(drop: Drop) = dropDao.updateDrop(drop)
    suspend fun deleteDrop(drop: Drop) = dropDao.deleteDrop(drop)

    // Feature 6: Nearby drops within ~500m radius
    suspend fun getDropsNearby(lat: Double, lng: Double, radiusMeters: Double = 500.0): List<Drop> {
        val delta = radiusMeters / 111_000.0  // ~degrees
        return dropDao.getDropsNearby(lat - delta, lat + delta, lng - delta, lng + delta)
    }

    // ── Tag Registry ──────────────────────────────────────────────────────────

    val allTags: Flow<List<RegisteredTag>> = tagDao.getAllTags()

    suspend fun resolveTag(uid: String): TagType =
        tagDao.getTagByUid(uid)?.tagType ?: TagType.UNKNOWN

    suspend fun registerTag(uid: String, type: TagType, label: String) {
        tagDao.upsertTag(RegisteredTag(uid = uid, tagType = type, label = label))
    }

    suspend fun updateTag(tag: RegisteredTag) {
        tagDao.upsertTag(tag)
    }

    suspend fun deleteTag(tag: RegisteredTag) = tagDao.deleteTag(tag)
    suspend fun getTagByUid(uid: String): RegisteredTag? = tagDao.getTagByUid(uid)

    // Feature 13: Increment scan count on every scan
    suspend fun incrementTagScanCount(uid: String) = tagDao.incrementScanCount(uid)

    // ── Streaks (Feature 5) ───────────────────────────────────────────────────

    val streak: Flow<Streak?> = streakDao.getStreak()

    private suspend fun updateStreak() {
        val today = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        val current = streakDao.getStreakOnce() ?: Streak()
        val lastDay = TimeUnit.MILLISECONDS.toDays(current.lastDropDate)

        val newStreak = when {
            lastDay == today          -> current  // Already dropped today
            lastDay == today - 1      -> current.copy(
                currentStreak = current.currentStreak + 1,
                longestStreak = maxOf(current.longestStreak, current.currentStreak + 1),
                lastDropDate  = System.currentTimeMillis()
            )
            else                      -> current.copy(
                currentStreak = 1,
                lastDropDate  = System.currentTimeMillis()
            )
        }
        streakDao.upsertStreak(newStreak)
    }

    // ── Scan Patterns (Feature 7) ─────────────────────────────────────────────

    suspend fun recordScanPattern(uid: String) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY).toFloat()
        val today = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()).toString()
        val existing = scanPatternDao.getPattern(uid)

        val updated = if (existing == null) {
            ScanPattern(uid = uid, avgHourOfDay = hour, scanDates = today)
        } else {
            // Rolling average of hour
            val scans = existing.scanDates.split(",").size.toFloat()
            val newAvg = ((existing.avgHourOfDay * scans) + hour) / (scans + 1)
            val dates = (existing.scanDates.split(",") + today)
                .takeLast(30).joinToString(",")  // Keep last 30 days
            existing.copy(avgHourOfDay = newAvg, scanDates = dates)
        }
        scanPatternDao.upsertPattern(updated)
    }

    suspend fun getScanPattern(uid: String): ScanPattern? = scanPatternDao.getPattern(uid)
}
