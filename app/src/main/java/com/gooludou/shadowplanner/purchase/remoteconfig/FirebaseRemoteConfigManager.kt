package com.gooludou.shadowplanner.purchase.remoteconfig

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.gooludou.shadowplanner.BuildConfig
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.purchase.model.PurchaseCatalogState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class FirebaseRemoteConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) : RemoteConfigManager {
    private val remoteConfig = FirebaseRemoteConfig.getInstance()
    private val mutex = Mutex()
    private var initialized = false
    private val _purchaseCatalog = MutableStateFlow<PurchaseCatalogState>(
        PurchaseCatalogState.Loading
    )

    override val purchaseCatalog: StateFlow<PurchaseCatalogState> =
        _purchaseCatalog.asStateFlow()

    override suspend fun initialize() {
        mutex.withLock {
            if (initialized) return
            initialized = true
            runCatching {
                remoteConfig.setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(fetchIntervalSeconds())
                        .build()
                ).awaitResult()
                remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).awaitResult()
            }.onFailure { error ->
                Log.w(TAG, "Unable to initialize Remote Config defaults", error)
            }
            publishCurrentValue()
            fetchAndPublish()
        }
    }

    override suspend fun refresh() {
        mutex.withLock {
            if (!initialized) {
                initialized = true
                runCatching {
                    remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).awaitResult()
                }
                publishCurrentValue()
            }
            fetchAndPublish()
        }
    }

    private suspend fun fetchAndPublish() {
        runCatching { remoteConfig.fetchAndActivate().awaitResult() }
            .onSuccess { publishCurrentValue() }
            .onFailure { error ->
                Log.w(TAG, "Unable to refresh Remote Config", error)
                if (_purchaseCatalog.value !is PurchaseCatalogState.Ready) {
                    _purchaseCatalog.value = PurchaseCatalogState.Error(
                        context.getString(R.string.remote_config_unavailable)
                    )
                }
            }
    }

    private fun publishCurrentValue() {
        val rawValue = remoteConfig.getString(PURCHASE_CONFIG_KEY)
        PurchaseCatalogParser.parse(rawValue)
            .onSuccess { catalog ->
                _purchaseCatalog.value = PurchaseCatalogState.Ready(catalog)
            }
            .onFailure { error ->
                Log.w(TAG, "Ignoring invalid purchase catalog", error)
                if (_purchaseCatalog.value !is PurchaseCatalogState.Ready) {
                    _purchaseCatalog.value = PurchaseCatalogState.Error(
                        context.getString(R.string.purchase_configuration_invalid)
                    )
                }
            }
    }

    private fun fetchIntervalSeconds(): Long = if (BuildConfig.DEBUG) {
        DEBUG_FETCH_INTERVAL_SECONDS
    } else {
        PRODUCTION_FETCH_INTERVAL_SECONDS
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception
                        ?: IllegalStateException(
                            context.getString(R.string.remote_config_task_failed)
                        )
                )
            }
        }
    }

    private companion object {
        const val TAG = "RemoteConfigManager"
        const val PURCHASE_CONFIG_KEY = "inapp_purchase_android"
        const val DEBUG_FETCH_INTERVAL_SECONDS = 60L
        const val PRODUCTION_FETCH_INTERVAL_SECONDS = 12L * 60L * 60L
    }
}
