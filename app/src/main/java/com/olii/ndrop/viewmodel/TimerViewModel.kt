package com.olii.ndrop.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
 *  - Survives process death: the running/paused timer's absolute end-time (or
 *    paused remaining) is persisted to DataStore and rehydrated in init(), so
 *    reopening the app after the process was killed reconnects to the timer
 *    that TimerForegroundService kept counting down in the background.
 *
 * Signature: Olii-8882
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val notificationHelper: NotificationHelper,
    private val dataStore: DataStore<Preferences>,
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

    // ── Persistence keys ──────────────────────────────────────────────────────

    private companion object {
        val KEY_END_AT           = longPreferencesKey("timer_end_at")
        val KEY_TOTAL_SECONDS    = longPreferencesKey("timer_total_seconds")
        val KEY_PAUSED_REMAINING = longPreferencesKey("timer_paused_remaining")
        val KEY_LABEL            = stringPreferencesKey("timer_label")
    }

    // ── Presets ───────────────────────────────────────────────────────────────

    data class Preset(val label: String, val seconds: Long)

    val presets = listOf(
        Preset("15m",  15 * 60L),
        Preset("30m",  30 * 60L),
        Preset("1h",   60 * 60L),
        Preset("2h",   120 * 60L)
    )

    init {
        rehydrateFromPersistedTimer()
    }

    /** Reconnects to a still-running (or paused) timer after process death. */
    private fun rehydrateFromPersistedTimer() {
        viewModelScope.launch {
            val prefs           = dataStore.data.first()
            val label           = prefs[KEY_LABEL] ?: "Timer"
            val total           = prefs[KEY_TOTAL_SECONDS] ?: 0L
            val endAt           = prefs[KEY_END_AT] ?: 0L
            val pausedRemaining = prefs[KEY_PAUSED_REMAINING] ?: -1L

            when {
                total <= 0L -> Unit // nothing persisted

                pausedRemaining >= 0L -> {
                    _uiState.value = TimerUiState(
                        state            = TimerState.PAUSED,
                        totalSeconds     = total,
                        remainingSeconds = pausedRemaining,
                        label            = label,
                        progress         = pausedRemaining.toFloat() / total.toFloat()
                    )
                }

                endAt > System.currentTimeMillis() -> {
                    val remaining = (endAt - System.currentTimeMillis()) / 1000L
                    _uiState.value = TimerUiState(
                        state            = TimerState.RUNNING,
                        totalSeconds     = total,
                        remainingSeconds = remaining,
                        label            = label,
                        progress         = remaining.toFloat() / total.toFloat()
                    )
                    // Restart the service too, in case the *whole* app — service
                    // included — was killed (e.g. force-stop), not just the UI
                    // process. Harmless no-op/refresh if the service is still alive.
                    context.startForegroundService(
                        TimerForegroundService.buildStartIntent(context, remaining, label)
                    )
                    tick(endAt)
                }

                else -> {
                    // Timer expired while the process was dead. If its foreground
                    // service survived, it already fired the expiry notification;
                    // this is a harmless best-effort catch-up notify in case the
                    // service was killed too and never got the chance to.
                    notificationHelper.showTimerExpiredNotification(label)
                    clearPersistedTimer()
                }
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun startTimer(totalSeconds: Long, label: String = "Timer") {
        if (totalSeconds <= 0) return
        timerJob?.cancel()

        val endAt = System.currentTimeMillis() + totalSeconds * 1000L
        _uiState.value = TimerUiState(
            state            = TimerState.RUNNING,
            totalSeconds     = totalSeconds,
            remainingSeconds = totalSeconds,
            label            = label,
            progress         = 1f
        )
        persistRunning(endAt, totalSeconds, label)

        // Start background service so timer survives app close
        context.startForegroundService(
            TimerForegroundService.buildStartIntent(context, totalSeconds, label)
        )

        tick(endAt)
    }

    fun pauseTimer() {
        if (_uiState.value.state != TimerState.RUNNING) return
        timerJob?.cancel()
        // Cancel background service when paused
        context.startService(TimerForegroundService.buildCancelIntent(context))
        _uiState.update { it.copy(state = TimerState.PAUSED) }
        val s = _uiState.value
        persistPaused(s.remainingSeconds, s.totalSeconds, s.label)
    }

    fun resumeTimer() {
        if (_uiState.value.state != TimerState.PAUSED) return
        val remaining = _uiState.value.remainingSeconds
        val endAt = System.currentTimeMillis() + remaining * 1000L
        _uiState.update { it.copy(state = TimerState.RUNNING) }
        persistRunning(endAt, _uiState.value.totalSeconds, _uiState.value.label)
        // Restart background service with remaining time
        context.startForegroundService(
            TimerForegroundService.buildStartIntent(context, remaining, _uiState.value.label)
        )
        tick(endAt)
    }

    fun cancelTimer() {
        timerJob?.cancel()
        context.startService(TimerForegroundService.buildCancelIntent(context))
        _uiState.value = TimerUiState()
        clearPersistedTimer()
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

        // Keep the persisted state AND the background service's own countdown
        // in sync — otherwise the service still expires (and notifies) at the
        // original duration, ignoring the added time.
        val s = _uiState.value
        when (s.state) {
            TimerState.RUNNING -> {
                timerJob?.cancel()
                val endAt = System.currentTimeMillis() + s.remainingSeconds * 1000L
                persistRunning(endAt, s.totalSeconds, s.label)
                context.startForegroundService(
                    TimerForegroundService.buildStartIntent(context, s.remainingSeconds, s.label)
                )
                tick(endAt)
            }
            TimerState.PAUSED -> persistPaused(s.remainingSeconds, s.totalSeconds, s.label)
            else -> Unit
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

    /** Ticks off the absolute [endAtMillis] rather than a plain decrementing
     *  counter, so the displayed remaining time is always correct even after
     *  rehydrating from a persisted timer or surviving a dropped frame/delay. */
    private fun tick(endAtMillis: Long) {
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = ((endAtMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                _uiState.update { state ->
                    state.copy(
                        remainingSeconds = remaining,
                        progress = if (state.totalSeconds > 0)
                            remaining.toFloat() / state.totalSeconds.toFloat()
                        else 0f
                    )
                }
                if (remaining <= 0L) break
                delay(1_000L)
            }
            _uiState.update { it.copy(state = TimerState.FINISHED, progress = 0f) }
            clearPersistedTimer()
            // Service fires the notification — ViewModel just updates UI
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun persistRunning(endAt: Long, total: Long, label: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_END_AT] = endAt
                prefs[KEY_TOTAL_SECONDS] = total
                prefs[KEY_LABEL] = label
                prefs.remove(KEY_PAUSED_REMAINING)
            }
        }
    }

    private fun persistPaused(remaining: Long, total: Long, label: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_PAUSED_REMAINING] = remaining
                prefs[KEY_TOTAL_SECONDS] = total
                prefs[KEY_LABEL] = label
                prefs.remove(KEY_END_AT)
            }
        }
    }

    private fun clearPersistedTimer() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs.remove(KEY_END_AT)
                prefs.remove(KEY_TOTAL_SECONDS)
                prefs.remove(KEY_PAUSED_REMAINING)
                prefs.remove(KEY_LABEL)
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
