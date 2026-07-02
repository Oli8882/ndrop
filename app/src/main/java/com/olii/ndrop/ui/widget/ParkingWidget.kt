package com.olii.ndrop.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.olii.ndrop.MainActivity
import com.olii.ndrop.R
import com.olii.ndrop.data.db.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * NDrop — ParkingWidget
 *
 * Traditional RemoteViews AppWidget — no Glance dependency needed.
 * Shows parked car status on the home screen with a Navigate button.
 *
 * Signature: Olii-8882
 */
class ParkingWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db   = DatabaseProvider.getInstance(context)
                val spot = db.parkingDao().getParkingSpot().firstOrNull()

                val views = RemoteViews(context.packageName, R.layout.widget_parking_layout)

                if (spot != null) {
                    val sdf  = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    val time = sdf.format(Date(spot.timestamp))

                    views.setTextViewText(R.id.widget_title, "🅿️  Car Parked")
                    views.setTextViewText(R.id.widget_subtitle, time)

                    // Navigate button — fires geo intent directly
                    val geoUri    = Uri.parse("geo:${spot.latitude},${spot.longitude}?q=${spot.latitude},${spot.longitude}(My+Car)")
                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                    val mapPending = PendingIntent.getActivity(
                        context, 0, mapIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_navigate_btn, mapPending)
                    views.setViewVisibility(R.id.widget_navigate_btn, android.view.View.VISIBLE)
                } else {
                    views.setTextViewText(R.id.widget_title, "🅿️  No car parked")
                    views.setTextViewText(R.id.widget_subtitle, "Scan your parking tag")
                    views.setViewVisibility(R.id.widget_navigate_btn, android.view.View.GONE)
                }

                // Tap widget body → open app
                val appIntent = Intent(context, MainActivity::class.java)
                val appPending = PendingIntent.getActivity(
                    context, 1, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, appPending)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}