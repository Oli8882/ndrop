package com.olii.ndrop.util

/**
 * NDrop — shared countdown formatter.
 * Used by both TimerViewModel (UI) and TimerForegroundService (notification)
 * so the displayed time is always identical in both places.
 * Signature: Olii-8882
 */
fun Long.toTimerDisplay(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
