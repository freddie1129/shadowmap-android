package com.gooludou.shadowplanner.app.navigation

/** App-level destinations. Map viewing and drawing are modes of the single Map workspace. */
sealed interface AppDestination {
    data class Onboarding(val isReplay: Boolean = false) : AppDestination
    data object Map : AppDestination
    data object Settings : AppDestination
    data object DeveloperSettings : AppDestination
    data object About : AppDestination
    data object LocationSearch : AppDestination
    data object Projects : AppDestination
}
