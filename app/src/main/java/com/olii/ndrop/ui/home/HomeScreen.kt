package com.olii.ndrop.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.olii.ndrop.util.ShareHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.olii.ndrop.R
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.olii.ndrop.data.model.Drop
import com.olii.ndrop.data.model.ParkingSpot
import com.olii.ndrop.data.model.Streak
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography
import com.olii.ndrop.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * NDrop — HomeScreen
 * Full-bleed map with a floating parking card and bottom navigation.
 * Signature: Olii-8882
 */
@Composable
fun HomeScreen(
    onNavigateToDiscovery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTimer: () -> Unit,
    onNavigateToArCompass: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val parkingSpot           by viewModel.parkingSpot.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val streak                by viewModel.streak.collectAsStateWithLifecycle()
    val nearbyDrops           by viewModel.nearbyDrops.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val appContext = androidx.compose.ui.platform.LocalContext.current

    // Refresh nearby drops when screen appears
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) viewModel.refreshNearbyDrops()
    }

    // Show permission denied screen instead of map when location is not granted
    if (!hasLocationPermission) {
        LocationDeniedScreen(
            onRequestPermission = {
                // Re-trigger permission from the Activity via a shared event
                // For simplicity, deep-link to settings — user already denied once
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", appContext.packageName, null)
                )
                appContext.startActivity(intent)
            }
        )
        return
    }

    // Show error messages as snackbars
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    val defaultLatLng = LatLng(41.0082, 28.9784)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 14f)
    }

    LaunchedEffect(parkingSpot) {
        parkingSpot?.let { spot ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(spot.latitude, spot.longitude), 16f
                ), durationMs = 800
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NDropColors.NavySubtle,
                    contentColor = NDropColors.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // ── Full-bleed Google Map ─────────────────────────────────────────────
            val mapStyleOptions = remember(appContext) {
                MapStyleOptions.loadRawResourceStyle(appContext, R.raw.map_style_dark)
            }

            GoogleMap(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(top = 100.dp, bottom = 160.dp),
                cameraPositionState = cameraPositionState,
                uiSettings          = MapUiSettings(
                    zoomControlsEnabled    = false,
                    mapToolbarEnabled      = false,
                    myLocationButtonEnabled = false, // Turn off default, use custom
                    compassEnabled         = true
                ),
                properties = MapProperties(
                    mapType               = MapType.NORMAL,
                    isMyLocationEnabled   = true,
                    isBuildingEnabled     = true,
                    isTrafficEnabled      = true,
                    mapStyleOptions       = mapStyleOptions
                )
            ) {
                parkingSpot?.let { spot ->
                    Marker(
                        state   = MarkerState(LatLng(spot.latitude, spot.longitude)),
                        title   = spot.carLabel,
                        snippet = formatTimestamp(spot.timestamp)
                    )
                }
                // Feature 6: Nearby discovery markers
                nearbyDrops.forEach { drop ->
                    Marker(
                        state   = MarkerState(LatLng(drop.latitude, drop.longitude)),
                        title   = drop.title,
                        snippet = drop.collectionName
                    )
                }
            }

            // (FAB moved inside the bottom Column to dynamically dodge overlays)

            // ── Top bar ───────────────────────────────────────────────────────────
            TopBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // ── Bottom sheet area ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Custom Stylish My Location Button
                FloatingActionButton(
                    onClick = {
                        viewModel.getCurrentLocation { loc ->
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude), 16f
                                    ), durationMs = 800
                                )
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    containerColor = NDropColors.Indigo,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = "My Location")
                }

                // Feature 5: Streak card
                StreakCard(streak = streak)

                // Feature 6: Nearby discoveries row
                val context = LocalContext.current
                NearbyDropsRow(
                    drops = nearbyDrops,
                    onOpenInMaps = { drop ->
                        ShareHelper.openInMaps(context, drop.latitude, drop.longitude, drop.title)
                    }
                )

                // Parking card
                AnimatedVisibility(
                    visible = parkingSpot != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    parkingSpot?.let { spot ->
                        ParkingCard(
                            spot = spot,
                            onNavigate = {
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(spot.latitude, spot.longitude), 17f
                                        ), durationMs = 600
                                    )
                                }
                            },
                            onArCompass = onNavigateToArCompass,
                            onClear     = viewModel::clearParking
                        )
                    }
                }

                BottomNavRow(
                    onDiscovery   = onNavigateToDiscovery,
                    onTimer       = onNavigateToTimer,
                    onSettings    = onNavigateToSettings,
                    hasParkingSpot = parkingSpot != null
                )
            }
        } // end inner Box
    } // end Scaffold
}

