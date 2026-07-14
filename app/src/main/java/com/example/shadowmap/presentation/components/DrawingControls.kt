package com.example.shadowmap.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    modifier: Modifier = Modifier
) {
    val canFinish = when (mode) {
        DrawMode.BUILDING -> vertexCount >= 3
        DrawMode.WALL -> vertexCount >= 2
        DrawMode.TREE -> false
    }
    val addText = when (mode) {
        DrawMode.BUILDING -> "Add corner"
        DrawMode.WALL -> "Add end"
        DrawMode.TREE -> "Add tree"
    }
    val hint = when (mode) {
        DrawMode.BUILDING -> "Move the map under the crosshair, then add each building corner."
        DrawMode.WALL -> "Move the map under the crosshair, then add each wall point."
        DrawMode.TREE -> "Move the map under the crosshair, then add the tree."
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = hint,
            color = Color.White,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF45484D),
                    contentColor = Color.White
                )
            ) {
                Text("Cancel")
            }
            if (vertexCount > 0 && mode != DrawMode.TREE) {
                Button(
                    onClick = onUndo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE1E3E6),
                        contentColor = Color(0xFF242629)
                    )
                ) {
                    Text("Undo")
                }
            }
            Button(onClick = onAdd) { Text(addText) }
            if (canFinish) {
                Button(onClick = onDone) {
                    Text("Finish")
                }
            }
        }
    }
}

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
            modifier = Modifier.padding(12.dp)
        )
    }
}
