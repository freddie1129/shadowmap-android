package com.gooludou.shadowplanner.presentation.drawview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.domain.DrawnObjectType
import com.gooludou.shadowplanner.domain.SceneObjectSource
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme
import java.util.Locale
import kotlin.math.abs

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod")
fun DrawingPropertiesSheet(
    type: DrawnObjectType,
    initialHeightMeters: Double,
    initialRadiusMeters: Double?,
    isCreating: Boolean,
    onBack: () -> Unit,
    onApply: (heightMeters: Double, radiusMeters: Double?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: () -> Unit = {},
    objectSource: SceneObjectSource? = null,
    isEditedAutomaticObject: Boolean = false,
    loadedHeightMeters: Double? = null
) {
    val config = type.propertyPanelConfig()
    var heightText by remember(type, initialHeightMeters) {
        mutableStateOf(initialHeightMeters.toEditableText())
    }
    var crownWidthText by remember(type, initialRadiusMeters) {
        mutableStateOf(((initialRadiusMeters ?: 2.5) * 2.0).toEditableText())
    }
    val height = heightText.toDoubleOrNull()
    val crownWidth = crownWidthText.toDoubleOrNull()
    val isHeightValid = height != null && height in config.heightRange
    val isCrownWidthValid = type != DrawnObjectType.TREE ||
        (crownWidth != null && crownWidth in MIN_CROWN_WIDTH_METERS..MAX_CROWN_WIDTH_METERS)

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
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PropertyPanelHeader(
                title = config.title,
                sourceLabel = propertySourceLabel(
                    isCreating,
                    objectSource,
                    isEditedAutomaticObject
                ),
                onBack = onBack
            )
            MeasurementEditor(
                label = stringResource(R.string.height),
                value = heightText,
                onValueChange = { heightText = it.asDecimalInput() },
                onStep = { delta ->
                    heightText = ((height ?: initialHeightMeters) + delta)
                        .coerceIn(config.heightRange.start, config.heightRange.endInclusive)
                        .toEditableText()
                },
                isError = heightText.isNotEmpty() && !isHeightValid,
                supportingText = if (heightText.isNotEmpty() && !isHeightValid) {
                    stringResource(
                        R.string.enter_range_meters,
                        config.heightRange.start.toEditableText(),
                        config.heightRange.endInclusive.toEditableText()
                    )
                } else {
                    null
                }
            )

            if (type == DrawnObjectType.BUILDING) {
                BuildingHeightControls(
                    objectSource = objectSource,
                    loadedHeightMeters = loadedHeightMeters,
                    currentHeightMeters = height,
                    onSelected = { heightText = it.toEditableText() }
                )
            }

            if (type == DrawnObjectType.TREE) {
                TreeCrownEditor(crownWidthText, crownWidth, isCrownWidthValid) {
                    crownWidthText = it
                }
            }

            PropertyPanelActions(
                isCreating = isCreating,
                saveLabel = if (isCreating) {
                    config.saveLabel
                } else {
                    stringResource(
                        R.string.save_changes
                    )
                },
                isSaveEnabled = isHeightValid && isCrownWidthValid,
                onDelete = onDelete,
                onMove = onMove,
                onSave = {
                    onApply(
                        requireNotNull(height),
                        if (type == DrawnObjectType.TREE) requireNotNull(crownWidth) / 2.0 else null
                    )
                }
            )
        }
    }
}

@Composable
private fun propertySourceLabel(
    isCreating: Boolean,
    objectSource: SceneObjectSource?,
    isEditedAutomaticObject: Boolean
): String = when {
    isCreating -> stringResource(R.string.new_manual_object)

    objectSource == SceneObjectSource.AUTOMATIC && isEditedAutomaticObject ->
        stringResource(R.string.automatically_loaded_edited)

    objectSource == SceneObjectSource.AUTOMATIC -> stringResource(R.string.automatically_loaded)

    else -> stringResource(R.string.manually_drawn)
}

@Composable
private fun BuildingHeightControls(
    objectSource: SceneObjectSource?,
    loadedHeightMeters: Double?,
    currentHeightMeters: Double?,
    onSelected: (Double) -> Unit
) {
    Column {
        if (objectSource == SceneObjectSource.AUTOMATIC && loadedHeightMeters != null) {
            TextButton(
                onClick = { onSelected(loadedHeightMeters) },
                enabled = currentHeightMeters == null ||
                    abs(currentHeightMeters - loadedHeightMeters) >=
                    HEIGHT_EQUALITY_TOLERANCE_METERS
            ) {
                Text(
                    stringResource(
                        R.string.reset_loaded_height,
                        loadedHeightMeters.toEditableText()
                    )
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3.0, 6.0, 10.0).forEach { preset ->
                OutlinedButton(onClick = { onSelected(preset) }) {
                    Text(stringResource(R.string.preset_meters, preset.toInt()))
                }
            }
        }
    }
}

@Composable
private fun PropertyPanelHeader(title: String, sourceLabel: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back)
            )
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = sourceLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TreeCrownEditor(
    crownWidthText: String,
    crownWidth: Double?,
    isValid: Boolean,
    onValueChange: (String) -> Unit
) {
    MeasurementEditor(
        label = stringResource(R.string.crown_width),
        value = crownWidthText,
        onValueChange = { onValueChange(it.asDecimalInput()) },
        onStep = { delta ->
            onValueChange(
                ((crownWidth ?: 5.0) + delta)
                    .coerceIn(MIN_CROWN_WIDTH_METERS, MAX_CROWN_WIDTH_METERS)
                    .toEditableText()
            )
        },
        isError = crownWidthText.isNotEmpty() && !isValid,
        supportingText = if (crownWidthText.isNotEmpty() && !isValid) {
            stringResource(R.string.enter_tree_crown_width)
        } else {
            null
        }
    )
}

