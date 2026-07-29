package com.gooludou.shadowplanner.feature.shadowmap.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.Config
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.model.DrawMode
import com.gooludou.shadowplanner.core.model.ShadowAppearance
import com.gooludou.shadowplanner.core.ui.components.MapRoundIconButton
import com.gooludou.shadowplanner.core.ui.theme.Map3DActionBlue
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@Composable
fun MapToolBar(
    autoState: AutoToolState,
    hasSceneObjects: Boolean,
    isTimeVisible: Boolean,
    shadowAppearance: ShadowAppearance,
    onDrawMode: (DrawMode) -> Unit,
    onAutoLoad: () -> Unit,
    onClear: () -> Unit,
    onToggleTime: () -> Unit,
    onOpenShadowColor: () -> Unit,
    onOpenMapbox3D: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showTimeToggle: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShadowColorButton(shadowAppearance, onOpenShadowColor)
        PrimaryToolBar(autoState, hasSceneObjects, onDrawMode, onAutoLoad, onClear)
        if (showTimeToggle) {
            TimeToolButton(isTimeVisible, onToggleTime)
        }
        if (onOpenMapbox3D != null) {
            Spacer(modifier = Modifier.weight(1f))
            Open3DToolButton(onOpenMapbox3D)
        }
    }
}

@Composable
fun ShadowColorButton(appearance: ShadowAppearance, onClick: () -> Unit) {
    MapRoundIconButton(
        onClick = onClick,
        contentDescription = stringResource(R.string.change_shadow_colour)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color(appearance.colorArgb), CircleShape)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun PrimaryToolBar(
    autoState: AutoToolState,
    hasSceneObjects: Boolean,
    onDrawMode: (DrawMode) -> Unit,
    onAutoLoad: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        shadowElevation = 6.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIconButton({
                onDrawMode(DrawMode.BUILDING)
            }, stringResource(R.string.outline_a_building)) {
                Icon(
                    painter = painterResource(R.drawable.square_polygon),
                    contentDescription = null
                )
            }
            ToolIconButton({ onDrawMode(DrawMode.WALL) }, stringResource(R.string.draw_wall)) {
                Icon(Icons.Outlined.Timeline, contentDescription = null)
            }
            ToolIconButton({ onDrawMode(DrawMode.TREE) }, stringResource(R.string.place_tree)) {
                Icon(Icons.Outlined.Park, contentDescription = null)
            }
            if (Config.ALLOW_LOAD_BUILDING) {
                AutoToolButton(autoState, onAutoLoad)
            }
            ToolIconButton(
                onClear,
                stringResource(R.string.clear_scene),
                enabled = hasSceneObjects
            ) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
            }
        }
    }
}

@Composable
private fun AutoToolButton(autoState: AutoToolState, onClick: () -> Unit) {
    val description = when (autoState) {
        AutoToolState.TOO_LARGE -> stringResource(R.string.zoom_in_load_buildings)
        AutoToolState.LOADING -> stringResource(R.string.loading_buildings)
        else -> stringResource(R.string.load_buildings_automatically)
    }
    val tint = when (autoState) {
        AutoToolState.TOO_LARGE, AutoToolState.CHECKING ->
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

        AutoToolState.LOADED -> MaterialTheme.colorScheme.primary

        AutoToolState.ERROR -> MaterialTheme.colorScheme.error

        else -> MaterialTheme.colorScheme.onSurface
    }
    ToolIconButton(
        onClick = onClick,
        contentDescription = description,
        enabled = autoState != AutoToolState.LOADING,
        tint = tint
    ) {
        if (autoState == AutoToolState.LOADING) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
        } else {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
        }
    }
}

@Composable
private fun TimeToolButton(isTimeVisible: Boolean, onClick: () -> Unit) {
    MapRoundIconButton(
        onClick = onClick,
        contentDescription = stringResource(
            if (isTimeVisible) R.string.hide_date_time else R.string.show_date_time
        ),
        size = 48.dp
    ) {
        Icon(
            imageVector = if (isTimeVisible) {
                Icons.Outlined.ExpandMore
            } else {
                Icons.Outlined.ExpandLess
            },
            contentDescription = null
        )
    }
}

@Composable
private fun Open3DToolButton(onClick: () -> Unit) {
    MapRoundIconButton(
        onClick = onClick,
        contentDescription = stringResource(R.string.open_3d_button),
        size = 48.dp,
        containerColor = Map3DActionBlue,
        contentColor = Color.White,
        border = null
    ) {
        Icon(Icons.Outlined.ViewInAr, contentDescription = null)
    }
}

@Composable
private fun ToolIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription },
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
        )
    ) {
        content()
    }
}

@Preview(name = "Map toolbar", showBackground = true, backgroundColor = 0xFF52654B)
@Composable
private fun MapToolBarLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        MapToolBarPreview()
    }
}

@Preview(name = "Map toolbar (dark)", showBackground = true, backgroundColor = 0xFF263238)
@Composable
private fun MapToolBarDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        MapToolBarPreview()
    }
}

@Composable
private fun MapToolBarPreview() {
    MapToolBar(
        autoState = AutoToolState.READY,
        hasSceneObjects = true,
        isTimeVisible = false,
        shadowAppearance = ShadowAppearance.DEFAULT,
        onDrawMode = {},
        onAutoLoad = {},
        onClear = {},
        onToggleTime = {},
        onOpenShadowColor = {},
        onOpenMapbox3D = {}
    )
}
