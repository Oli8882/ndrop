package com.olii.ndrop.ui.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olii.ndrop.data.model.ParkingSpot
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography
import com.olii.ndrop.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*

/**
 * NDrop — ArCompassScreen (Feature 3 — Walk Back to Car)
 *
 * Uses the device magnetometer + accelerometer to compute heading,
 * then calculates bearing to the parked car and shows a live arrow.
 * Works completely offline — no maps needed.
 *
 * Signature: Olii-8882
 */
@Composable
fun ArCompassScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val parkingSpot by viewModel.parkingSpot.collectAsStateWithLifecycle()
    val liveLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Live device heading from compass sensor
    var deviceHeading by remember { mutableFloatStateOf(0f) }
    val currentLat = liveLocation?.latitude ?: 0.0
    val currentLng = liveLocation?.longitude ?: 0.0

    // Register compass sensor
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity   = FloatArray(3)
        val geomagnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER ->
                        System.arraycopy(event.values, 0, gravity, 0, gravity.size)
                    Sensor.TYPE_MAGNETIC_FIELD ->
                        System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.size)
                }
                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    deviceHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Poll a fresh location fix at a steady cadence while this screen is visible,
    // so the arrow/distance track the user's actual walk back to the car.
    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.refreshLocationForCompass()
            delay(2_000L)
        }
    }

    val spot = parkingSpot

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NDropColors.SpaceNavy)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back",
                    tint = NDropColors.WhiteMuted)
            }
            Column {
                Text("Walk to Car", style = NDropTypography.displayMedium)
                Text("Point your phone forward",
                    style = NDropTypography.bodyMedium)
            }
        }

        Spacer(Modifier.weight(1f))

        if (spot != null && liveLocation == null) {
            CircularProgressIndicator(color = NDropColors.Mint)
            Spacer(Modifier.height(16.dp))
            Text(
                "Getting your location…",
                style = NDropTypography.bodyMedium,
                color = NDropColors.WhiteMuted,
                textAlign = TextAlign.Center
            )
        } else if (spot != null) {
            // Real bearing/distance from the device's live GPS fix to the parking spot.
            val bearing = calculateBearing(
                fromLat = currentLat,
                fromLng = currentLng,
                toLat   = spot.latitude,
                toLng   = spot.longitude
            )

            val arrowRotation = bearing - deviceHeading
            val animatedRotation by animateFloatAsState(
                targetValue = arrowRotation,
                animationSpec = tween(durationMillis = 200, easing = LinearEasing),
                label = "compass_rotation"
            )

            // Distance
            val distanceM = approximateDistance(
                lat1 = currentLat,
                lng1 = currentLng,
                lat2 = spot.latitude,
                lng2 = spot.longitude
            )

            // Compass ring
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(NDropColors.NavyElevated),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing ring
                val pulse by rememberInfiniteTransition(label = "pulse")
                    .animateFloat(
                        initialValue = 0.9f,
                        targetValue  = 1.0f,
                        animationSpec = infiniteRepeatable(
                            tween(1000), RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                Box(
                    modifier = Modifier
                        .size((240 * pulse).dp)
                        .clip(CircleShape)
                        .background(NDropColors.Mint.copy(alpha = 0.05f))
                )

                // Arrow
                Icon(
                    Icons.Rounded.Navigation,
                    contentDescription = "Direction to car",
                    tint = NDropColors.Mint,
                    modifier = Modifier
                        .size(100.dp)
                        .rotate(animatedRotation)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                formatDistance(distanceM),
                style = NDropTypography.displayLarge.copy(fontSize = 48.sp),
                color = NDropColors.Mint
            )

            if (spot.floorNote.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NDropColors.MintDim)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("📍 ${spot.floorNote}",
                        style = NDropTypography.labelLarge,
                        color = NDropColors.Mint)
                }
            }
        } else {
            Text("🅿️", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "No car parked",
                style = NDropTypography.titleLarge,
                color = NDropColors.WhiteMuted,
                textAlign = TextAlign.Center
            )
            Text(
                "Scan your Parking tag first",
                style = NDropTypography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

// ── Math helpers ──────────────────────────────────────────────────────────────

private fun calculateBearing(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Float {
    val dLng = Math.toRadians(toLng - fromLng)
    val lat1  = Math.toRadians(fromLat)
    val lat2  = Math.toRadians(toLat)
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    return ((Math.toDegrees(atan2(y, x)).toFloat() + 360) % 360)
}

private fun approximateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return (6_371_000 * 2 * atan2(sqrt(a), sqrt(1 - a))).toFloat()
}

private fun formatDistance(meters: Float): String = when {
    meters < 1000 -> "${meters.toInt()}m"
    else          -> "${"%.1f".format(meters / 1000)}km"
}
