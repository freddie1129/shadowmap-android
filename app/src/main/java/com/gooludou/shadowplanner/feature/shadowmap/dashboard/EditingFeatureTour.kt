package com.gooludou.shadowplanner.feature.shadowmap.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme
import kotlin.math.roundToInt

internal enum class EditingTooltipTarget {
    SHADOW_COLOUR,
    BUILDING,
    WALL,
    TREE,
    AUTO_LOAD,
    CLEAR
}

internal data class FeatureTourStep(
    val target: String,
    val title: Int,
    val description: Int
)

private val editingTooltipSteps = listOf(
    FeatureTourStep(
        EditingTooltipTarget.SHADOW_COLOUR.name,
        R.string.editing_tooltip_shadow_colour_title,
        R.string.editing_tooltip_shadow_colour_description
    ),
    FeatureTourStep(
        EditingTooltipTarget.BUILDING.name,
        R.string.editing_tooltip_building_title,
        R.string.editing_tooltip_building_description
    ),
    FeatureTourStep(
        EditingTooltipTarget.WALL.name,
        R.string.editing_tooltip_wall_title,
        R.string.editing_tooltip_wall_description
    ),
    FeatureTourStep(
        EditingTooltipTarget.TREE.name,
        R.string.editing_tooltip_tree_title,
        R.string.editing_tooltip_tree_description
    ),
    FeatureTourStep(
        EditingTooltipTarget.AUTO_LOAD.name,
        R.string.editing_tooltip_auto_load_title,
        R.string.editing_tooltip_auto_load_description
    ),
    FeatureTourStep(
        EditingTooltipTarget.CLEAR.name,
        R.string.editing_tooltip_clear_title,
        R.string.editing_tooltip_clear_description
    )
)

@Composable
internal fun FeatureTourOverlay(
    targetBounds: Map<String, Rect>,
    steps: List<FeatureTourStep>,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = steps[stepIndex]
    val target = targetBounds[step.target] ?: return
    val density = LocalDensity.current
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()
    val cardWidthPx = with(density) { 304.dp.toPx() }
    val cardHeightPx = with(density) { 156.dp.toPx() }
    val horizontalMarginPx = with(density) { 16.dp.toPx() }
    val verticalMarginPx = with(density) { 24.dp.toPx() }
    val cardLeft = (target.center.x - cardWidthPx / 2)
        .coerceIn(horizontalMarginPx, screenWidthPx - cardWidthPx - horizontalMarginPx)
    val cardTop = if (target.top > cardHeightPx + verticalMarginPx) {
        target.top - cardHeightPx - verticalMarginPx
    } else {
        target.bottom + verticalMarginPx
    }
    val highlightColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { }
                }
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(Color.Black.copy(alpha = 0.64f))
            drawRoundRect(
                color = Color.Transparent,
                topLeft = target.topLeft,
                size = target.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                blendMode = BlendMode.Clear
            )
            drawRoundRect(
                color = highlightColor,
                topLeft = target.topLeft,
                size = target.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
        }
        Surface(
            modifier = Modifier
                .widthIn(max = 304.dp)
                .width(304.dp)
                .align(Alignment.TopStart)
                .offset { IntOffset(cardLeft.roundToInt(), cardTop.roundToInt()) },
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(step.title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(step.description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.editing_tooltip_progress,
                            stepIndex + 1,
                            steps.size
                        ),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (stepIndex == steps.lastIndex) {
                                onCompleted()
                            } else {
                                stepIndex += 1
                            }
                        }
                    ) {
                        Text(
                            stringResource(
                                if (stepIndex == steps.lastIndex) {
                                    R.string.editing_tooltip_done
                                } else {
                                    R.string.editing_tooltip_close
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditingFeatureTour(
    targetBounds: Map<EditingTooltipTarget, Rect>,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    FeatureTourOverlay(
        targetBounds = targetBounds.mapKeys { it.key.name },
        steps = editingTooltipSteps,
        onCompleted = onCompleted,
        modifier = modifier
    )
}

internal fun Modifier.reportEditingTooltipTarget(
    target: EditingTooltipTarget,
    onBoundsChanged: (EditingTooltipTarget, Rect) -> Unit
): Modifier = reportFeatureTourTarget(target.name) { _, bounds ->
    onBoundsChanged(target, bounds)
}

internal fun Modifier.reportFeatureTourTarget(
    target: String,
    onBoundsChanged: (String, Rect) -> Unit
): Modifier = onGloballyPositioned { coordinates: LayoutCoordinates ->
    val position = coordinates.positionInRoot()
    onBoundsChanged(
        target,
        Rect(position, Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()))
    )
}

@Preview(name = "Editing feature tour - light", widthDp = 400, heightDp = 800)
@Composable
private fun EditingFeatureTourLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        EditingFeatureTourPreviewContent()
    }
}

@Preview(name = "Editing feature tour - dark", widthDp = 400, heightDp = 800)
@Composable
private fun EditingFeatureTourDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        EditingFeatureTourPreviewContent()
    }
}

@Composable
private fun EditingFeatureTourPreviewContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EditingFeatureTour(
            targetBounds = mapOf(
                EditingTooltipTarget.SHADOW_COLOUR to Rect(24f, 570f, 72f, 618f)
            ),
            onCompleted = {}
        )
    }
}
