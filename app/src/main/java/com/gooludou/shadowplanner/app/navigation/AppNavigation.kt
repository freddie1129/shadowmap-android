package com.gooludou.shadowplanner.app.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.gooludou.shadowplanner.feature.locationsearch.LocationSearchScreen
import com.gooludou.shadowplanner.feature.locationsearch.LocationSearchViewModel
import com.gooludou.shadowplanner.feature.onboarding.OnboardingRoute
import com.gooludou.shadowplanner.feature.onboarding.OnboardingViewModel
import com.gooludou.shadowplanner.feature.projects.ProjectListScreen
import com.gooludou.shadowplanner.feature.projects.ProjectListViewModel
import com.gooludou.shadowplanner.feature.settings.SettingsScreen
import com.gooludou.shadowplanner.feature.settings.DeveloperSettingsScreen
import com.gooludou.shadowplanner.BuildConfig
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.feature.settings.AboutActions
import com.gooludou.shadowplanner.feature.settings.AboutScreen
import com.gooludou.shadowplanner.feature.settings.FeedbackEmailHelper
import com.gooludou.shadowplanner.feature.shadowmap.ShadowMapRoute
import com.gooludou.shadowplanner.location.LocationSearchResult
import com.gooludou.shadowplanner.purchase.model.EntitlementState
import com.gooludou.shadowplanner.purchase.model.InAppPurchaseState
import com.gooludou.shadowplanner.purchase.ui.PaywallSheet
import com.gooludou.shadowplanner.purchase.ui.PurchaseViewModel
import com.gooludou.shadowplanner.renderer.mapbox.MapboxShadowMapController

@Composable
@Suppress("LongMethod")
fun AppNavigation(
    mapControllerFactory: MapboxShadowMapController.Factory,
    modifier: Modifier = Modifier
) {
    val purchaseViewModel: PurchaseViewModel = hiltViewModel()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val purchaseState by purchaseViewModel.purchaseState.collectAsStateWithLifecycle()
    val forcePremium by purchaseViewModel.forcePremium.collectAsStateWithLifecycle()
    val showPaywall by purchaseViewModel.showPaywall.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by onboardingViewModel.hasCompletedOnboarding
        .collectAsStateWithLifecycle()
    if (hasCompletedOnboarding == null) {
        Surface(modifier = modifier.fillMaxSize()) {}
        return
    }
    val backStack = remember {
        mutableStateListOf<Any>(
            if (hasCompletedOnboarding == true) AppDestination.Map
            else AppDestination.Onboarding()
        )
    }
    var pendingLocation by remember {
        androidx.compose.runtime.mutableStateOf<LocationSearchResult?>(null)
    }
    var pendingProjectId by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    RefreshPurchasesOnResume(purchaseViewModel::onAppResumed)

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
                                if (key.isReplay) backStack.removeLastOrNull()
                                else finishOnboarding()
                            }
                        )
                    }

                    AppDestination.Map -> NavEntry(key) {
                        ShadowMapRoute(
                            mapControllerFactory = mapControllerFactory,
                            entitlementState = purchaseViewModel.effectiveEntitlement(
                                purchaseState.entitlement,
                                forcePremium
                            ),
                            onPremiumRequired = purchaseViewModel::requestPaywall,
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
                            isPremium = purchaseViewModel.effectiveEntitlement(
                                purchaseState.entitlement,
                                forcePremium
                            ) == EntitlementState.Premium,
                            onPremiumClick = purchaseViewModel::requestPaywall,
                            onDeveloperClick = {
                                backStack.add(AppDestination.DeveloperSettings)
                            },
                            onRateClick = context::rateApp,
                            onShareClick = context::shareApp,
                            onAboutClick = { backStack.add(AppDestination.About) },
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }

                    AppDestination.DeveloperSettings -> NavEntry(key) {
                        DeveloperSettingsScreen(
                            forcePremium = forcePremium,
                            onForcePremiumChange = purchaseViewModel::setForcePremium,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }

                    AppDestination.About -> NavEntry(key) {
                        val context = LocalContext.current
                        val isPremium = purchaseViewModel.effectiveEntitlement(
                            purchaseState.entitlement,
                            forcePremium
                        ) == EntitlementState.Premium
                        AboutScreen(
                            versionName = BuildConfig.VERSION_NAME,
                            actions = aboutActions(
                                context = context,
                                isPremium = isPremium,
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
        PurchasePaywallHost(showPaywall, purchaseState, purchaseViewModel)
    }
}

private fun aboutActions(
    context: Context,
    isPremium: Boolean,
    onViewIntroductionClick: () -> Unit
) = AboutActions(
    onViewIntroductionClick = onViewIntroductionClick,
    onWebsiteClick = { context.openUri(WEBSITE_URL) },
    onPrivacyPolicyClick = { context.openUri(PRIVACY_URL) },
    onContactUsClick = {
        context.startActivity(FeedbackEmailHelper.buildIntent(context, isPremium))
    },
    onShareClick = context::shareApp,
    onRateClick = context::rateApp
)

private fun Context.shareApp() {
    val text = getString(R.string.about_share_text, WEBSITE_URL)
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
    openUri("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
}

private fun Context.openUri(uri: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
}

private const val WEBSITE_URL = "https://sunfinderapps.com"
private const val PRIVACY_URL = "https://sunfinderapps.com/privacy.html"

@Composable
private fun RefreshPurchasesOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun PurchasePaywallHost(
    showPaywall: Boolean,
    purchaseState: InAppPurchaseState,
    viewModel: PurchaseViewModel
) {
    if (!showPaywall) return
    val activity = LocalContext.current.findActivity()
    PaywallSheet(
        state = purchaseState,
        onDismiss = viewModel::dismissPaywall,
        onPurchase = { optionId ->
            activity?.let { viewModel.purchase(it, optionId) }
        },
        onRestore = viewModel::restorePurchases,
        onRetry = viewModel::refresh
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
