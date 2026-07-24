package com.gooludou.shadowplanner.presentation.drawview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.domain.DrawMode
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun DrawingCrosshair(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(40.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val arm = 12.dp.toPx()
        val gap = 4.dp.toPx()
        val shadowWidth = 4.dp.toPx()
        val lineWidth = 2.dp.toPx()
        val shadow = Color.Black.copy(alpha = 0.65f)
        listOf(
            Offset(center.x - arm - gap, center.y) to Offset(center.x - gap, center.y),
            Offset(center.x + gap, center.y) to Offset(center.x + arm + gap, center.y),
            Offset(center.x, center.y - arm - gap) to Offset(center.x, center.y - gap),
            Offset(center.x, center.y + gap) to Offset(center.x, center.y + arm + gap)
        ).forEach { (start, end) ->
            drawLine(shadow, start, end, shadowWidth)
            drawLine(Color.White, start, end, lineWidth)
        }
        drawCircle(shadow, 4.dp.toPx(), center, style = Stroke(shadowWidth))
        drawCircle(Color.White, 4.dp.toPx(), center, style = Stroke(lineWidth))
    }
}

@Composable
fun DrawingToolChooser(
    expanded: Boolean,
    onExpand: () -> Unit,
    onSelect: (DrawMode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!expanded) {
        Button(onClick = onExpand, modifier = modifier) {
            Text(stringResource(R.string.add_object))
        }
        return
    }
    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                RoundedCornerShape(16.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = {
            onSelect(DrawMode.BUILDING)
        }) { Text(stringResource(R.string.building)) }
        OutlinedButton(onClick = {
            onSelect(DrawMode.WALL)
        }) { Text(stringResource(R.string.wall)) }
        OutlinedButton(onClick = {
            onSelect(DrawMode.TREE)
        }) { Text(stringResource(R.string.tree)) }
    }
}

@Composable
fun ActiveDrawingControls(
    mode: DrawMode,
    vertexCount: Int,
    onAdd: () -> Unit,
    onUndo: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    val config = mode.toDrawingPanelConfig(vertexCount)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DrawingPanelHeader(config.title, config.canUndo, onCancel, onUndo)
            Text(text = config.hint, style = MaterialTheme.typography.bodyMedium)
            if (config.progress != null) {
                Text(
                    text = config.progress,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            DrawingPanelActions(config, onAdd, onDone)
        }
    }
}

@Composable
private fun DrawingPanelHeader(
    title: String,
    canUndo: Boolean?,
    onCancel: () -> Unit,
    onUndo: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCancel) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.cancel_drawing)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        if (canUndo != null) {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Outlined.Undo,
                    contentDescription = stringResource(R.string.undo_last_point)
                )
            }
        }
    }
}

@Composable
private fun DrawingPanelActions(config: DrawingPanelConfig, onAdd: () -> Unit, onDone: () -> Unit) {
    if (config.canFinish == true) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAdd, modifier = Modifier.weight(1f)) {
                Text(config.addText)
            }
            Button(onClick = onDone, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.finish))
            }
        }
    } else {
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text(config.addText)
        }
    }
}

@Composable
private fun DrawMode.toDrawingPanelConfig(vertexCount: Int): DrawingPanelConfig = when (this) {
    DrawMode.BUILDING -> DrawingPanelConfig(
        title = stringResource(R.string.outline_building),
        hint = stringResource(R.string.building_drawing_hint),
        addText = stringResource(
            if (vertexCount == 0) R.string.add_first_corner else R.string.add_corner
        ),
        progress = stringResource(R.string.minimum_corners_progress, vertexCount),
        canUndo = vertexCount > 0,
        canFinish = vertexCount >= 3
    )

    DrawMode.WALL -> DrawingPanelConfig(
        title = stringResource(R.string.draw_wall),
        hint = stringResource(R.string.wall_drawing_hint),
        addText = stringResource(
            if (vertexCount == 0) R.string.add_first_point else R.string.add_point
        ),
        progress = stringResource(R.string.minimum_points_progress, vertexCount),
        canUndo = vertexCount > 0,
        canFinish = vertexCount >= 2
    )

    DrawMode.TREE -> DrawingPanelConfig(
        title = stringResource(R.string.place_tree),
        hint = stringResource(R.string.tree_drawing_hint),
        addText = stringResource(R.string.place_tree),
        progress = null,
        canUndo = null,
        canFinish = null
    )
}

private data class DrawingPanelConfig(
    val title: String,
    val hint: String,
    val addText: String,
    val progress: String?,
    val canUndo: Boolean?,
    val canFinish: Boolean?
)

@Preview(
    name = "Drawing crosshair",
    widthDp = 80,
    heightDp = 80,
    showBackground = true,
    backgroundColor = 0xFF52654B
)
@Composable
private fun DrawingCrosshairPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            DrawingCrosshair()
        }
    }
}

@Preview(
    name = "Drawing tool chooser",
    widthDp = 380,
    showBackground = true,
    backgroundColor = 0xFF52654B
)
@Composable
private fun DrawingToolChooserPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        DrawingToolChooser(
            expanded = true,
            onExpand = {},
            onSelect = {},
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Preview(
    name = "Active building controls",
    widthDp = 540,
    showBackground = true,
    backgroundColor = 0xFF52654B
)
@Composable
private fun ActiveDrawingControlsPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        ActiveDrawingControls(
            mode = DrawMode.BUILDING,
            vertexCount = 3,
            onAdd = {},
            onUndo = {},
            onDone = {},
            onCancel = {},
            error = null,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Preview(
    name = "Active building controls \u00b7 dark",
    widthDp = 540,
    showBackground = true,
    backgroundColor = 0xFF263238
)
@Composable
private fun ActiveDrawingControlsDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        ActiveDrawingControls(
            mode = DrawMode.BUILDING,
            vertexCount = 0,
            onAdd = {},
            onUndo = {},
            onDone = {},
            onCancel = {},
            error = null,
            modifier = Modifier.padding(12.dp)
        )
    }
}
