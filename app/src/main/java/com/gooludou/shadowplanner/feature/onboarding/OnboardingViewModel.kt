package com.gooludou.shadowplanner.feature.onboarding

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
class OnboardingViewModel @Inject constructor(private val dataStoreManager: DataStoreManager) :
    ViewModel() {
    val hasCompletedOnboarding: StateFlow<Boolean?> = dataStoreManager.hasCompletedOnboarding
        .map<Boolean, Boolean?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun markCompleted() {
        viewModelScope.launch { dataStoreManager.markOnboardingCompleted() }
    }
}
