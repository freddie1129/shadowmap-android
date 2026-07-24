package com.gooludou.shadowplanner.location

import com.gooludou.shadowplanner.domain.GeoPoint
import com.mapbox.geojson.Point
import com.mapbox.search.ApiType
import com.mapbox.search.NewQueryType
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.result.SearchResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

interface CurrentLocationResolver {
    suspend fun resolve(location: GeoPoint): Result<LocationSearchResult>
}

@Singleton
class MapboxCurrentLocationResolver @Inject constructor() : CurrentLocationResolver {
    private val searchEngine = SearchEngine.createSearchEngine(
        ApiType.GEOCODING,
        SearchEngineSettings()
    )

    override suspend fun resolve(location: GeoPoint): Result<LocationSearchResult> =
        suspendCancellableCoroutine { continuation ->
            val task = searchEngine.search(
                ReverseGeoOptions.Builder(
                    Point.fromLngLat(location.longitude, location.latitude)
                ).build(),
                object : SearchCallback {
                    override fun onResults(
                        results: List<SearchResult>,
                        responseInfo: com.mapbox.search.ResponseInfo
                    ) {
                        val result = results.firstOrNull {
                            NewQueryType.ADDRESS in it.newTypes
                        } ?: results.firstOrNull {
                            NewQueryType.STREET in it.newTypes
                        } ?: results.firstOrNull()
                        if (result == null) {
                            continuation.resume(
                                Result.failure(
                                    IllegalStateException(
                                        "Mapbox returned no result for current location"
                                    )
                                )
                            )
                            return
                        }
                        continuation.resume(
                            Result.success(
                                LocationSearchResult(
                                    id = result.mapboxId?.ifBlank { result.id.orEmpty() }
                                        ?: result.id.orEmpty(),
                                    name = result.name.orEmpty()
                                        .ifBlank { result.fullAddress.orEmpty() },
                                    address = result.fullAddress.orEmpty()
                                        .ifBlank { result.descriptionText.orEmpty() },
                                    latitude = location.latitude,
                                    longitude = location.longitude
                                )
                            )
                        )
                    }

                    override fun onError(e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }
            )
            continuation.invokeOnCancellation { task.cancel() }
        }
}
