package com.gooludou.shadowplanner.app.navigation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.gooludou.shadowplanner.BuildConfig
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.feature.locationsearch.LocationSearchScreen
import com.gooludou.shadowplanner.feature.locationsearch.LocationSearchViewModel
import com.gooludou.shadowplanner.feature.onboarding.OnboardingRoute
import com.gooludou.shadowplanner.feature.onboarding.OnboardingViewModel
import com.gooludou.shadowplanner.feature.projects.ProjectListScreen
import com.gooludou.shadowplanner.feature.projects.ProjectListViewModel
import com.gooludou.shadowplanner.feature.settings.AboutActions
import com.gooludou.shadowplanner.feature.settings.AboutScreen
import com.gooludou.shadowplanner.feature.settings.FeedbackEmailHelper
import com.gooludou.shadowplanner.feature.settings.SettingsScreen
import com.gooludou.shadowplanner.feature.shadowmap.ShadowMapRoute
import com.gooludou.shadowplanner.location.LocationSearchResult
import com.gooludou.shadowplanner.renderer.mapbox.MapboxShadowMapController

@Composable
@Suppress("LongMethod")
fun AppNavigation(
    mapControllerFactory: MapboxShadowMapController.Factory,
    modifier: Modifier = Modifier
) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val hasCompletedOnboarding by onboardingViewModel.hasCompletedOnboarding
        .collectAsStateWithLifecycle()
    if (hasCompletedOnboarding == null) {
        Surface(modifier = modifier.fillMaxSize()) {}
        return
    }
    val backStack = remember {
        mutableStateListOf<Any>(
            if (hasCompletedOnboarding == true) {
                AppDestination.Map
            } else {
                AppDestination.Onboarding()
            }
        )
    }
    var pendingLocation by remember {
        androidx.compose.runtime.mutableStateOf<LocationSearchResult?>(null)
    }
    var pendingProjectId by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    Box(modifier = modifier.fillMaxSize()) {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    is AppDestination.Onboarding -> NavEntry(key) {
                        val finishOnboarding: () -> Unit = {
                            onboardingViewModel.markCompleted()
                            if (key.isReplay) {
                                backStack.removeLastOrNull()
                            } else {
                                backStack.clear()
                                backStack.add(AppDestination.Map)
                            }
                            Unit
                        }
                        OnboardingRoute(
                            onFinish = finishOnboarding,
                            onClose = {
                                if (key.isReplay) {
                                    backStack.removeLastOrNull()
                                } else {
                                    finishOnboarding()
                                }
                            }
                        )
                    }

                    AppDestination.Map -> NavEntry(key) {
                        ShadowMapRoute(
                            mapControllerFactory = mapControllerFactory,
                            pendingLocation = pendingLocation,
                            pendingProjectId = pendingProjectId,
                            onLocationApplied = { pendingLocation = null },
                            onProjectApplied = { pendingProjectId = null },
                            onOpenLocationSearch = {
                                backStack.add(AppDestination.LocationSearch)
                            },
                            onOpenProjects = { backStack.add(AppDestination.Projects) },
                            onOpenSettings = { backStack.add(AppDestination.Settings) }
                        )
                    }

                    AppDestination.Settings -> NavEntry(key) {
                        val context = LocalContext.current
                        SettingsScreen(
                            onRateClick = context::rateApp,
                            onShareClick = context::shareApp,
                            onLanguageClick = context::openLanguageSettings,
                            onAboutClick = { backStack.add(AppDestination.About) },
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }

                    AppDestination.About -> NavEntry(key) {
                        val context = LocalContext.current
                        AboutScreen(
                            versionName = BuildConfig.VERSION_NAME,
                            actions = aboutActions(
                                context = context,
                                onViewIntroductionClick = {
                                    backStack.add(AppDestination.Onboarding(isReplay = true))
                                }
                            ),
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }

                    AppDestination.LocationSearch -> NavEntry(key) {
                        val viewModel: LocationSearchViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsState()
                        LaunchedEffect(uiState.selectedLocation) {
                            uiState.selectedLocation?.let { selected ->
                                pendingLocation = selected
                                viewModel.clear()
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
}

private fun Context.openLanguageSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = "package:$packageName".toUri()
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
        }
    }
    startActivity(intent)
}

private fun aboutActions(
    context: Context,
    onViewIntroductionClick: () -> Unit
) = AboutActions(
    onViewIntroductionClick = onViewIntroductionClick,
    onWebsiteClick = { context.openUri(WEBSITE_URL) },
    onPrivacyPolicyClick = { context.openUri(PRIVACY_URL) },
    onContactUsClick = {
        context.startActivity(FeedbackEmailHelper.buildIntent(context))
    },
    onShareClick = context::shareApp,
    onRateClick = context::rateApp
)

private fun Context.shareApp() {
    val text = getString(R.string.about_share_text, PLAY_STORE_URL)
    startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            null
        )
    )
}

private fun Context.rateApp() {
    openUri(PLAY_STORE_URL)
}

private fun Context.openUri(uri: String) {
    startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
}

private const val WEBSITE_URL = "https://sunfinderapps.com"
private const val PRIVACY_URL = "https://sunfinderapps.com/privacy.html"
private const val PLAY_STORE_URL =
    "https://play.google.com/store/apps/details?id=com.gooludou.shadowplanner"
