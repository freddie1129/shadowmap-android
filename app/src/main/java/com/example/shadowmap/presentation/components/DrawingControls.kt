package com.example.shadowmap.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shadowmap.domain.DrawMode
import com.example.shadowmap.ui.theme.ShadowMapTheme

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
        Button(onClick = onExpand, modifier = modifier) { Text("Add object") }
        return
    }
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = { onSelect(DrawMode.BUILDING) }) { Text("Building") }
        OutlinedButton(onClick = { onSelect(DrawMode.WALL) }) { Text("Wall") }
        OutlinedButton(onClick = { onSelect(DrawMode.TREE) }) { Text("Tree") }
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
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Cancel drawing")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        if (canUndo != null) {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo last point")
            }
        }
    }
}

@Composable
private fun DrawingPanelActions(
    config: DrawingPanelConfig,
    onAdd: () -> Unit,
    onDone: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onAdd, modifier = Modifier.weight(1f)) {
            Text(config.addText)
        }
        if (config.canFinish != null) {
            OutlinedButton(
                onClick = onDone,
                enabled = config.canFinish,
                modifier = Modifier.weight(1f)
            ) {
                Text("Finish")
            }
        }
    }
}

private fun DrawMode.toDrawingPanelConfig(vertexCount: Int): DrawingPanelConfig = when (this) {
    DrawMode.BUILDING -> DrawingPanelConfig(
        title = "Draw building",
        hint = "Move the map under the crosshair, then add each building corner.",
        addText = "Add corner",
        progress = "$vertexCount ${if (vertexCount == 1) "corner" else "corners"}",
        canUndo = vertexCount > 0,
        canFinish = vertexCount >= 3
    )
    DrawMode.WALL -> DrawingPanelConfig(
        title = "Draw wall",
        hint = "Move the map under the crosshair, then add each wall point.",
        addText = "Add point",
        progress = "$vertexCount ${if (vertexCount == 1) "point" else "points"}",
        canUndo = vertexCount > 0,
        canFinish = vertexCount >= 2
    )
    DrawMode.TREE -> DrawingPanelConfig(
        title = "Place tree",
        hint = "Move the map under the crosshair, then add the tree.",
        addText = "Place tree",
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
