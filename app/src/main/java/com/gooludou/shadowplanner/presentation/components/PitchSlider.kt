package com.gooludou.shadowplanner.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme
import kotlin.math.roundToInt

@Composable
fun PitchSlider(
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = CONTROL_SURFACE_ALPHA),
        tonalElevation = dimensions.floatingControlElevation
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = dimensions.spacingSmall,
                vertical = dimensions.spacingMedium
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.pitch_degrees, pitch.roundToInt()),
                style = MaterialTheme.typography.labelMedium
            )
            Box(
                modifier = Modifier
                    .width(dimensions.minimumTouchTarget)
                    .height(PITCH_SLIDER_LENGTH),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = pitch.coerceIn(MIN_PITCH_DEGREES, MAX_PITCH_DEGREES),
                    onValueChange = onPitchChange,
                    valueRange = MIN_PITCH_DEGREES..MAX_PITCH_DEGREES,
                    modifier = Modifier
                        .width(PITCH_SLIDER_LENGTH)
                        .rotate(-90f)
                )
            }
        }
    }
}

@Preview(name = "Pitch slider - light", showBackground = true)
@Composable
private fun PitchSliderLightPreview() {
    PitchSliderPreviewContent(darkTheme = false)
}

@Preview(name = "Pitch slider - dark", showBackground = true)
@Composable
private fun PitchSliderDarkPreview() {
    PitchSliderPreviewContent(darkTheme = true)
}

@Composable
private fun PitchSliderPreviewContent(darkTheme: Boolean) {
    ShadowMapTheme(darkTheme = darkTheme, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            PitchSlider(pitch = 45f, onPitchChange = {})
        }
    }
}

private val PITCH_SLIDER_LENGTH = 180.dp
private const val MIN_PITCH_DEGREES = 0f
private const val MAX_PITCH_DEGREES = 60f
private const val CONTROL_SURFACE_ALPHA = 0.9f
