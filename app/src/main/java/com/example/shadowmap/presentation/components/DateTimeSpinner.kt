@file:Suppress("TooManyFunctions")

package com.example.shadowmap.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shadowmap.R
import com.example.shadowmap.domain.SolarPosition
import com.example.shadowmap.ui.theme.ShadowMapTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val DAY_WIDTH = 2.dp
private val MINUTE_WIDTH = 1.dp
private val LINE_COLOR = Color.White
private val CENTRE_ARROW_COLOR = Color(0xFFCC0000)
private const val DATE_RANGE_YEARS = 20L

/**
 * Scrollable date and time rulers ported from Sun Finder's DateTimeSpinner.
 *
 * The public API stays epoch-based so the selected instant remains unambiguous in the map's
 * display time zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun DateTimeSpinner(
    selectedEpochMillis: Long,
    timeZoneId: String,
    solarPosition: SolarPosition?,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoneId = remember(timeZoneId) {
        runCatching { ZoneId.of(timeZoneId) }.getOrElse { ZoneId.systemDefault() }
    }
    val initialDateTime = remember(selectedEpochMillis, zoneId) {
        Instant.ofEpochMilli(selectedEpochMillis).atZone(zoneId)
    }
    val dateRange = remember(initialDateTime.year) { buildDateRange(initialDateTime.toLocalDate()) }
    val timeRange = remember(initialDateTime.toLocalDate()) { buildTimeRange(initialDateTime.toLocalDate()) }
    val dateListState = rememberLazyListState()
    val timeListState = rememberLazyListState()
    val density = LocalDensity.current
    val dayWidthPx = with(density) { DAY_WIDTH.toPx() }
    val minuteWidthPx = with(density) { MINUTE_WIDTH.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val selectedDate by remember(dateListState, dateRange, dayWidthPx) {
        derivedStateOf { dateListState.dateAtViewportCentre(dateRange, dayWidthPx) }
    }
    val selectedTime by remember(timeListState, timeRange, minuteWidthPx) {
        derivedStateOf { timeListState.timeAtViewportCentre(timeRange, minuteWidthPx) }
    }

    var suppressDateCallback by remember { mutableStateOf(true) }
    var suppressTimeCallback by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(dateListState, dateRange) {
        snapshotFlow { dateListState.layoutInfo.viewportSize.width }.filter { it > 0 }.first()
        dateListState.centerOnDate(initialDateTime.toLocalDate(), dateRange, dayWidthPx)
        suppressDateCallback = false
    }
    LaunchedEffect(timeListState, timeRange) {
        snapshotFlow { timeListState.layoutInfo.viewportSize.width }.filter { it > 0 }.first()
        timeListState.centerOnTime(initialDateTime.toLocalDateTime(), timeRange, minuteWidthPx)
        suppressTimeCallback = false
    }
    LaunchedEffect(dateListState, zoneId) {
        snapshotFlow { selectedDate }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { date ->
                if (!suppressDateCallback) {
                    onDateTimeChanged(date.atTime(selectedTime ?: initialDateTime.toLocalTime()).toEpochMillis(zoneId))
                }
            }
    }
    LaunchedEffect(timeListState, zoneId) {
        snapshotFlow { selectedTime }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { time ->
                if (!suppressTimeCallback) {
                    val dateTime = (selectedDate ?: initialDateTime.toLocalDate()).atTime(time)
                    onDateTimeChanged(dateTime.toEpochMillis(zoneId))
                }
            }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (selectedDate ?: initialDateTime.toLocalDate())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            suppressDateCallback = true
                            coroutineScope.launch {
                                dateListState.centerOnDate(date, dateRange, dayWidthPx)
                                suppressDateCallback = false
                            }
                            onDateTimeChanged(
                                date.atTime(selectedTime ?: initialDateTime.toLocalTime()).toEpochMillis(zoneId),
                            )
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
    ) {
        SpinnerHeader(
            selectedDate = selectedDate ?: initialDateTime.toLocalDate(),
            selectedTime = selectedTime ?: initialDateTime.toLocalTime(),
            onReset = {
                suppressDateCallback = true
                suppressTimeCallback = true
                val now = java.time.ZonedDateTime.now(zoneId)
                coroutineScope.launch {
                    dateListState.centerOnDate(now.toLocalDate(), dateRange, dayWidthPx)
                    timeListState.centerOnTime(now.toLocalDateTime(), timeRange, minuteWidthPx)
                    suppressDateCallback = false
                    suppressTimeCallback = false
                }
                onNowSelected()
            },
            onCalendar = { showDatePicker = true },
        )
        DateRuler(dateListState, dateRange, DAY_WIDTH)
        TimeRuler(timeListState, timeRange, MINUTE_WIDTH)
        Text(
            text = solarPosition.statusText(),
            color = LINE_COLOR,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun buildDateRange(referenceDate: LocalDate): List<LocalDate> {
    val start = LocalDate.of(referenceDate.year, 1, 1).minusYears(DATE_RANGE_YEARS)
    return (0..(DATE_RANGE_YEARS * 24).toInt()).map { start.plusMonths(it.toLong()) }
}

private fun buildTimeRange(referenceDate: LocalDate): List<LocalDateTime> =
    (0..72).map { referenceDate.minusDays(1).atStartOfDay().plusHours(it.toLong()) }

@Composable
private fun SpinnerHeader(
    selectedDate: LocalDate,
    selectedTime: LocalTime,
    onReset: () -> Unit,
    onCalendar: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = selectedDate.format(DateTimeFormatter.ISO_DATE),
            color = LINE_COLOR,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            text = selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
            color = LINE_COLOR,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = onReset) {
                Icon(
                    painter = painterResource(R.drawable.baseline_settings_backup_restore_24),
                    contentDescription = "Reset to now",
                    tint = LINE_COLOR,
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = onCalendar) {
                Icon(
                    painter = painterResource(R.drawable.baseline_calendar_month_24),
                    contentDescription = "Choose date",
                    tint = LINE_COLOR,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

@Composable
private fun DateRuler(state: LazyListState, dates: List<LocalDate>, dayWidth: Dp) {
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(state = state, modifier = Modifier.fillMaxWidth()) {
            items(count = dates.size, key = { dates[it].toString() }) { index ->
                MonthRulerItem(dates[index], dayWidth)
            }
        }
        CentreArrow()
    }
}

@Composable
private fun TimeRuler(state: LazyListState, times: List<LocalDateTime>, minuteWidth: Dp) {
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(state = state, modifier = Modifier.fillMaxWidth()) {
            items(count = times.size, key = { times[it].toString() }) { index ->
                HourRulerItem(times[index], minuteWidth)
            }
        }
        CentreArrow()
    }
}

@Composable
private fun BoxScope.CentreArrow() {
    Icon(
        painter = painterResource(R.drawable.sharp_arrow_drop_down_24),
        contentDescription = null,
        tint = CENTRE_ARROW_COLOR,
        modifier = Modifier.size(width = 10.dp, height = 8.dp).align(Alignment.TopCenter),
    )
}

@Composable
private fun MonthRulerItem(month: LocalDate, dayWidth: Dp) {
    Column(
        modifier = Modifier
            .width(dayWidth * month.lengthOfMonth())
            .padding(top = 10.dp),
    ) {
        RulerTicks(count = 5)
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMM")),
            fontSize = 10.sp,
            color = LINE_COLOR,
            modifier = Modifier.centerLabelOnStart(),
        )
    }
}

@Composable
private fun HourRulerItem(hour: LocalDateTime, minuteWidth: Dp) {
    Column(
        modifier = Modifier
            .width(minuteWidth * 60)
            .padding(top = 10.dp),
    ) {
        RulerTicks(count = 12)
        Text(
            text = hour.format(DateTimeFormatter.ofPattern("hh:mm a")),
            fontSize = 8.sp,
            color = LINE_COLOR,
            modifier = Modifier.centerLabelOnStart(),
        )
    }
}

@Composable
private fun RulerTicks(count: Int) {
    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
        val strokeWidth = 1.dp.toPx()
        val spacing = size.width / count
        repeat(count) { index ->
            val x = spacing * index + strokeWidth / 2f
            val lineHeight = if (index == 0) size.height else size.height / 2f
            drawLine(LINE_COLOR, Offset(x, 0f), Offset(x, lineHeight), strokeWidth)
        }
    }
}

private fun Modifier.centerLabelOnStart(): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    layout(placeable.width, placeable.height) {
        placeable.place(-placeable.width / 2, 0)
    }
}

private fun LazyListState.dateAtViewportCentre(dates: List<LocalDate>, dayWidthPx: Float): LocalDate? {
    val centre = layoutInfo.viewportSize.width / 2f
    if (centre == 0f || dayWidthPx == 0f) return null
    return layoutInfo.visibleItemsInfo
        .firstOrNull { centre >= it.offset && centre < it.offset + it.size }
        ?.let { item ->
        val month = dates[item.index]
        val day = ceil((centre - item.offset) / dayWidthPx).toInt().coerceIn(1, month.lengthOfMonth())
        month.withDayOfMonth(day)
    }
}

private fun LazyListState.timeAtViewportCentre(times: List<LocalDateTime>, minuteWidthPx: Float): LocalTime? {
    val centre = layoutInfo.viewportSize.width / 2f
    if (centre == 0f || minuteWidthPx == 0f) return null
    return layoutInfo.visibleItemsInfo
        .firstOrNull { centre >= it.offset && centre < it.offset + it.size }
        ?.let { item ->
        val minute = floor((centre - item.offset) / minuteWidthPx).toInt().coerceIn(0, 59)
        LocalTime.of(times[item.index].hour, minute)
    }
}

@Suppress("ReturnCount")
private suspend fun LazyListState.centerOnDate(
    date: LocalDate,
    dates: List<LocalDate>,
    dayWidthPx: Float,
) {
    val viewportWidth = layoutInfo.viewportSize.width
    if (viewportWidth == 0) return
    val monthIndex = dates.indexOfFirst { it.year == date.year && it.month == date.month }
    if (monthIndex < 0) return
    val pixelsBeforeMonth = dates.take(monthIndex).sumOf { it.lengthOfMonth().toDouble() }.toFloat() * dayWidthPx
    val target = pixelsBeforeMonth + (date.dayOfMonth - 0.5f) * dayWidthPx - viewportWidth / 2f
    var consumed = 0f
    dates.forEachIndexed { index, month ->
        val monthWidth = month.lengthOfMonth() * dayWidthPx
        if (consumed + monthWidth > target.coerceAtLeast(0f)) {
            scrollToItem(index, (target.coerceAtLeast(0f) - consumed).toInt())
            return
        }
        consumed += monthWidth
    }
}

private suspend fun LazyListState.centerOnTime(
    dateTime: LocalDateTime,
    times: List<LocalDateTime>,
    minuteWidthPx: Float,
) {
    val viewportWidth = layoutInfo.viewportSize.width
    if (viewportWidth == 0) return
    val hourIndex = times.indexOfFirst { it.toLocalDate() == dateTime.toLocalDate() && it.hour == dateTime.hour }
    if (hourIndex < 0) return
    val hourWidth = minuteWidthPx * 60f
    val target = (hourIndex * hourWidth + (dateTime.minute + 0.5f) * minuteWidthPx - viewportWidth / 2f)
        .coerceAtLeast(0f)
    val itemIndex = (target / hourWidth).toInt().coerceIn(times.indices)
    scrollToItem(itemIndex, (target - itemIndex * hourWidth).toInt())
}

private fun LocalDateTime.toEpochMillis(zoneId: ZoneId): Long = atZone(zoneId).toInstant().toEpochMilli()

private fun SolarPosition?.statusText(): String = when {
    this == null -> "Load buildings to calculate sunlight"
    !isAboveHorizon -> "Sun below horizon — no direct shadow"
    else -> "Azimuth ${azimuthDegrees.roundToInt()}° · Zenith ${zenithDegrees.roundToInt()}°"
}

@Preview(
    name = "Date and time spinner",
    widthDp = 360,
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun DateTimeSpinnerPreview() {
    val zoneId = ZoneId.of("Australia/Brisbane")
    val dateTime = LocalDateTime.of(2026, 7, 13, 14, 30)

    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        DateTimeSpinner(
            selectedEpochMillis = dateTime.atZone(zoneId).toInstant().toEpochMilli(),
            timeZoneId = zoneId.id,
            solarPosition = null,
            onDateTimeChanged = {},
            onNowSelected = {},
        )
    }
}
