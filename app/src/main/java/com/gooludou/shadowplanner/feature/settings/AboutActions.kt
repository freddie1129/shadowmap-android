package com.gooludou.shadowplanner.feature.settings

data class AboutActions(
    val onViewIntroductionClick: () -> Unit,
    val onWebsiteClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
    val onContactUsClick: () -> Unit,
    val onShareClick: () -> Unit,
    val onRateClick: () -> Unit
)
