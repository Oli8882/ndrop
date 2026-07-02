package com.olii.ndrop.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olii.ndrop.service.NotificationHelper
import com.olii.ndrop.service.TimerForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * NDrop — TimerViewModel
 *
 * Drives the Timer Tag feature. Supports:
 *  - Preset durations (15m, 30m, 1h, 2h)
 *  - Custom duration via time picker (hours + minutes)
 *  - Pause / Resume / Cancel
 *  - Background execution via TimerForegroundService
 *  - Live countdown in UI while foregrounded
 *  - Notification on expiry even when app is closed
 *
 * Signature: Olii-8882
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val notificationHelper: NotificationHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }

    data class TimerUiState(
        val state: TimerState      = TimerState.IDLE,
        val totalSeconds: Long     = 0L,
        val remainingSeconds: Long = 0L,
        val label: String          = "Timer",
        val progress: Float        = 1f,
        val showCustomPicker: Boolean = false  // NEW: toggle custom input UI
    )

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // ── Presets ───────────────────────────────────────────────────────────────

    data class Preset(val label: String, val seconds: Long)

    val presets = listOf(
        Preset("15m",  15 * 60L),
        Preset("30m",  30 * 60L),
        Preset("1h",   60 * 60L),
        Preset("2h",   120 * 60L)
    )

    // ── Actions ───────────────────────────────────────────────────────────────

    fun startTimer(totalSeconds: Long, label: String = "Timer") {
        if (totalSeconds <= 0) return
        timerJob?.cancel()

        _uiState.value = TimerUiState(
            state            = TimerState.RUNNING,
            totalSeconds     = totalSeconds,
            remainingSeconds = totalSeconds,
            label            = label,
            progress         = 1f
        )

        // Start background service so timer survives app close
        context.startForegroundService(
            TimerForegroundService.buildStartIntent(context, totalSeconds, label)
        )

        tick()
    }

    fun pauseTimer() {
        if (_uiState.value.state != TimerState.RUNNING) return
        timerJob?.cancel()
        // Cancel background service when paused
        context.startService(TimerForegroundService.buildCancelIntent(context))
        _uiState.update { it.copy(state = TimerState.PAUSED) }
    }

    fun resumeTimer() {
        if (_uiState.value.state != TimerState.PAUSED) return
        val remaining = _uiState.value.remainingSeconds
        _uiState.update { it.copy(state = TimerState.RUNNING) }
        // Restart background service with remaining time
        context.startForegroundService(
            TimerForegroundService.buildStartIntent(context, remaining, _uiState.value.label)
        )
        tick()
    }

    fun cancelTimer() {
        timerJob?.cancel()
        context.startService(TimerForegroundService.buildCancelIntent(context))
        _uiState.value = TimerUiState()
    }

    fun addTime(seconds: Long) {
        _uiState.update { state ->
            val newRemaining = state.remainingSeconds + seconds
            val newTotal     = maxOf(state.totalSeconds, newRemaining)
            state.copy(
                totalSeconds     = newTotal,
                remainingSeconds = newRemaining,
                progress         = newRemaining.toFloat() / newTotal.toFloat()
            )
        }
    }

    // ── Custom picker ─────────────────────────────────────────────────────────

    fun showCustomPicker()  { _uiState.update { it.copy(showCustomPicker = true) } }
    fun hideCustomPicker()  { _uiState.update { it.copy(showCustomPicker = false) } }

    fun startCustomTimer(hours: Int, minutes: Int, seconds: Int, label: String) {
        val total = (hours * 3600L) + (minutes * 60L) + seconds
        hideCustomPicker()
        startTimer(total, label.ifBlank { buildLabel(hours, minutes, seconds) })
    }

    private fun buildLabel(h: Int, m: Int, s: Int): String = buildString {
        if (h > 0) append("${h}h ")
        if (m > 0) append("${m}m ")
        if (s > 0) append("${s}s")
    }.trim().ifBlank { "Timer" }

    // ── Internal tick loop ────────────────────────────────────────────────────

    private fun tick() {
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1_000L)
                _uiState.update { state ->
                    val newRemaining = state.remainingSeconds - 1
                    state.copy(
                        remainingSeconds = newRemaining,
                        progress = if (state.totalSeconds > 0)
                            newRemaining.toFloat() / state.totalSeconds.toFloat()
                        else 0f
                    )
                }
            }
            _uiState.update { it.copy(state = TimerState.FINISHED, progress = 0f) }
            // Service fires the notification — ViewModel just updates UI
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}

fun Long.toTimerDisplay(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
