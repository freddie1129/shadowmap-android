package com.example.shadowmap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shadowmap.domain.DrawnObjectType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingPropertiesSheet(
    type: DrawnObjectType,
    initialHeightMeters: Double,
    initialRadiusMeters: Double?,
    isCreating: Boolean,
    onBack: () -> Unit,
    onApply: (heightMeters: Double, radiusMeters: Double?) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heightRange = when (type) {
        DrawnObjectType.BUILDING -> 2f..50f
        DrawnObjectType.WALL -> 1f..5f
        DrawnObjectType.TREE -> 1f..20f
    }
    var height by remember(type, initialHeightMeters) { mutableDoubleStateOf(initialHeightMeters) }
    var radius by remember(type, initialRadiusMeters) {
        mutableDoubleStateOf(initialRadiusMeters ?: 5.0)
    }
    ModalBottomSheet(onDismissRequest = onBack, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when (type) {
                    DrawnObjectType.BUILDING -> "How tall is this building?"
                    DrawnObjectType.WALL -> "How tall is this wall?"
                    DrawnObjectType.TREE -> "How tall and wide is this tree?"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Text("Height: %.1f m".format(height))
            Slider(
                value = height.toFloat(),
                onValueChange = { height = it.toDouble() },
                valueRange = heightRange
            )
            if (type == DrawnObjectType.TREE) {
                Text("Canopy radius: %.1f m".format(radius))
                Slider(
                    value = radius.toFloat(),
                    onValueChange = { radius = it.toDouble() },
                    valueRange = 1f..20f
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = if (isCreating) onBack else onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isCreating) "Back to drawing" else "Delete")
                }
                Button(
                    onClick = {
                        onApply(height, if (type == DrawnObjectType.TREE) radius else null)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isCreating) "Create" else "Apply")
                }
            }
        }
    }
}
