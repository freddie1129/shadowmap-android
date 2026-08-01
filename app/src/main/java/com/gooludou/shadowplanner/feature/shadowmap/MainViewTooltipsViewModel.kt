package com.gooludou.shadowplanner.feature.shadowmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gooludou.shadowplanner.data.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewTooltipsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    val hasCompleted: StateFlow<Boolean> = dataStoreManager.hasCompletedMainViewTooltips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun markCompleted() {
        viewModelScope.launch { dataStoreManager.markMainViewTooltipsCompleted() }
    }
}
