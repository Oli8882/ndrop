package com.olii.ndrop.nfc

import android.net.Uri
import com.olii.ndrop.data.model.Drop

/**
 * NDrop — TreasureCodec (Treasure Trail)
 *
 * Encodes a Discovery drop's location + identity onto a physical tag so
 * anyone's NDrop install can read it back — no server, no accounts. Leave a
 * seeded tag somewhere interesting and whoever finds it can save your pin
 * straight into their own Discoveries.
 *
 * Payload shape: "ndroptrail:<lat>:<lng>:<encodedTitle>:<encodedEmoji>:<encodedCollection>"
 * Free-text fields are Uri-encoded (which escapes ':') so the fixed-count
 * split below is unambiguous even if a title contains a colon.
 *
 * Signature: Olii-8882
 */
object TreasureCodec {

    private const val PREFIX = "ndroptrail:"

    data class FoundTreasure(
        val latitude: Double,
        val longitude: Double,
        val title: String,
        val emoji: String,
        val collectionName: String
    )

    fun encode(drop: Drop): String = buildString {
        append(PREFIX)
        append(drop.latitude).append(':')
        append(drop.longitude).append(':')
        append(Uri.encode(drop.title)).append(':')
        append(Uri.encode(drop.emoji)).append(':')
        append(Uri.encode(drop.collectionName))
    }

    fun decode(payload: String): FoundTreasure? {
        if (!payload.startsWith(PREFIX)) return null
        val parts = payload.removePrefix(PREFIX).split(":", limit = 5)
        if (parts.size != 5) return null
        return try {
            FoundTreasure(
                latitude = parts[0].toDouble(),
                longitude = parts[1].toDouble(),
                title = Uri.decode(parts[2]),
                emoji = Uri.decode(parts[3]),
                collectionName = Uri.decode(parts[4])
            )
        } catch (_: NumberFormatException) {
            null
        }
    }
}
