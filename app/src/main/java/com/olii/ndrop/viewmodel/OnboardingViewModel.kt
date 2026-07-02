package com.olii.ndrop.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NDrop — OnboardingViewModel
 *
 * Reads/writes a single DataStore flag: "onboarding_complete".
 * The UI observes [showOnboarding] — true until the user completes or skips.
 *
 * Signature: Olii-8882
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_complete")
    }

    val showOnboarding: StateFlow<Boolean?> = dataStore.data
        .map { prefs -> !(prefs[KEY_ONBOARDING_DONE] ?: false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null) // null = loading

    fun completeOnboarding() {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_ONBOARDING_DONE] = true }
        }
    }
}