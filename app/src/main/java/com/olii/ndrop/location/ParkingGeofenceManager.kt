package com.olii.ndrop.service

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.olii.ndrop.data.repository.DropRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NDrop — ParkingGeofenceManager
 *
 * Polls device location while the app is foregrounded and fires a
 * proximity notification when the user enters a 150m radius of their
 * parked car (and hasn't been notified in the last 10 minutes).
 *
 * Kept intentionally lightweight — no background service needed for v1.
 * The check runs only while the app is alive (HomeViewModel scope).
 *
 * Signature: Olii-8882
 */
@Singleton
class ParkingGeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DropRepository,
    private val notificationHelper: NotificationHelper
) {
    companion object {
        private const val PROXIMITY_RADIUS_METERS = 150f
        private const val NOTIFY_COOLDOWN_MS      = 10 * 60 * 1000L  // 10 min
        private const val LOCATION_INTERVAL_MS    = 15_000L           // poll every 15s
    }

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private var lastNotifiedAt = 0L
    private var monitoringJob: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val current = result.lastLocation ?: return
            checkProximity(current)
        }
    }

    @SuppressLint("MissingPermission")
    fun startMonitoring(scope: CoroutineScope) {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            // Only start GPS polling when a parking spot actually exists
            repository.parkingSpot.collectLatest { spot ->
                if (spot != null) {
                    val request = LocationRequest.Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        LOCATION_INTERVAL_MS
                    ).setMinUpdateDistanceMeters(20f).build()

                    fusedClient.requestLocationUpdates(
                        request, locationCallback, Looper.getMainLooper()
                    )
                } else {
                    fusedClient.removeLocationUpdates(locationCallback)
                    notificationHelper.cancelParkingNotification()
                }
            }
        }
    }

    fun stopMonitoring() {
        fusedClient.removeLocationUpdates(locationCallback)
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun checkProximity(current: Location) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val spot = repository.getParkingSpotOnce() ?: return@launch

            val spotLocation = Location("parking").apply {
                latitude  = spot.latitude
                longitude = spot.longitude
            }

            val distance = current.distanceTo(spotLocation)
            val now = System.currentTimeMillis()
            val cooldownElapsed = (now - lastNotifiedAt) > NOTIFY_COOLDOWN_MS

            if (distance <= PROXIMITY_RADIUS_METERS && cooldownElapsed) {
                lastNotifiedAt = now
                notificationHelper.showParkingProximityNotification(distance)
            }
        }
    }
}
