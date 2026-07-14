package com.example.shadowmap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timeline
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shadowmap.domain.DrawMode
import com.example.shadowmap.ui.theme.ShadowMapTheme

@Composable
fun MapToolBar(
    autoState: AutoToolState,
    hasSceneObjects: Boolean,
    isTimeVisible: Boolean,
    onDrawMode: (DrawMode) -> Unit,
    onAutoLoad: () -> Unit,
    onClear: () -> Unit,
    onToggleTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrimaryToolBar(autoState, hasSceneObjects, onDrawMode, onAutoLoad, onClear)
        TimeToolButton(isTimeVisible, onToggleTime)
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
        shadowElevation = 6.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIconButton({ onDrawMode(DrawMode.BUILDING) }, "Draw building") {
                Icon(Icons.Outlined.Business, contentDescription = null)
            }
            ToolIconButton({ onDrawMode(DrawMode.WALL) }, "Draw wall") {
                Icon(Icons.Outlined.Timeline, contentDescription = null)
            }
            ToolIconButton({ onDrawMode(DrawMode.TREE) }, "Place tree") {
                Icon(Icons.Outlined.Park, contentDescription = null)
            }
            AutoToolButton(autoState, onAutoLoad)
            ToolIconButton(onClear, "Clear scene", enabled = hasSceneObjects) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
            }
        }
    }
}

@Composable
private fun AutoToolButton(autoState: AutoToolState, onClick: () -> Unit) {
    val description = when (autoState) {
        AutoToolState.TOO_LARGE -> "Zoom in to load buildings"
        AutoToolState.LOADING -> "Loading buildings"
        else -> "Load buildings automatically"
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
    Surface(
        shape = CircleShape,
        color = if (isTimeVisible) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        },
        shadowElevation = 6.dp
    ) {
        ToolIconButton(
            onClick = onClick,
            contentDescription = if (isTimeVisible) "Hide date and time" else "Show date and time",
            tint = if (isTimeVisible) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null)
        }
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
private fun MapToolBarPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        MapToolBar(
            autoState = AutoToolState.READY,
            hasSceneObjects = true,
            isTimeVisible = false,
            onDrawMode = {},
            onAutoLoad = {},
            onClear = {},
            onToggleTime = {}
        )
    }
}
