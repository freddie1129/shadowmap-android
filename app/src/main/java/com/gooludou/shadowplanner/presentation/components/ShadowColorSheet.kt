package com.gooludou.shadowplanner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.domain.ShadowAppearance
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme
import kotlin.math.roundToInt

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun ShadowColorSheet(
    initialAppearance: ShadowAppearance,
    onDismissRequest: () -> Unit,
    onApply: (ShadowAppearance) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        ShadowColorSheetContent(initialAppearance, onDismissRequest, onApply)
    }
}

@Composable
private fun ShadowColorSheetContent(
    initialAppearance: ShadowAppearance,
    onDismissRequest: () -> Unit,
    onApply: (ShadowAppearance) -> Unit
) {
    var selectedColor by remember(initialAppearance) {
        mutableLongStateOf(initialAppearance.colorArgb)
    }
    var opacity by remember(initialAppearance) {
        mutableFloatStateOf(initialAppearance.opacity)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.shadow_colour),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShadowAppearance.PRESET_COLORS.forEach { color ->
                val isSelected = selectedColor == color
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) {
                                Color.White
                            } else {
                                Color.White.copy(
                                    alpha = 0.45f
                                )
                            },
                            shape = CircleShape
                        )
                        .clickable { selectedColor = color }
                ) {
                    if (isSelected) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.selected),
                            tint = Color.White
                        )
                    }
                }
            }
        }
        Column {
            Text(stringResource(R.string.opacity, (opacity * 100).roundToInt()))
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0.1f..1.0f
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onDismissRequest, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = { onApply(ShadowAppearance(selectedColor, opacity)) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.apply))
            }
        }
        Spacer(Modifier.size(4.dp))
    }
}

@Preview(name = "Shadow colour sheet · light", showBackground = true)
@Composable
private fun ShadowColorSheetLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ShadowColorSheetContent(ShadowAppearance.DEFAULT, {}, {})
        }
    }
}

@Preview(name = "Shadow colour sheet · dark", showBackground = true)
@Composable
private fun ShadowColorSheetDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ShadowColorSheetContent(ShadowAppearance.DEFAULT, {}, {})
        }
    }
}
