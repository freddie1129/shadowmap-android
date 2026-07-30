package com.gooludou.shadowplanner.feature.shadowmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gooludou.shadowplanner.data.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LocationPermissionViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    val hasRequestedLocationPermission: StateFlow<Boolean?> =
        dataStoreManager.hasRequestedLocationPermission
            .map<Boolean, Boolean?> { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    fun markLocationPermissionRequested() {
        viewModelScope.launch { dataStoreManager.markLocationPermissionRequested() }
    }
}
