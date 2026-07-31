package com.gooludou.shadowplanner.feature.onboarding

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.RawRes
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme
import kotlinx.coroutines.launch

private val onboardingPages = listOf(
    OnboardingPage(
        R.string.onboarding_shade_title,
        R.string.onboarding_shade_description,
        artwork = OnboardingArtwork.Video(R.raw.onboarding_1)
    ),
    OnboardingPage(
        R.string.onboarding_time_title,
        R.string.onboarding_time_description,
        artwork = OnboardingArtwork.Video(R.raw.onboarding_2)
    ),
    OnboardingPage(
        R.string.onboarding_plan_title,
        R.string.onboarding_plan_description,
        artwork = OnboardingArtwork.Video(R.raw.onboarding_3)
    )
)

@Composable
fun OnboardingRoute(
    onFinish: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = onboardingPages::size)
    val coroutineScope = rememberCoroutineScope()

    fun moveTo(page: Int) {
        coroutineScope.launch { pagerState.animateScrollToPage(page) }
    }

    BackHandler {
        if (pagerState.currentPage > 0) moveTo(pagerState.currentPage - 1) else onClose()
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { pageIndex ->
        val page = onboardingPages[pageIndex]
        OnboardingScreen(
            page = page,
            pageIndex = pageIndex,
            pageCount = onboardingPages.size,
            onSkip = onFinish,
            onBack = { moveTo(pageIndex - 1) },
            onNext = {
                if (pageIndex == onboardingPages.lastIndex) onFinish()
                else moveTo(pageIndex + 1)
            },
            artwork = { OnboardingArtwork(page.artwork, pagerState.currentPage == pageIndex) }
        )
    }
}

@Composable
fun OnboardingScreen(
    page: OnboardingPage,
    pageIndex: Int,
    pageCount: Int,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: @Composable () -> Unit = { OnboardingArtworkPlaceholder() }
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val isWide = maxWidth >= 600.dp
            if (isWide) {
                WideOnboardingLayout(
                    page = page,
                    pageIndex = pageIndex,
                    pageCount = pageCount,
                    onSkip = onSkip,
                    onBack = onBack,
                    onNext = onNext,
                    artwork = artwork
                )
            } else {
                CompactOnboardingLayout(
                    page = page,
                    pageIndex = pageIndex,
                    pageCount = pageCount,
                    onSkip = onSkip,
                    onBack = onBack,
                    onNext = onNext,
                    artwork = artwork
                )
            }
        }
    }
}

@Composable
private fun OnboardingArtwork(
    artwork: OnboardingArtwork,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    when (artwork) {
        OnboardingArtwork.Image -> OnboardingArtworkPlaceholder(modifier)
        is OnboardingArtwork.Video -> OnboardingArtworkVideo(
            resourceId = artwork.resourceId,
            isActive = isActive,
            modifier = modifier
        )
    }
}

@Composable
private fun OnboardingArtworkVideo(
    @RawRes resourceId: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        OnboardingArtworkPlaceholder(modifier)
        return
    }

    val context = LocalContext.current
    val videoView = remember(context, resourceId) { VideoView(context) }
    val videoDescription = stringResource(R.string.onboarding_artwork_video_description)

    DisposableEffect(videoView) {
        onDispose { videoView.stopPlayback() }
    }

    LaunchedEffect(isActive) {
        if (isActive) videoView.start() else videoView.pause()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .aspectRatio(1f),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        AndroidView(
            factory = {
                videoView.apply {
                    val playerView = this
                    tag = isActive
                    setVideoURI(Uri.parse("android.resource://${context.packageName}/$resourceId"))
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true
                        mediaPlayer.setVolume(0f, 0f)
                        if (playerView.tag == true) playerView.start()
                    }
                }
            },
            update = { view ->
                view.tag = isActive
                if (isActive && !view.isPlaying) view.start()
                if (!isActive && view.isPlaying) view.pause()
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
                .semantics {
                    contentDescription = videoDescription
                }
        )
    }
}

