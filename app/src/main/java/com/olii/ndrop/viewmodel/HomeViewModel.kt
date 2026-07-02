package com.olii.ndrop.viewmodel

import android.content.Intent
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
import com.olii.ndrop.nfc.TagType
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

    // ── Scan UI ───────────────────────────────────────────────────────────────
    private val _activeScanResult = MutableStateFlow<ScanResult?>(null)
    val activeScanResult: StateFlow<ScanResult?> = _activeScanResult.asStateFlow()

    private val _pendingUnknownUid = MutableStateFlow<String?>(null)
    val pendingUnknownUid: StateFlow<String?> = _pendingUnknownUid.asStateFlow()

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
                handleScan(event.uid)
            }
        }
    }

    fun processNfcIntent(intent: Intent) {
        nfcManager.processIntent(intent)
    }

    private fun handleScan(uid: String) {
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

        // Check if this tag maps to a specific car
        val existingSpot = repository.getParkingSpotByTag(uid)
        val carId    = existingSpot?.id ?: 1
        val carLabel = existingSpot?.carLabel ?: "My Car"

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

    // ── Tag Registration ──────────────────────────────────────────────────────

    fun registerTag(uid: String, type: TagType, label: String) {
        viewModelScope.launch {
            repository.registerTag(uid, type, label)
            _pendingUnknownUid.value = null
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
