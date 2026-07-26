package com.gooludou.shadowplanner.navigation

/**
 * App navigation flow:
 *
 * Map
 * ├── LocationSearch -> Map
 * ├── Projects -> Map
 * ├── Settings
 * └── Scene
 *     ├── Map2D
 *     ├── Filament3D -> Map2D
 *     └── Mapbox3D -> Map2D
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