@Composable
private fun CompactOnboardingLayout(
    page: OnboardingPage,
    pageIndex: Int,
    pageCount: Int,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    artwork: @Composable () -> Unit
) {
    val dimensions = ShadowMapDesign.dimensions
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensions.screenPadding)
    ) {
        SkipButton(onSkip, Modifier.align(Alignment.End))
        Box(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxWidth()
                .padding(vertical = dimensions.spacingLarge),
            contentAlignment = Alignment.Center
        ) {
            artwork()
        }
        OnboardingDetails(
            page = page,
            pageIndex = pageIndex,
            pageCount = pageCount,
            onBack = onBack,
            onNext = onNext
        )
        Spacer(modifier = Modifier.weight(0.15f))
        Spacer(modifier = Modifier.height(dimensions.spacingLarge))
    }
}

@Composable
private fun WideOnboardingLayout(
    page: OnboardingPage,
    pageIndex: Int,
    pageCount: Int,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    artwork: @Composable () -> Unit
) {
    val dimensions = ShadowMapDesign.dimensions
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensions.spacingHuge)
    ) {
        SkipButton(onSkip, Modifier.align(Alignment.End))
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = dimensions.spacingHuge),
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingHuge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                artwork()
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                OnboardingDetails(
                    page = page,
                    pageIndex = pageIndex,
                    pageCount = pageCount,
                    onBack = onBack,
                    onNext = onNext,
                    modifier = Modifier.widthIn(max = 480.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingDetails(
    page: OnboardingPage,
    pageIndex: Int,
    pageCount: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PageIndicator(pageIndex, pageCount)
        Spacer(modifier = Modifier.height(dimensions.spacingExtraLarge))
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensions.spacingMedium))
        Text(
            text = stringResource(page.description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensions.spacingExtraLarge))
        NavigationButtons(
            showBack = pageIndex > 0,
            isLastPage = pageIndex == pageCount - 1,
            onBack = onBack,
            onNext = onNext
        )
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(stringResource(R.string.skip))
    }
}

@Composable
private fun NavigationButtons(
    showBack: Boolean,
    isLastPage: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val dimensions = ShadowMapDesign.dimensions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        }
        Button(onClick = onNext, modifier = Modifier.weight(1f)) {
            Text(
                stringResource(
                    if (isLastPage) R.string.start_exploring else R.string.next
                )
            )
        }
    }
}

@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int, modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    val description = stringResource(
        R.string.onboarding_page_description,
        currentPage + 1,
        pageCount
    )
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
    ) {
        repeat(pageCount) { index ->
            Surface(
                modifier = Modifier.size(if (index == currentPage) 10.dp else 8.dp),
                shape = CircleShape,
                color = if (index == currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                content = {}
            )
        }
    }
}

@Composable
private fun OnboardingArtworkPlaceholder(modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .aspectRatio(4f / 3f),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(dimensions.spacingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                modifier = Modifier.size(dimensions.minimumTouchTarget),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(dimensions.spacingMedium))
            Text(
                text = stringResource(R.string.onboarding_artwork_placeholder),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val previewPage = OnboardingPage(
    R.string.onboarding_shade_title,
    R.string.onboarding_shade_description
)

@Preview(name = "Onboarding phone - light", showBackground = true)
@Composable
private fun OnboardingPhoneLightPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = false) {
        OnboardingScreen(previewPage, 0, 3, {}, {}, {})
    }
}

@Preview(name = "Onboarding phone - dark", showBackground = true)
@Composable
private fun OnboardingPhoneDarkPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = true) {
        OnboardingScreen(previewPage, 0, 3, {}, {}, {})
    }
}

@PreviewScreenSizes
@Composable
private fun OnboardingAdaptivePreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = false) {
        OnboardingScreen(previewPage, 1, 3, {}, {}, {})
    }
}
