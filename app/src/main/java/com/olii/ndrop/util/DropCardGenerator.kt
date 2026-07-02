package com.olii.ndrop.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.FileProvider
import com.olii.ndrop.data.model.Drop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * NDrop — DropCardGenerator (Feature 9)
 *
 * Generates a 1080x1080 shareable image card for a Discovery drop.
 * Drawn entirely with Android Canvas — no external library needed.
 * Triggers native share sheet on completion.
 *
 * Card layout:
 *   - Deep navy background
 *   - Large emoji centered
 *   - Drop title
 *   - Collection name
 *   - Coordinates
 *   - "Saved with NDrop" branding
 *
 * Signature: Olii-8882
 */
object DropCardGenerator {

    fun shareDropCard(context: Context, drop: Drop) {
        val bitmap = generateCard(drop)

        val file = File(context.cacheDir, "ndrop_card_${drop.id}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "📍 ${drop.title} — saved with NDrop")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${drop.title}"))
    }

    private fun generateCard(drop: Drop): Bitmap {
        val size   = 1080
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = Color.parseColor("#0A0E1A") }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Subtle grid lines
        val gridPaint = Paint().apply {
            color   = Color.parseColor("#111828")
            strokeWidth = 1f
        }
        for (i in 0..size step 80) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), size.toFloat(), gridPaint)
            canvas.drawLine(0f, i.toFloat(), size.toFloat(), i.toFloat(), gridPaint)
        }

        // Accent circle behind emoji
        val accentPaint = Paint().apply {
            color  = Color.parseColor("#1A2D2A")
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, 360f, 200f, accentPaint)

        // Emoji (drawn as text)
        val emojiPaint = Paint().apply {
            textSize    = 160f
            textAlign   = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(drop.emoji, size / 2f, 420f, emojiPaint)

        // Collection tag
        val collectionPaint = Paint().apply {
            color    = Color.parseColor("#FFB547")
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            drop.collectionName.uppercase(),
            size / 2f, 560f, collectionPaint
        )

        // Title
        val titlePaint = Paint().apply {
            color    = Color.parseColor("#F0F2FF")
            textSize = 72f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(drop.title, size / 2f, 660f, titlePaint)

        // Coordinates
        val coordPaint = Paint().apply {
            color    = Color.parseColor("#4A5270")
            textSize = 32f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            "%.4f, %.4f".format(drop.latitude, drop.longitude),
            size / 2f, 720f, coordPaint
        )

        // Divider
        val dividerPaint = Paint().apply {
            color       = Color.parseColor("#1A2235")
            strokeWidth = 2f
        }
        canvas.drawLine(160f, 780f, 920f, 780f, dividerPaint)

        // Branding
        val brandPaint = Paint().apply {
            color    = Color.parseColor("#2D3080")
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Saved with NDrop  ✦", size / 2f, 860f, brandPaint)

        // Date
        val datePaint = Paint().apply {
            color    = Color.parseColor("#4A5270")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            .format(Date(drop.timestamp))
        canvas.drawText(dateStr, size / 2f, 910f, datePaint)

        return bitmap
    }
}
