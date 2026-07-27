package com.gooludou.shadowplanner.navigation

/** App-level destinations. Map viewing and drawing are modes of the single Map workspace. */
sealed interface AppDestination {
    data object Map : AppDestination
    data object Settings : AppDestination
    data object LocationSearch : AppDestination
    data object Projects : AppDestination
}
