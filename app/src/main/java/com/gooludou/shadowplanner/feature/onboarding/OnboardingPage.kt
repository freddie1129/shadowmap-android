package com.gooludou.shadowplanner.feature.onboarding

import androidx.annotation.RawRes
import androidx.annotation.StringRes

sealed interface OnboardingArtwork {
    data object Image : OnboardingArtwork

    data class Video(@param:RawRes val resourceId: Int) : OnboardingArtwork
}

data class OnboardingPage(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    val artwork: OnboardingArtwork = OnboardingArtwork.Image
)
