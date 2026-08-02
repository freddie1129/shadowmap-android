package com.gooludou.shadowplanner.purchase.developer

import android.content.Context
import androidx.core.content.edit
import com.gooludou.shadowplanner.BuildConfig
import com.gooludou.shadowplanner.purchase.model.EntitlementState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DeveloperSettings @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _forcePremium = MutableStateFlow(
        BuildConfig.DEBUG && preferences.getBoolean(FORCE_PREMIUM_KEY, false)
    )
    val forcePremium: StateFlow<Boolean> = _forcePremium.asStateFlow()

    fun setForcePremium(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        preferences.edit { putBoolean(FORCE_PREMIUM_KEY, enabled) }
        _forcePremium.value = enabled
    }

    fun clear() {
        if (!BuildConfig.DEBUG) return
        preferences.edit { clear() }
        _forcePremium.value = false
    }

    private companion object {
        const val PREFERENCES_NAME = "developer_settings"
        const val FORCE_PREMIUM_KEY = "force_premium"
    }
}

object DeveloperEntitlementOverride {
    fun resolve(
        entitlement: EntitlementState,
        forcePremium: Boolean,
        isDebugBuild: Boolean = BuildConfig.DEBUG
    ): EntitlementState = if (isDebugBuild && forcePremium) {
        EntitlementState.Premium
    } else {
        entitlement
    }
}
