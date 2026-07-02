package com.olii.ndrop.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olii.ndrop.data.model.RegisteredTag
import com.olii.ndrop.data.model.ScanPattern
import com.olii.ndrop.data.repository.DropRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NDrop — SettingsViewModel
 * Signature: Olii-8882
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: DropRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    // ── Theme ─────────────────────────────────────────────────────────────────
    val isDarkMode: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_DARK_MODE] ?: true } // default: dark
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun toggleTheme() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_DARK_MODE] = !(prefs[KEY_DARK_MODE] ?: true)
            }
        }
    }

    // ── Tags ──────────────────────────────────────────────────────────────────
    val registeredTags: StateFlow<List<RegisteredTag>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allDrops = repository.allDrops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allParkingSpots = repository.allParkingSpots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _tagSuggestions = MutableStateFlow<List<Pair<RegisteredTag, ScanPattern>>>(emptyList())
    val tagSuggestions: StateFlow<List<Pair<RegisteredTag, ScanPattern>>> = _tagSuggestions

    init { loadSuggestions() }

    private fun loadSuggestions() {
        viewModelScope.launch {
            repository.allTags.collect { tags ->
                val suggestions = tags.mapNotNull { tag ->
                    val pattern = repository.getScanPattern(tag.uid)
                    if (pattern != null && pattern.scanDates.split(",").size >= 3)
                        tag to pattern else null
                }
                _tagSuggestions.value = suggestions
            }
        }
    }

    fun deleteTag(tag: RegisteredTag) {
        viewModelScope.launch { repository.deleteTag(tag) }
    }

    fun clearParkingSpot(id: Int) {
        viewModelScope.launch { repository.clearParkingSpot(id) }
    }
}
