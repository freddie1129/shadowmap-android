package com.gooludou.shadowplanner.navigation

/**
 * Scene navigation flow:
 *
 * Mapbox3D
 * └── Map2D
 *     └── Mapbox3D
 *
 * Other app destinations are Map, LocationSearch, Projects, and Settings.
 */
sealed interface AppDestination {
    data object Map : AppDestination
    data object Settings : AppDestination
    data object LocationSearch : AppDestination
    data object Projects : AppDestination

    sealed interface Scene : AppDestination {
        data object Map2D : Scene
        data object Filament3D : Scene
        data object Mapbox3D : Scene
    }
}
