package com.gooludou.shadowplanner.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PitchSlider(pitch: Float, onPitchChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    val sliderColors = SliderDefaults.colors(
        activeTrackColor = Color.White.copy(alpha = 0.92f),
        inactiveTrackColor = Color.White.copy(alpha = 0.48f)
    )
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .width(dimensions.minimumTouchTarget)
            .height(PITCH_SLIDER_LENGTH),
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = pitch.coerceIn(MIN_PITCH_DEGREES, MAX_PITCH_DEGREES),
            onValueChange = onPitchChange,
            valueRange = MIN_PITCH_DEGREES..MAX_PITCH_DEGREES,
            colors = sliderColors,
            interactionSource = interactionSource,
            modifier = Modifier
                .requiredWidth(PITCH_SLIDER_LENGTH)
                .rotate(-90f),
            thumb = {
                Surface(
                    modifier = Modifier.size(PITCH_THUMB_SIZE),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.95f),
                    border = BorderStroke(2.dp, Color.Black.copy(alpha = 0.55f)),
                    shadowElevation = 2.dp
                ) {}
            },
            track = { sliderState ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PITCH_TRACK_HALO_WIDTH)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(PITCH_TRACK_WIDTH),
                        colors = sliderColors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 2.dp
                    )
                }
            }
        )
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

private val PITCH_SLIDER_LENGTH = 144.dp
private val PITCH_THUMB_SIZE = 18.dp
private val PITCH_TRACK_HALO_WIDTH = 8.dp
private val PITCH_TRACK_WIDTH = 4.dp
private const val MIN_PITCH_DEGREES = 0f
private const val MAX_PITCH_DEGREES = 60f
