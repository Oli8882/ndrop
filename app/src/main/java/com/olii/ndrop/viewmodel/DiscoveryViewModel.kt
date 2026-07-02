package com.olii.ndrop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olii.ndrop.data.model.Drop
import com.olii.ndrop.data.repository.DropRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NDrop — DiscoveryViewModel
 * Signature: Olii-8882
 */
@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val repository: DropRepository
) : ViewModel() {

    private val _selectedCollection = MutableStateFlow("All")
    val selectedCollection: StateFlow<String> = _selectedCollection.asStateFlow()

    val allCollections: StateFlow<List<String>> = repository.allCollections
        .map { listOf("All") + it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf("All"))

    val drops: StateFlow<List<Drop>> = _selectedCollection
        .flatMapLatest { collection ->
            if (collection == "All") repository.allDrops
            else repository.getDropsByCollection(collection)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectCollection(name: String) {
        _selectedCollection.value = name
    }

    fun updateDropDetails(drop: Drop, newTitle: String, newCollection: String) {
        viewModelScope.launch {
            val updated = drop.copy(
                title = if (newTitle.isNotBlank()) newTitle.trim() else drop.title,
                collectionName = if (newCollection.isNotBlank()) newCollection.trim() else drop.collectionName
            )
            repository.updateDrop(updated)
        }
    }

    fun deleteDrop(drop: Drop) {
        viewModelScope.launch {
            repository.deleteDrop(drop)
        }
    }

    fun updateDropEmoji(drop: Drop, emoji: String) {
        viewModelScope.launch {
            repository.updateDrop(drop.copy(emoji = emoji))
        }
    }
}