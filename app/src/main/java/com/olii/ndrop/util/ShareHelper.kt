package com.olii.ndrop.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.olii.ndrop.data.model.Drop
import com.olii.ndrop.data.model.ParkingSpot

/**
 * NDrop — ShareHelper
 *
 * Builds shareable deep links for Discovery drops and parking spots.
 * Uses the universal geo: URI which all major map apps handle.
 *
 * Link format examples:
 *   geo:41.0082,28.9784?q=41.0082,28.9784(My+Discovery+%231)
 *   https://maps.google.com/maps?q=41.0082,28.9784
 *
 * Signature: Olii-8882
 */
object ShareHelper {

    /**
     * Share a Discovery drop — opens native share sheet.
     * Works with Google Maps, Apple Maps (via browser), WhatsApp, Messages, etc.
     */
    fun shareDrop(context: Context, drop: Drop) {
        val encodedTitle = Uri.encode(drop.title)
        val lat = drop.latitude
        val lng = drop.longitude

        // Universal fallback URL — works in any browser, opens in Maps
        val mapsUrl = "https://maps.google.com/maps?q=$lat,$lng($encodedTitle)"

        val shareText = buildString {
            append("📍 ${drop.title}\n")
            append("Saved with NDrop\n\n")
            append(mapsUrl)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, drop.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(
            Intent.createChooser(intent, "Share ${drop.title} via…")
        )
    }

    /**
     * Share parked car location.
     */
    fun shareParking(context: Context, spot: ParkingSpot) {
        val lat = spot.latitude
        val lng = spot.longitude
        val mapsUrl = "https://maps.google.com/maps?q=$lat,$lng(My+Parked+Car)"

        val shareText = buildString {
            append("🅿️ My car is parked here\n")
            append("Tracked with NDrop\n\n")
            append(mapsUrl)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My Parked Car")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(
            Intent.createChooser(intent, "Share parking location via…")
        )
    }

    /**
     * Open a location directly in the user's preferred map app.
     * Falls back to browser if no map app is installed.
     */
    fun openInMaps(context: Context, lat: Double, lng: Double, title: String) {
        val encodedTitle = Uri.encode(title)
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedTitle)")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            // Fallback — open in browser
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://maps.google.com/maps?q=$lat,$lng($encodedTitle)")
            )
            context.startActivity(browserIntent)
        }
    }
}
