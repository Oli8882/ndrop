package com.olii.ndrop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olii.ndrop.nfc.NfcManager
import com.olii.ndrop.ui.NDropNavGraph
import com.olii.ndrop.ui.home.NfcStatus
import com.olii.ndrop.ui.home.NfcUnavailableScreen
import com.olii.ndrop.ui.onboarding.OnboardingScreen
import com.olii.ndrop.ui.scan.FloorNoteOverlay
import com.olii.ndrop.ui.scan.QuickCollectionOverlay
import com.olii.ndrop.ui.scan.ScanOverlay
import com.olii.ndrop.ui.scan.TagRegistrationSheet
import com.olii.ndrop.ui.theme.NDropTheme
import com.olii.ndrop.viewmodel.HomeViewModel
import com.olii.ndrop.viewmodel.OnboardingViewModel
import com.olii.ndrop.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * NDrop — MainActivity
 * Signature: Olii-8882
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var nfcManager: NfcManager

    private val homeViewModel:     HomeViewModel     by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        homeViewModel.setLocationPermissionGranted(granted)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* optional — never block the app */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        checkAndRequestLocationPermission()
        requestNotificationPermissionIfNeeded()

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val nfcStatus  = when {
            nfcAdapter == null       -> NfcStatus.NO_HARDWARE
            !nfcAdapter.isEnabled    -> NfcStatus.DISABLED
            else                     -> NfcStatus.AVAILABLE
        }

        setContent {
            // Observe dark mode preference — drives the entire app theme
            val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()

            NDropTheme(darkTheme = isDarkMode) {
                NDropRoot(
                    homeViewModel     = homeViewModel,
                    settingsViewModel = settingsViewModel,
                    nfcStatus         = nfcStatus,
                    isDarkTheme       = isDarkMode
                )
            }
        }

        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        nfcManager.enableForegroundDispatch(this)
    }

    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        intent?.let { homeViewModel.processNfcIntent(it) }
    }

    private fun checkAndRequestLocationPermission() {
        val fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            homeViewModel.setLocationPermissionGranted(true)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
private fun NDropRoot(
    homeViewModel:     HomeViewModel,
    settingsViewModel: SettingsViewModel,
    nfcStatus:         NfcStatus,
    isDarkTheme:       Boolean
) {
    val activeScanResult      by homeViewModel.activeScanResult.collectAsStateWithLifecycle()
    val pendingUnknownUid     by homeViewModel.pendingUnknownUid.collectAsStateWithLifecycle()
    val showFloorNoteOverlay  by homeViewModel.showFloorNoteOverlay.collectAsStateWithLifecycle()
    val pendingCollectionDrop by homeViewModel.pendingCollectionDrop.collectAsStateWithLifecycle()
    val collections           by homeViewModel.allCollections.collectAsStateWithLifecycle()

    val onboardingViewModel = androidx.hilt.navigation.compose.hiltViewModel<OnboardingViewModel>()
    val showOnboarding      by onboardingViewModel.showOnboarding.collectAsStateWithLifecycle()

    var nfcWarningDismissed by remember {
        mutableStateOf(nfcStatus == NfcStatus.AVAILABLE)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        when {
            !nfcWarningDismissed -> {
                NfcUnavailableScreen(
                    status           = nfcStatus,
                    onContinueAnyway = { nfcWarningDismissed = true }
                )
            }

            showOnboarding == null -> { /* Splash screen covers loading state */ }

            showOnboarding == true -> {
                OnboardingScreen(onFinish = { onboardingViewModel.completeOnboarding() })
            }

            else -> {
                NDropNavGraph(
                    homeViewModel = homeViewModel,
                    isDarkTheme   = isDarkTheme
                )

                AnimatedVisibility(
                    visible = activeScanResult != null,
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    activeScanResult?.let { result ->
                        ScanOverlay(result = result, onDismiss = homeViewModel::dismissScanResult)
                    }
                }

                pendingUnknownUid?.let { uid ->
                    TagRegistrationSheet(
                        uid        = uid,
                        onRegister = { type, label -> homeViewModel.registerTag(uid, type, label) },
                        onDismiss  = homeViewModel::dismissRegistrationSheet
                    )
                }

                if (showFloorNoteOverlay) {
                    FloorNoteOverlay(
                        onSave    = { note -> homeViewModel.saveFloorNote(note) },
                        onDismiss = homeViewModel::dismissFloorNote
                    )
                }

                pendingCollectionDrop?.let { drop ->
                    QuickCollectionOverlay(
                        dropTitle           = drop.title,
                        existingCollections = collections,
                        onAssign            = { col -> homeViewModel.assignCollection(col) },
                        onDismiss           = homeViewModel::dismissCollectionPick
                    )
                }
            }
        }
    }
}