@Composable
private fun PropertyPanelActions(
    isCreating: Boolean,
    saveLabel: String,
    isSaveEnabled: Boolean,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onSave: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isCreating) {
            OutlinedButton(onClick = onMove, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.move_object))
            }
        }
        if (!isCreating) {
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        }
        Button(
            onClick = onSave,
            enabled = isSaveEnabled,
            modifier = Modifier.weight(1f)
        ) {
            Text(saveLabel)
        }
    }
}

@Composable
private fun MeasurementEditor(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onStep: (Double) -> Unit,
    isError: Boolean,
    supportingText: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = { onStep(-MEASUREMENT_STEP_METERS) }) {
            Text(stringResource(R.string.decrease))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            suffix = { Text(stringResource(R.string.meters)) },
            singleLine = true,
            isError = isError,
            supportingText = if (supportingText == null) {
                null
            } else {
                { Text(supportingText) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = { onStep(MEASUREMENT_STEP_METERS) }) {
            Text(stringResource(R.string.increase))
        }
    }
}

private fun String.asDecimalInput(): String = buildString {
    var hasDecimalPoint = false
    this@asDecimalInput.forEach { character ->
        when {
            character.isDigit() -> append(character)

            character == '.' && !hasDecimalPoint && isNotEmpty() -> {
                append(character)
                hasDecimalPoint = true
            }
        }
    }
}

private fun Double.toEditableText(): String = String.format(Locale.US, "%.1f", this)

@Composable
private fun DrawnObjectType.propertyPanelConfig(): PropertyPanelConfig = when (this) {
    DrawnObjectType.BUILDING -> PropertyPanelConfig(
        title = stringResource(R.string.building_details),
        saveLabel = stringResource(R.string.save_building),
        heightRange = 2.0..100.0
    )

    DrawnObjectType.WALL -> PropertyPanelConfig(
        title = stringResource(R.string.wall_details),
        saveLabel = stringResource(R.string.save_wall),
        heightRange = 0.5..20.0
    )

    DrawnObjectType.TREE -> PropertyPanelConfig(
        title = stringResource(R.string.tree_details),
        saveLabel = stringResource(R.string.save_tree),
        heightRange = 1.0..50.0
    )
}

private data class PropertyPanelConfig(
    val title: String,
    val saveLabel: String,
    val heightRange: ClosedFloatingPointRange<Double>
)

@Preview(name = "Building details", widthDp = 420, showBackground = true)
@Composable
private fun BuildingPropertiesSheetPreview() {
    DrawingPropertiesPreview(type = DrawnObjectType.BUILDING)
}

@Preview(name = "Wall details", widthDp = 420, showBackground = true)
@Composable
private fun WallPropertiesSheetPreview() {
    DrawingPropertiesPreview(type = DrawnObjectType.WALL)
}

@Preview(name = "Tree details", widthDp = 420, showBackground = true)
@Composable
private fun TreePropertiesSheetPreview() {
    DrawingPropertiesPreview(type = DrawnObjectType.TREE)
}

@Preview(name = "Edited loaded building · tablet", widthDp = 800, showBackground = true)
@Composable
private fun EditedLoadedBuildingPropertiesSheetPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        DrawingPropertiesSheet(
            type = DrawnObjectType.BUILDING,
            initialHeightMeters = 12.5,
            initialRadiusMeters = null,
            isCreating = false,
            objectSource = SceneObjectSource.AUTOMATIC,
            isEditedAutomaticObject = true,
            loadedHeightMeters = 18.0,
            onBack = {},
            onApply = { _, _ -> },
            onDelete = {}
        )
    }
}

@Composable
private fun DrawingPropertiesPreview(type: DrawnObjectType) {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        DrawingPropertiesSheet(
            type = type,
            initialHeightMeters = when (type) {
                DrawnObjectType.BUILDING -> 6.0
                DrawnObjectType.WALL -> 2.5
                DrawnObjectType.TREE -> 8.0
            },
            initialRadiusMeters = if (type == DrawnObjectType.TREE) 2.5 else null,
            isCreating = true,
            objectSource = SceneObjectSource.MANUAL,
            onBack = {},
            onApply = { _, _ -> },
            onDelete = {}
        )
    }
}

private const val MEASUREMENT_STEP_METERS = 0.5
private const val MIN_CROWN_WIDTH_METERS = 1.0
private const val MAX_CROWN_WIDTH_METERS = 40.0
private const val HEIGHT_EQUALITY_TOLERANCE_METERS = 0.051
