package com.gooludou.shadowplanner.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@Composable
fun PremiumFeatureIndicator(
    showIndicator: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dimensions = ShadowMapDesign.dimensions
    Box(modifier = modifier) {
        content()
        if (showIndicator) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = dimensions.spacingXs, y = -dimensions.spacingXs)
                    .size(dimensions.spacingLarge),
                shape = CircleShape,
                color = PremiumIndicatorGold,
                contentColor = PremiumIndicatorContent,
                shadowElevation = dimensions.spacingXxs
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_crown_24),
                    contentDescription = null,
                    modifier = Modifier.padding(dimensions.spacingXxs)
                )
            }
        }
    }
}

private val PremiumIndicatorGold = Color(0xFFFFC400)
private val PremiumIndicatorContent = Color(0xFF2C1A04)

@Preview(name = "Premium feature indicator - light", showBackground = true)
@Composable
private fun PremiumFeatureIndicatorLightPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = false) {
        PremiumFeatureIndicatorPreviewContent()
    }
}

@Preview(name = "Premium feature indicator - dark", showBackground = true)
@Composable
private fun PremiumFeatureIndicatorDarkPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = true) {
        PremiumFeatureIndicatorPreviewContent()
    }
}

@Composable
private fun PremiumFeatureIndicatorPreviewContent() {
    Surface(color = MaterialTheme.colorScheme.background) {
        PremiumFeatureIndicator(showIndicator = true) {
            MapRoundIconButton(onClick = {}, contentDescription = "Save project") {
                Icon(Icons.Outlined.Save, contentDescription = null)
            }
        }
    }
}
