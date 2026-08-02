package com.gooludou.shadowplanner.feature.shadowmap

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@Composable
internal fun MoveModeOverlay(
    onDrag: (Offset, Offset) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val dimensions = ShadowMapDesign.dimensions
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragStart = it },
                    onDragCancel = { dragStart = null },
                    onDragEnd = { dragStart = null },
                    onDrag = { change, _ ->
                        val start = dragStart ?: change.position
                        onDrag(start, change.position)
                    }
                )
            }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = dimensions.spacingLarge,
                    end = dimensions.spacingLarge,
                    bottom = 96.dp
                ),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.move_object_hint),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = dimensions.spacingMedium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = onDone) { Text(stringResource(R.string.done)) }
            }
        }
    }
}

@Preview(name = "Move mode light", showBackground = true, widthDp = 360, heightDp = 180)
@Composable
private fun MoveModeOverlayLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF71856B))) {
            MoveModeOverlay(onDrag = { _, _ -> }, onDone = {}, onCancel = {})
        }
    }
}

@Preview(name = "Move mode dark", showBackground = true, widthDp = 360, heightDp = 180)
@Composable
private fun MoveModeOverlayDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF263326))) {
            MoveModeOverlay(onDrag = { _, _ -> }, onDone = {}, onCancel = {})
        }
    }
}
