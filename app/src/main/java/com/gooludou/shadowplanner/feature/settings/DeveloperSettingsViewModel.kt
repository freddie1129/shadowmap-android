package com.gooludou.shadowplanner.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gooludou.shadowplanner.data.DataStoreManager
import com.gooludou.shadowplanner.purchase.developer.DeveloperSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DeveloperSettingsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val developerSettings: DeveloperSettings
) : ViewModel() {
    fun clearAllStoredData() {
        developerSettings.clear()
        viewModelScope.launch {
            dataStoreManager.clearAllPreferences()
        }
    }
}
