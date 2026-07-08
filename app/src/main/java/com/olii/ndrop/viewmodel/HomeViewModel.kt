package com.olii.ndrop.viewmodel

import android.content.Intent
import android.location.Location
import android.nfc.Tag
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olii.ndrop.data.model.Drop
import com.olii.ndrop.data.model.ParkingSpot
import com.olii.ndrop.data.model.Streak
import com.olii.ndrop.data.repository.DropRepository
import com.olii.ndrop.location.LocationManager
import com.olii.ndrop.nfc.NfcManager
import com.olii.ndrop.nfc.NfcWriter
import com.olii.ndrop.nfc.TagType
import com.olii.ndrop.nfc.TreasureCodec
import com.olii.ndrop.nfc.toHexString
import com.olii.ndrop.service.NotificationHelper
import com.olii.ndrop.service.ParkingGeofenceManager
import com.olii.ndrop.ui.scan.ScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NDrop — HomeViewModel (expanded for all 13 features)
 * Signature: Olii-8882
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val nfcWriter: NfcWriter,
    private val locationManager: LocationManager,
    private val repository: DropRepository,
    private val geofenceManager: ParkingGeofenceManager,
    private val notificationHelper: NotificationHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Core state ────────────────────────────────────────────────────────────
    val parkingSpot: StateFlow<ParkingSpot?> = repository.parkingSpot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val streak: StateFlow<Streak?> = repository.streak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    // Feature 3: live location for the AR "Walk to Car" compass
    val currentLocation: StateFlow<Location?> = locationManager.lastKnownLocation

    // ── Scan UI ───────────────────────────────────────────────────────────────
    private val _activeScanResult = MutableStateFlow<ScanResult?>(null)
    val activeScanResult: StateFlow<ScanResult?> = _activeScanResult.asStateFlow()

    private val _pendingUnknownUid = MutableStateFlow<String?>(null)
    val pendingUnknownUid: StateFlow<String?> = _pendingUnknownUid.asStateFlow()

    // Feature 10: the raw Tag from the most recent scan, kept only long enough to
    // attempt a write-back if the user registers it before pulling the tag away.
    private var lastScannedTag: Tag? = null

    // Treasure Trail: someone else's seeded Discovery, read off a foreign tag
    private val _pendingFoundTreasure = MutableStateFlow<TreasureCodec.FoundTreasure?>(null)
    val pendingFoundTreasure: StateFlow<TreasureCodec.FoundTreasure?> = _pendingFoundTreasure.asStateFlow()

    // Treasure Trail: a drop queued to be written onto the next tag scanned
    private val _pendingTreasureWrite = MutableStateFlow<Drop?>(null)
    val pendingTreasureWrite: StateFlow<Drop?> = _pendingTreasureWrite.asStateFlow()

    // Feature 1: Pending floor note after parking scan
    private val _showFloorNoteOverlay = MutableStateFlow(false)
    val showFloorNoteOverlay: StateFlow<Boolean> = _showFloorNoteOverlay.asStateFlow()

    // Feature 3: Quick collection after discovery scan
    private val _pendingCollectionDrop = MutableStateFlow<Drop?>(null)
    val pendingCollectionDrop: StateFlow<Drop?> = _pendingCollectionDrop.asStateFlow()

    val allCollections: StateFlow<List<String>> = repository.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Feature 6: Nearby drops
    private val _nearbyDrops = MutableStateFlow<List<Drop>>(emptyList())
    val nearbyDrops: StateFlow<List<Drop>> = _nearbyDrops.asStateFlow()

    // Navigation
    private val _navigateToTimer = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToTimer: SharedFlow<Unit> = _navigateToTimer.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    init {
        notificationHelper.createChannels()
        geofenceManager.startMonitoring(viewModelScope)
        observeNfcScans()
    }

    override fun onCleared() {
        geofenceManager.stopMonitoring()
        super.onCleared()
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _hasLocationPermission.value = granted
        if (granted) refreshNearbyDrops()
    }

    // ── NFC Pipeline ──────────────────────────────────────────────────────────

    private fun observeNfcScans() {
        viewModelScope.launch {
            nfcManager.scanEvents.collect { event ->
                lastScannedTag = event.rawTag

                // Treasure Trail: if a write is queued, this scan is the target
                // tag to write to, not a normal read — don't also process it as one.
                val toWrite = _pendingTreasureWrite.value
                if (toWrite != null) {
                    _pendingTreasureWrite.value = null
                    writeTreasureToTag(event.rawTag, toWrite)
                    return@collect
                }

                handleScan(event.uid, event.ndefText)
            }
        }
    }

    fun processNfcIntent(intent: Intent) {
        nfcManager.processIntent(intent)
    }

    private fun handleScan(uid: String, ndefText: String? = null) {
        viewModelScope.launch {
            vibrate()
            // Feature 13: increment scan counter
            repository.incrementTagScanCount(uid)
            // Feature 7: record pattern
            repository.recordScanPattern(uid)

            if (!_hasLocationPermission.value) {
                _errorMessage.emit("Enable location to use NDrop")
                return@launch
            }

            val tagType = repository.resolveTag(uid)

            // Treasure Trail: an unregistered tag carrying a treasure payload
            // isn't "unknown" — it's someone else's seeded Discovery.
            if (tagType == TagType.UNKNOWN && ndefText != null) {
                val found = TreasureCodec.decode(ndefText)
                if (found != null) {
                    _pendingFoundTreasure.value = found
                    return@launch
                }
            }

            when (tagType) {
                TagType.PARKING   -> handleParkingScan(uid)
                TagType.DISCOVERY -> handleDiscoveryScan()
                TagType.TIMER     -> handleTimerScan()
                TagType.UNKNOWN   -> _pendingUnknownUid.value = uid
            }
        }
    }

    private suspend fun handleParkingScan(uid: String) {
        val location = locationManager.getCurrentLocation()
        if (location == null) { _errorMessage.emit("Couldn't get location. Try again."); return }

        // Feature 12: each distinct Parking tag tracks its own car, named after
        // whatever label the user gave the tag when registering it.
        val existingSpot = repository.getParkingSpotByTag(uid)
        val carId    = existingSpot?.id ?: repository.resolveCarId(uid)
        val carLabel = existingSpot?.carLabel
            ?: repository.getTagByUid(uid)?.label
            ?: "My Car"

        repository.saveParkingSpot(location.latitude, location.longitude,
            carId = carId, carLabel = carLabel, tagUid = uid)

        _activeScanResult.value = ScanResult(
            tagType    = TagType.PARKING,
            message    = "$carLabel Saved",
            subMessage = "Your car location has been pinned"
        )
        // Feature 1: show floor note overlay after scan overlay
        _showFloorNoteOverlay.value = true
        refreshNearbyDrops()
    }

    private suspend fun handleDiscoveryScan() {
        val location = locationManager.getCurrentLocation()
        if (location == null) { _errorMessage.emit("Couldn't get location. Try again."); return }

        val drop = repository.addDrop(location.latitude, location.longitude)
        _activeScanResult.value = ScanResult(
            tagType    = TagType.DISCOVERY,
            message    = "Place Saved",
            subMessage = drop.title
        )
        // Feature 3: prompt collection assignment
        _pendingCollectionDrop.value = drop
        refreshNearbyDrops()
    }

    private fun handleTimerScan() {
        _activeScanResult.value = ScanResult(
            tagType    = TagType.TIMER,
            message    = "Timer Ready",
            subMessage = "Opening timer..."
        )
        viewModelScope.launch { _navigateToTimer.emit(Unit) }
    }

    fun dismissScanResult()    { _activeScanResult.value = null }
    fun dismissFloorNote()     { _showFloorNoteOverlay.value = false }
    fun dismissCollectionPick(){ _pendingCollectionDrop.value = null }

    // ── Treasure Trail ────────────────────────────────────────────────────────

    fun dismissFoundTreasure() { _pendingFoundTreasure.value = null }

    fun saveFoundTreasure(found: TreasureCodec.FoundTreasure) {
        viewModelScope.launch {
            val drop = repository.addFoundDrop(found)
            _pendingFoundTreasure.value = null
            _activeScanResult.value = ScanResult(
                tagType    = TagType.DISCOVERY,
                message    = "Treasure Found!",
                subMessage = drop.title
            )
            // Let them re-file it into one of their own collections, same as
            // any other fresh Discovery scan.
            _pendingCollectionDrop.value = drop
            refreshNearbyDrops()
        }
    }

    /** Queues [drop] to be written to whatever tag is scanned next. */
    fun startWritingTreasure(drop: Drop) { _pendingTreasureWrite.value = drop }
    fun cancelWritingTreasure() { _pendingTreasureWrite.value = null }

    private fun writeTreasureToTag(tag: Tag, drop: Drop) {
        viewModelScope.launch {
            vibrate()
            when (val result = nfcWriter.writeDropInfo(tag, drop)) {
                is NfcWriter.WriteResult.Success ->
                    _errorMessage.emit("✨ Treasure written! Go hide it somewhere good.")
                is NfcWriter.WriteResult.Failure ->
                    _errorMessage.emit("Couldn't write to tag: ${result.reason}")
            }
        }
    }

    // Feature 1: Save floor note
    fun saveFloorNote(note: String) {
        viewModelScope.launch {
            repository.getParkingSpotOnce()?.let { spot ->
                repository.saveParkingSpot(
                    spot.latitude, spot.longitude,
                    floorNote = note,
                    carId     = spot.id,
                    carLabel  = spot.carLabel,
                    tagUid    = spot.tagUid
                )
            }
            _showFloorNoteOverlay.value = false
        }
    }

    // Feature 3: Assign collection to pending drop
    fun assignCollection(collection: String) {
        viewModelScope.launch {
            _pendingCollectionDrop.value?.let { drop ->
                repository.updateDrop(drop.copy(collectionName = collection))
            }
            _pendingCollectionDrop.value = null
        }
    }

    // Feature 6: Refresh nearby drops from current location
    fun refreshNearbyDrops() {
        viewModelScope.launch {
            val loc = locationManager.getCurrentLocation() ?: return@launch
            _nearbyDrops.value = repository.getDropsNearby(loc.latitude, loc.longitude)
        }
    }

    fun getCurrentLocation(onLocation: (android.location.Location) -> Unit) {
        viewModelScope.launch {
            locationManager.getCurrentLocation()?.let { onLocation(it) }
        }
    }

    /** Feature 3: one location fix for the AR compass; suspends until it resolves so
     *  callers can poll on a steady cadence without piling up overlapping requests. */
    suspend fun refreshLocationForCompass() {
        locationManager.getCurrentLocation()
    }

    // ── Tag Registration ──────────────────────────────────────────────────────

    fun registerTag(uid: String, type: TagType, label: String) {
        viewModelScope.launch {
            repository.registerTag(uid, type, label)
            _pendingUnknownUid.value = null

            // Feature 10: best-effort write-back so the tag self-identifies on any
            // device. Only attempted if it's still the same tag that triggered this
            // registration — the NFC connection is invalid once the tag is pulled away.
            val tag = lastScannedTag
            if (tag != null && tag.id.toHexString() == uid) {
                when (val result = nfcWriter.writeTagInfo(tag, type, label)) {
                    is NfcWriter.WriteResult.Success ->
                        _errorMessage.emit("✍️ Wrote tag info — it'll self-identify on any device")
                    is NfcWriter.WriteResult.Failure ->
                        _errorMessage.emit("Tag registered (couldn't write to it: ${result.reason})")
                }
            }

            handleScan(uid)
        }
    }

    fun dismissRegistrationSheet() { _pendingUnknownUid.value = null }

    // ── Parking ───────────────────────────────────────────────────────────────

    fun clearParking(id: Int = 1) {
        viewModelScope.launch { repository.clearParkingSpot(id) }
    }

    // ── Haptics ───────────────────────────────────────────────────────────────

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService<VibratorManager>()?.defaultVibrator
                    ?.vibrate(VibrationEffect.createOneShot(60L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService<Vibrator>()
                    ?.vibrate(VibrationEffect.createOneShot(60L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }
}
