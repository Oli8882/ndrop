package com.olii.ndrop.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.olii.ndrop.data.model.Drop
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NDrop — NfcWriter (Feature 10 + Treasure Trail)
 *
 * Writes an NDEF text record to a blank or writable NTag213. Two payload
 * shapes share the same low-level write path:
 *  - Tag self-identification: "ndrop:TYPE:label" (writeTagInfo)
 *  - A seeded Discovery for someone else to find: TreasureCodec's format
 *    (writeDropInfo)
 *
 * ⚠️ NDEF write requires the tag to be held still for ~300ms.
 *    Always wrap write() in a try-catch at the call site.
 *
 * ⚠️ This does NOT use the UID for security — the UID is still the
 *    routing key. The NDEF payload is informational only.
 *
 * Signature: Olii-8882
 */
@Singleton
class NfcWriter @Inject constructor() {

    sealed class WriteResult {
        object Success : WriteResult()
        data class Failure(val reason: String) : WriteResult()
    }

    /**
     * Writes tag type + label to the tag as a plain text NDEF record.
     * Called from the Tag Registration Sheet when user confirms.
     */
    fun writeTagInfo(tag: Tag, tagType: TagType, label: String): WriteResult =
        writeText(tag, "ndrop:${tagType.name}:$label")

    /**
     * Writes a Discovery drop's location + identity to the tag so any NDrop
     * install can read it back and save it — the Treasure Trail feature.
     * Called when the user chooses "Leave as Treasure" on one of their drops.
     */
    fun writeDropInfo(tag: Tag, drop: Drop): WriteResult =
        writeText(tag, TreasureCodec.encode(drop))

    private fun writeText(tag: Tag, payload: String): WriteResult {
        val record  = NdefRecord.createTextRecord("en", payload)
        val message = NdefMessage(arrayOf(record))

        return try {
            // Try Ndef first (already formatted)
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return WriteResult.Failure("Tag is read-only")
                }
                if (ndef.maxSize < message.toByteArray().size) {
                    ndef.close()
                    return WriteResult.Failure("Tag too small")
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                WriteResult.Success
            } else {
                // Try NdefFormatable (blank tag, needs formatting first)
                val formatable = NdefFormatable.get(tag)
                    ?: return WriteResult.Failure("Tag does not support NDEF")
                formatable.connect()
                formatable.format(message)
                formatable.close()
                WriteResult.Success
            }
        } catch (e: Exception) {
            WriteResult.Failure(e.message ?: "Write failed")
        }
    }
}
