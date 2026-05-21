package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SavedTrain
import com.example.data.TrainDatabase
import com.example.data.TrainRepository
import com.example.network.TrainApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val response: com.example.network.TrainResponse) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class TrainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TrainRepository
    
    init {
        val database = TrainDatabase.getDatabase(application)
        repository = TrainRepository(database.trainDao(), TrainApiService.create())
    }

    val savedTrains: StateFlow<List<SavedTrain>> = repository.savedTrains
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchTrain(trainNo: String) {
        val trimmed = trainNo.trim()
        if (trimmed.isEmpty()) return
        
        _searchQuery.value = trimmed
        _searchUiState.value = SearchUiState.Loading

        viewModelScope.launch {
            try {
                val response = repository.getTrainStatus(trimmed)
                if (response.success && response.data != null) {
                    _searchUiState.value = SearchUiState.Success(response)
                    
                    // Save to local database (history)
                    val name = response.trainName ?: "$trimmed Train"
                    repository.saveTrain(trimmed, name)
                } else {
                    val msg = response.message ?: "Failed to find train details for $trimmed. Please check the train number."
                    _searchUiState.value = SearchUiState.Error(msg)
                }
            } catch (e: Exception) {
                _searchUiState.value = SearchUiState.Error(
                    e.localizedMessage ?: "Network error. Please check your connection and try again."
                )
            }
        }
    }

    fun toggleFavorite(train: SavedTrain) {
        viewModelScope.launch {
            repository.toggleFavorite(train)
        }
    }

    fun deleteSavedTrain(trainNo: String) {
        viewModelScope.launch {
            repository.deleteTrain(trainNo)
        }
    }
    
    fun clearSearchState() {
        _searchUiState.value = SearchUiState.Idle
        _searchQuery.value = ""
    }
}
