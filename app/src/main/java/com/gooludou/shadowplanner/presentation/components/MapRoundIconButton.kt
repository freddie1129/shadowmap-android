package com.gooludou.shadowplanner.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun MapRoundIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
    shadowElevation: Dp? = null,
    border: BorderStroke? = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    ),
    icon: @Composable () -> Unit
) {
    val dimensions = ShadowMapDesign.dimensions
    val resolvedSize = size ?: dimensions.minimumTouchTarget
    Surface(
        modifier = modifier.size(resolvedSize),
        shape = CircleShape,
        color = containerColor,
        border = border,
        shadowElevation = shadowElevation ?: dimensions.floatingControlElevation
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .semantics { this.contentDescription = contentDescription },
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = contentColor,
                disabledContentColor = disabledContentColor
            )
        ) {
            icon()
        }
    }
}

@Preview(name = "Map round icon button", showBackground = true)
@Composable
private fun MapRoundIconButtonPreview() {
    ShadowMapTheme(dynamicColor = false) {
        MapRoundIconButton(
            onClick = {},
            contentDescription = stringResource(R.string.open_settings)
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = null)
        }
    }
}
