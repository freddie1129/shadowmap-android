package com.example.shadowmap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shadowmap.location.LocationSearchRepository
import com.example.shadowmap.location.LocationSearchResult
import com.example.shadowmap.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationSearchUiState(
    val query: String = "",
    val results: List<LocationSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessageRes: Int? = null,
    val selectedLocation: LocationSearchResult? = null
)

@HiltViewModel
class LocationSearchViewModel @Inject constructor(
    private val repository: LocationSearchRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocationSearchUiState())
    val uiState: StateFlow<LocationSearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, errorMessageRes = null) }
        searchJob?.cancel()
        if (query.trim().length < MIN_QUERY_LENGTH) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            _uiState.update { it.copy(isLoading = true, results = emptyList()) }
            repository.search(query.trim()).fold(
                onSuccess = { results ->
                    _uiState.update { it.copy(results = results, isLoading = false) }
                },
                onFailure = { error ->
                    android.util.Log.e("LocationSearch", "Search failed", error)
                    _uiState.update {
                        it.copy(
                            results = emptyList(),
                            isLoading = false,
                            errorMessageRes = R.string.location_search_failed
                        )
                    }
                }
            )
        }
    }

    fun clear() {
        searchJob?.cancel()
        _uiState.value = LocationSearchUiState()
    }

    fun select(result: LocationSearchResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
            repository.select(result).fold(
                onSuccess = { selected ->
                    _uiState.update { it.copy(isLoading = false, selectedLocation = selected) }
                },
                onFailure = { error ->
                    android.util.Log.e("LocationSearch", "Selection failed", error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = R.string.location_selection_failed
                        )
                    }
                }
            )
        }
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val SEARCH_DEBOUNCE_MILLIS = 250L
    }
}
