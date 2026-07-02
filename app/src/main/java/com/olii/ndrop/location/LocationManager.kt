package com.olii.ndrop.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * NDrop — LocationManager
 *
 * Wraps FusedLocationProviderClient with a clean suspend API.
 *
 * ⚠️ Location permission MUST be granted before calling getCurrentLocation().
 *    NFC scans can arrive before the user has granted permissions — always
 *    check and handle the null/timeout result gracefully.
 *
 * Signature: Olii-8882
 */
@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val _lastKnownLocation = MutableStateFlow<Location?>(null)
    val lastKnownLocation: StateFlow<Location?> = _lastKnownLocation

    /**
     * Gets the current location with a 5s timeout.
     * Returns null if permission denied, GPS off, or timeout exceeded.
     *
     * Uses PRIORITY_HIGH_ACCURACY for precise parking/discovery pins.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return withTimeoutOrNull(5_000L) {
            suspendCancellableCoroutine { continuation ->
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(3_000L)   // Accept location up to 3s old
                    .setDurationMillis(4_000L)
                    .build()

                val cancellationTokenSource = CancellationTokenSource()

                fusedClient.getCurrentLocation(request, cancellationTokenSource.token)
                    .addOnSuccessListener { location ->
                        _lastKnownLocation.value = location
                        continuation.resume(location)
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }
        }
    }

    /**
     * Starts continuous location updates for the map view.
     * Call stopLocationUpdates() when the map is no longer visible.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(callback: LocationCallback) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    fun stopLocationUpdates(callback: LocationCallback) {
        fusedClient.removeLocationUpdates(callback)
    }
}