@Composable
private fun TopBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NDropColors.NavyElevated.copy(alpha = 0.92f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NDrop",
            style = NDropTypography.titleLarge,
            color = NDropColors.White
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NDropColors.Mint)
            )
            Text(
                "Ready to Scan",
                style = NDropTypography.labelSmall,
                color = NDropColors.Mint
            )
        }
    }
}

@Composable
private fun ParkingCard(
    spot: ParkingSpot,
    onNavigate: () -> Unit,
    onArCompass: () -> Unit,
    onClear: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NDropColors.MintDim.copy(alpha = 0.7f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NDropColors.Mint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🅿️", style = NDropTypography.titleMedium.copy(fontSize = 20.dp.value.sp()))
            }
            Column {
                Text(spot.carLabel, style = NDropTypography.titleMedium, color = NDropColors.Mint)
                Text(
                    formatTimestamp(spot.timestamp),
                    style = NDropTypography.bodyMedium,
                    color = NDropColors.WhiteMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (spot.floorNote.isNotBlank()) {
                    Text(spot.floorNote, style = NDropTypography.labelSmall,
                        color = NDropColors.Mint)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Navigate on map
            IconButton(
                onClick = onNavigate,
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(NDropColors.Mint.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Rounded.Navigation, contentDescription = "Navigate",
                    tint = NDropColors.Mint, modifier = Modifier.size(18.dp))
            }
            // Feature 3: AR Compass walk-back
            IconButton(
                onClick = onArCompass,
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(NDropColors.Indigo.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Rounded.Explore, contentDescription = "Walk back",
                    tint = NDropColors.IndigoLight, modifier = Modifier.size(18.dp))
            }
            // Share
            IconButton(
                onClick = { ShareHelper.shareParking(context, spot) },
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(NDropColors.IndigoDim)
            ) {
                Icon(Icons.Rounded.Share, contentDescription = "Share",
                    tint = NDropColors.IndigoLight, modifier = Modifier.size(18.dp))
            }
            // Clear
            IconButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(NDropColors.Rose.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Clear",
                    tint = NDropColors.Rose, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = NDropColors.NavySubtle,
            title = {
                Text("Clear Parking Spot?", style = NDropTypography.titleMedium)
            },
            text = {
                Text(
                    "This will remove your parked car location.",
                    style = NDropTypography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    showClearConfirm = false
                }) {
                    Text("Clear", color = NDropColors.Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = NDropColors.WhiteMuted)
                }
            }
        )
    }
}

@Composable
private fun BottomNavRow(
    onDiscovery: () -> Unit,
    onTimer: () -> Unit,
    onSettings: () -> Unit,
    hasParkingSpot: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NDropColors.NavyElevated.copy(alpha = 0.95f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavButton(
            icon = Icons.Rounded.Map,
            label = "Map",
            active = true,
            activeColor = NDropColors.Indigo,
            onClick = {}
        )
        NavButton(
            icon = Icons.Rounded.BookmarkBorder,
            label = "Discovery",
            active = false,
            activeColor = NDropColors.Amber,
            onClick = onDiscovery
        )
        NavButton(
            icon = Icons.Rounded.Timer,
            label = "Timer",
            active = false,
            activeColor = NDropColors.IndigoLight,
            onClick = onTimer
        )
        NavButton(
            icon = Icons.Rounded.Settings,
            label = "Settings",
            active = false,
            activeColor = NDropColors.Indigo,
            onClick = onSettings
        )
    }
}

@Composable
private fun NavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (active) activeColor.copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (active) activeColor else NDropColors.WhiteDim
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "nav_press_scale"
    )

    Column(
        modifier = Modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(
                    bounded = true, color = activeColor
                ),
                enabled = !active,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        Text(label, style = NDropTypography.labelSmall, color = contentColor)
    }
}

// ── Nearby Drops Row (Feature 6) ─────────────────────────────────────────────

@Composable
private fun NearbyDropsRow(
    drops: List<Drop>,
    onOpenInMaps: (Drop) -> Unit
) {
    AnimatedVisibility(
        visible = drops.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Nearby  •  ${drops.size}",
                style = NDropTypography.labelSmall,
                color = NDropColors.WhiteDim,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(drops, key = { it.id }) { drop ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(NDropColors.NavyElevated)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { onOpenInMaps(drop) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(drop.emoji)
                        Text(drop.title,
                            style = NDropTypography.labelLarge,
                            color = NDropColors.WhiteMuted,
                            maxLines = 1)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun Float.sp() = androidx.compose.ui.unit.TextUnit(this, androidx.compose.ui.unit.TextUnitType.Sp)

private fun formatTimestamp(ts: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(ts))
}
