package com.gooludou.shadowplanner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "shadow_planner_preferences")

/** Central access point for app-wide preferences stored with DataStore. */
@Singleton
class DataStoreManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val preferences = context.dataStore.data.catch { exception ->
        if (exception is IOException) emit(emptyPreferences()) else throw exception
    }

    val hasCompletedOnboarding: Flow<Boolean> = preferences
        .map { it[Keys.HasCompletedOnboarding] ?: false }

    val hasRequestedLocationPermission: Flow<Boolean> = preferences
        .map { it[Keys.HasRequestedLocationPermission] ?: false }

    val hasCompletedEditingTooltips: Flow<Boolean> = preferences
        .map { it[Keys.HasCompletedEditingTooltips] ?: false }

    val hasCompletedMainViewTooltips: Flow<Boolean> = preferences
        .map { it[Keys.HasCompletedMainViewTooltips] ?: false }

    suspend fun markOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[Keys.HasCompletedOnboarding] = true
        }
    }

    suspend fun markLocationPermissionRequested() {
        context.dataStore.edit { preferences ->
            preferences[Keys.HasRequestedLocationPermission] = true
        }
    }

    suspend fun markEditingTooltipsCompleted() {
        context.dataStore.edit { preferences ->
            preferences[Keys.HasCompletedEditingTooltips] = true
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun markMainViewTooltipsCompleted() {
        context.dataStore.edit { preferences ->
            preferences[Keys.HasCompletedMainViewTooltips] = true
        }
    }

    private object Keys {
        val HasCompletedOnboarding = booleanPreferencesKey("has_completed_onboarding")
        val HasRequestedLocationPermission =
            booleanPreferencesKey("has_requested_location_permission")
        val HasCompletedEditingTooltips =
            booleanPreferencesKey("has_completed_editing_tooltips")
        val HasCompletedMainViewTooltips =
            booleanPreferencesKey("has_completed_main_view_tooltips")
    }
}
