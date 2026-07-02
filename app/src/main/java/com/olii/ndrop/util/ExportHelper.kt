package com.olii.ndrop.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.olii.ndrop.data.model.Drop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * NDrop — ExportHelper (Feature 8)
 *
 * Generates a .gpx file from all Discovery drops and
 * triggers the native share sheet so the user can send it
 * to Google Maps, OsmAnd, Komoot, or any GPX-compatible app.
 *
 * Signature: Olii-8882
 */
object ExportHelper {

    fun exportDropsAsGpx(context: Context, drops: List<Drop>) {
        if (drops.isEmpty()) return

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val gpx = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="NDrop"
                |  xmlns="http://www.topografix.com/GPX/1/1"
                |  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                |  xsi:schemaLocation="http://www.topografix.com/GPX/1/1
                |  http://www.topografix.com/GPX/1/1/gpx.xsd">""".trimMargin())
            appendLine("  <metadata>")
            appendLine("    <name>NDrop Discoveries</name>")
            appendLine("    <time>${sdf.format(Date())}</time>")
            appendLine("  </metadata>")

            drops.forEach { drop ->
                appendLine("""  <wpt lat="${drop.latitude}" lon="${drop.longitude}">""")
                appendLine("    <name>${drop.title.escapeXml()}</name>")
                appendLine("    <desc>${drop.collectionName.escapeXml()}</desc>")
                appendLine("    <time>${sdf.format(Date(drop.timestamp))}</time>")
                appendLine("    <sym>City</sym>")
                appendLine("  </wpt>")
            }
            appendLine("</gpx>")
        }

        // Write to cache dir — accessible via FileProvider
        val file = File(context.cacheDir, "ndrop_discoveries.gpx")
        file.writeText(gpx)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "NDrop Discoveries")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Export ${drops.size} places via…"))
    }

    private fun String.escapeXml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
