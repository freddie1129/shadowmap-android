package com.gooludou.shadowplanner.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.ui.theme.Map3DActionBlue
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun SceneViewSwitchButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) {
                Map3DActionBlue
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            },
            contentColor = if (emphasized) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        border = if (emphasized) {
            null
        } else {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            )
        }
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(ShadowMapDesign.dimensions.spacingSmall))
        Text(label)
    }
}

@Preview(name = "Scene view switch", showBackground = true, backgroundColor = 0xFF6B8064)
@Composable
private fun SceneViewSwitchButtonLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        SceneViewSwitchButtonPreview()
    }
}

@Preview(name = "Scene view switch (dark)", showBackground = true, backgroundColor = 0xFF263238)
@Composable
private fun SceneViewSwitchButtonDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        SceneViewSwitchButtonPreview()
    }
}

@Composable
private fun SceneViewSwitchButtonPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6B8064)),
        contentAlignment = Alignment.Center
    ) {
        SceneViewSwitchButton(
            label = "3D View",
            icon = Icons.Outlined.ViewInAr,
            onClick = {}
        )
    }
}
