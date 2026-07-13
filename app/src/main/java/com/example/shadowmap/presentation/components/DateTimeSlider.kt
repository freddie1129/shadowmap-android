package com.example.shadowmap.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.shadowmap.domain.SolarPosition
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSlider(
    selectedEpochMillis: Long,
    timeZoneId: String,
    solarPosition: SolarPosition?,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneId = remember(timeZoneId) {
        runCatching { ZoneId.of(timeZoneId) }.getOrElse { ZoneId.systemDefault() }
    }
    val selectedDateTime = Instant.ofEpochMilli(selectedEpochMillis).atZone(zoneId)
    val selectedMinutes = selectedDateTime.hour * MINUTES_PER_HOUR + selectedDateTime.minute
    val dateText = selectedDateTime.format(DATE_FORMATTER)
    val timeText = selectedDateTime.format(TIME_FORMATTER)
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { showDatePicker = true }) { Text(dateText) }
            Text(text = timeText, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onNowSelected) { Text("Now") }
        }
        Slider(
            value = selectedMinutes.toFloat(),
            onValueChange = { sliderValue ->
                val minuteOfDay =
                    (sliderValue / MINUTE_STEP).roundToInt() * MINUTE_STEP
                val selectedTime = LocalTime.of(
                    minuteOfDay / MINUTES_PER_HOUR,
                    minuteOfDay % MINUTES_PER_HOUR
                )
                val updated = LocalDateTime.of(selectedDateTime.toLocalDate(), selectedTime)
                    .atZone(zoneId)
                onDateTimeChanged(updated.toInstant().toEpochMilli())
            },
            valueRange = 0f..LAST_MINUTE_OF_DAY.toFloat(),
            steps = TIME_SLIDER_STEPS,
            modifier = Modifier.semantics {
                contentDescription = "Time of day"
                stateDescription = timeText
            }
        )
        Text(
            text = solarPosition.statusText(),
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (showDatePicker) {
        DateSelectionDialog(
            selectedDateTime = selectedDateTime,
            zoneId = zoneId,
            onDateTimeChanged = onDateTimeChanged,
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectionDialog(
    selectedDateTime: ZonedDateTime,
    zoneId: ZoneId,
    onDateTimeChanged: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateTime.toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                        val date = Instant.ofEpochMilli(selectedDateMillis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        val updated = LocalDateTime.of(date, selectedDateTime.toLocalTime())
                            .atZone(zoneId)
                        onDateTimeChanged(updated.toInstant().toEpochMilli())
                    }
                    onDismiss()
                }
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun SolarPosition?.statusText(): String = when {
    this == null -> "Load buildings to calculate sunlight"
    !isAboveHorizon -> "Sun below horizon — no direct shadow"
    else -> "Azimuth ${azimuthDegrees.roundToInt()}° · Zenith ${zenithDegrees.roundToInt()}°"
}

private const val MINUTES_PER_HOUR = 60
private const val MINUTE_STEP = 5
private const val LAST_MINUTE_OF_DAY = 23 * MINUTES_PER_HOUR + 55
private const val TIME_SLIDER_VALUES = LAST_MINUTE_OF_DAY / MINUTE_STEP + 1
private const val TIME_SLIDER_STEPS = TIME_SLIDER_VALUES - 2
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
