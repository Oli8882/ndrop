package com.olii.ndrop.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.os.Build
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NDrop — NfcManager
 *
 * Handles foreground dispatch setup/teardown and UID extraction.
 *
 * ⚠️ Foreground dispatch MUST be enabled in onResume() and disabled in onPause().
 *    Failure to disable causes ghost scans when the app is backgrounded.
 *
 * ⚠️ Debounce: NTag213 held against phone can fire 3-5 intents in <2s.
 *    We enforce a 1500ms cooldown between processed scans.
 *
 * ⚠️ UID as routing key only — never used for auth/security.
 *
 * Signature: Olii-8882
 */
@Singleton
class NfcManager @Inject constructor() {

    private val _scanEvents = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 1)
    val scanEvents = _scanEvents.asSharedFlow()

    private var lastScanTimestamp = 0L
    private val DEBOUNCE_MS = 1500L

    private var nfcAdapter: NfcAdapter? = null

    data class ScanEvent(val uid: String, val rawTag: Tag)

    /**
     * Call from MainActivity.onResume()
     */
    fun enableForegroundDispatch(activity: Activity) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity) ?: return

        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)

        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        )

        val techLists = arrayOf(
            arrayOf(NfcA::class.java.name)
        )

        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
    }

    /**
     * Call from MainActivity.onPause()
     */
    fun disableForegroundDispatch(activity: Activity) {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    /**
     * Call from MainActivity.onNewIntent() with the NFC intent.
     * Returns true if the intent was an NFC scan, false otherwise.
     */
    fun processIntent(intent: Intent): Boolean {
        val action = intent.action ?: return false
        if (action !in listOf(
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED,
                NfcAdapter.ACTION_NDEF_DISCOVERED
            )
        ) return false

        // ── Debounce ─────────────────────────────────────────────────────────
        val now = System.currentTimeMillis()
        if (now - lastScanTimestamp < DEBOUNCE_MS) return true // Silently swallow duplicate
        lastScanTimestamp = now

        // ── Extract Tag & UID ─────────────────────────────────────────────────
        val tag: Tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java) ?: return false
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return false
        }

        val uid = tag.id.toHexString()
        _scanEvents.tryEmit(ScanEvent(uid = uid, rawTag = tag))
        return true
    }

    fun isNfcAvailable(activity: Activity): Boolean {
        return NfcAdapter.getDefaultAdapter(activity) != null
    }

    fun isNfcEnabled(activity: Activity): Boolean {
        return NfcAdapter.getDefaultAdapter(activity)?.isEnabled == true
    }
}

/**
 * Converts a ByteArray NFC UID to a colon-separated hex string.
 * e.g. [0x04, 0xA1, 0xB2] → "04:A1:B2"
 */
fun ByteArray.toHexString(): String =
    joinToString(":") { "%02X".format(it) }
