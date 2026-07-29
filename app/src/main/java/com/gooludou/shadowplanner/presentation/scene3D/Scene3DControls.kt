package com.gooludou.shadowplanner.presentation.scene3D

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.scene.SceneCameraView
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@Composable
fun Scene3DControls(
    cameraView: SceneCameraView,
    showSatellite: Boolean,
    showSky: Boolean,
    onToggleCameraView: () -> Unit,
    onToggleSatellite: () -> Unit,
    onToggleSky: () -> Unit,
    onRefreshSky: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SceneControlButton(
            onClick = onToggleSatellite,
            contentDescription = stringResource(
                if (showSatellite) R.string.hide_satellite else R.string.show_satellite
            )
        ) {
            Icon(
                imageVector = if (showSatellite) {
                    Icons.Outlined.Visibility
                } else {
                    Icons.Outlined.VisibilityOff
                },
                contentDescription = null
            )
        }
        SceneControlButton(
            onClick = onToggleSky,
            contentDescription = stringResource(
                if (showSky) R.string.hide_sky_overview else R.string.show_sky_overview
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.WbSunny,
                contentDescription = null,
                tint = if (showSky) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        if (showSky) {
            SceneControlButton(
                onClick = onRefreshSky,
                contentDescription = stringResource(R.string.refresh_sky_dome)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null
                )
            }
        }
        SceneControlButton(
            onClick = onToggleCameraView,
            contentDescription = if (cameraView == SceneCameraView.TOP_DOWN) {
                stringResource(R.string.show_orbit_view)
            } else {
                stringResource(R.string.show_top_down_view)
            }
        ) {
            Icon(
                imageVector = if (cameraView == SceneCameraView.TOP_DOWN) {
                    Icons.Outlined.ViewInAr
                } else {
                    Icons.Outlined.Map
                },
                contentDescription = null
            )
        }
    }
}

@Composable
private fun SceneControlButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    val dimensions = ShadowMapDesign.dimensions
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 6.dp
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(dimensions.minimumTouchTarget)
                .semantics { this.contentDescription = contentDescription },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Box(
                modifier = Modifier.size(dimensions.iconSize),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Preview(name = "Orbit with satellite", showBackground = true, backgroundColor = 0xFF52654B)
@Composable
private fun Scene3DControlsPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        Scene3DControls(
            cameraView = SceneCameraView.ORBIT,
            showSatellite = true,
            showSky = true,
            onToggleCameraView = {},
            onToggleSatellite = {},
            onToggleSky = {},
            onRefreshSky = {}
        )
    }
}

@Preview(name = "Dark mode", showBackground = true, backgroundColor = 0xFF202124)
@Composable
private fun Scene3DControlsDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        Scene3DControls(
            cameraView = SceneCameraView.ORBIT,
            showSatellite = true,
            showSky = true,
            onToggleCameraView = {},
            onToggleSatellite = {},
            onToggleSky = {},
            onRefreshSky = {}
        )
    }
}
