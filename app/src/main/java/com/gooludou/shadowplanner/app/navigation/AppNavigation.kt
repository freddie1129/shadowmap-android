package com.gooludou.shadowplanner.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.gooludou.shadowplanner.ShadowMapRoute
import com.gooludou.shadowplanner.location.LocationSearchResult
import com.gooludou.shadowplanner.renderer.mapbox.MapboxShadowMapController
import com.gooludou.shadowplanner.feature.locationsearch.LocationSearchScreen
import com.gooludou.shadowplanner.feature.locationsearch.LocationSearchViewModel
import com.gooludou.shadowplanner.feature.projects.ProjectListScreen
import com.gooludou.shadowplanner.feature.projects.ProjectListViewModel
import com.gooludou.shadowplanner.feature.settings.SettingsScreen

@Composable
fun AppNavigation(
    mapControllerFactory: MapboxShadowMapController.Factory,
    modifier: Modifier = Modifier
) {
    val backStack = remember { mutableStateListOf<Any>(AppDestination.Map) }
    var pendingLocation by remember {
        androidx.compose.runtime.mutableStateOf<LocationSearchResult?>(null)
    }
    var pendingProjectId by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    NavDisplay(
        modifier = modifier.fillMaxSize(),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                AppDestination.Map -> NavEntry(key) {
                    ShadowMapRoute(
                        mapControllerFactory = mapControllerFactory,
                        pendingLocation = pendingLocation,
                        pendingProjectId = pendingProjectId,
                        onLocationApplied = { pendingLocation = null },
                        onProjectApplied = { pendingProjectId = null },
                        onOpenLocationSearch = { backStack.add(AppDestination.LocationSearch) },
                        onOpenProjects = { backStack.add(AppDestination.Projects) },
                        onOpenSettings = { backStack.add(AppDestination.Settings) }
                    )
                }

                AppDestination.Settings -> NavEntry(key) {
                    SettingsScreen(onBack = { backStack.removeLastOrNull() })
                }

                AppDestination.LocationSearch -> NavEntry(key) {
                    val viewModel: LocationSearchViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsState()
                    LaunchedEffect(uiState.selectedLocation) {
                        uiState.selectedLocation?.let { selected ->
                            pendingLocation = selected
                            backStack.removeLastOrNull()
                        }
                    }
                    LocationSearchScreen(
                        uiState = uiState,
                        onQueryChanged = viewModel::onQueryChanged,
                        onResultSelected = viewModel::select,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                AppDestination.Projects -> NavEntry(key) {
                    val viewModel: ProjectListViewModel = hiltViewModel()
                    val projects by viewModel.projects.collectAsState()
                    ProjectListScreen(
                        projects = projects,
                        onProjectSelected = { id ->
                            pendingProjectId = id
                            backStack.removeLastOrNull()
                        },
                        onDeleteProject = viewModel::deleteProject,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                else -> error("Unknown navigation destination: $key")
            }
        }
    )
}
