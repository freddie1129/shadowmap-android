package com.example.shadowmap.location

import com.mapbox.search.autocomplete.PlaceAutocomplete
import com.mapbox.search.autocomplete.PlaceAutocompleteSuggestion
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class LocationSearchRepository @Inject constructor() {
    private val autocomplete = PlaceAutocomplete.create(locationProvider = null)
    private val pendingSuggestions = ConcurrentHashMap<String, PlaceAutocompleteSuggestion>()

    suspend fun search(query: String): Result<List<LocationSearchResult>> {
        val response = autocomplete.suggestions(query = query)
        if (!response.isValue) {
            return Result.failure(
                IllegalStateException("Mapbox search failed: ${response.error}")
            )
        }
        return Result.success(
            response.value.orEmpty().mapIndexed { index, suggestion ->
                val id = "${suggestion.name}:${suggestion.formattedAddress}:$index"
                pendingSuggestions[id] = suggestion
                LocationSearchResult(
                    id = id,
                    name = suggestion.name,
                    address = suggestion.formattedAddress.orEmpty(),
                    latitude = suggestion.coordinate?.latitude(),
                    longitude = suggestion.coordinate?.longitude()
                )
            }
        )
    }

    suspend fun select(result: LocationSearchResult): Result<LocationSearchResult> {
        val suggestion = pendingSuggestions[result.id]
            ?: return Result.failure(IllegalStateException("Search result is no longer available"))
        val response = autocomplete.select(suggestion)
        return if (!response.isValue) {
            Result.failure(
                IllegalStateException("Mapbox selection failed: ${response.error}")
            )
        } else {
            val selected = response.value
            val coordinate = selected?.coordinate
            if (selected == null) {
                Result.failure(
                    IllegalStateException("Mapbox returned no location for the selected result")
                )
            } else if (coordinate == null) {
                Result.failure(
                    IllegalStateException("Mapbox returned no coordinates for the selected result")
                )
            } else {
                Result.success(
                    result.copy(
                        id = selected.mapboxId ?: result.id,
                        latitude = coordinate.latitude(),
                        longitude = coordinate.longitude()
                    )
                )
            }
        }
    }
}
