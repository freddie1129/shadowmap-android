@file:Suppress("TooManyFunctions")

package com.gooludou.shadowplanner.feature.shadowmap.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.solar.SunriseSunset
import com.gooludou.shadowplanner.core.solar.SunriseSunsetCalculator
import com.gooludou.shadowplanner.core.ui.components.PremiumFeatureBadge
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DATE_RANGE_YEARS = 20L
private val DAYLIGHT_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")

private data class SpinnerColors(
    val surface: Color,
    val content: Color,
    val tick: Color,
    val arrow: Color
)

private object DateTimeSpinnerDefaults {
    val dayWidth = 2.dp
    val minuteWidth = 1.dp
    val shadowElevation = 8.dp
    val tonalElevation = 2.dp
    val rulerHeight = 30.dp
    val edgeFadeWidth = 28.dp
    val arrowWidth = 10.dp
    val arrowHeight = 8.dp
    val rulerTopPadding = 4.dp
    val tickHeight = 10.dp
    val tickStrokeWidth = 1.dp
}

/**
 * Scrollable date and time rulers ported from Sun Finder's DateTimeSpinner.
 *
 * The public API stays epoch-based so the selected instant remains unambiguous in the map's
 * display time zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
fun DateTimeSpinner(
    selectedEpochMillis: Long,
    timeZoneId: String,
    modifier: Modifier = Modifier,
    location: GeoPoint? = null,
    onCollapse: (() -> Unit)? = null,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    isDateTimeChangeEnabled: Boolean = true,
    onLockedInteraction: () -> Unit = {}
) {
    val dimensions = ShadowMapDesign.dimensions
    val themeSurface = MaterialTheme.colorScheme.surface
    val themeOnSurface = MaterialTheme.colorScheme.onSurface
    val themePrimary = MaterialTheme.colorScheme.primary
    val spinnerColors = remember(themeSurface, themeOnSurface, themePrimary) {
        val isDark = themeSurface.luminance() < 0.5f
        if (isDark) {
            SpinnerColors(
                surface = Color.Black.copy(alpha = 0.92f),
                content = Color.White,
                tick = Color.White,
                arrow = Color(0xFFCC0000)
            )
        } else {
            SpinnerColors(
                surface = themeSurface.copy(alpha = 0.94f),
                content = themeOnSurface,
                tick = themeOnSurface.copy(alpha = 0.72f),
                arrow = themePrimary
            )
        }
    }
    val zoneId = remember(timeZoneId) {
        runCatching { ZoneId.of(timeZoneId) }.getOrElse { ZoneId.systemDefault() }
    }
    val initialDateTime = remember(selectedEpochMillis, zoneId) {
        Instant.ofEpochMilli(selectedEpochMillis).atZone(zoneId)
    }
    val dateRange = remember(initialDateTime.year) { buildDateRange(initialDateTime.toLocalDate()) }
    // The time ruler is intentionally independent from the selected date. Rebuilding this
    // range while the date ruler is dragged would reset the time ruler's scroll position.
    val timeRange = remember { buildTimeRange() }
    val dateListState = rememberLazyListState()
    val timeListState = rememberLazyListState()
    val density = LocalDensity.current
    val dayWidthPx = with(density) { DateTimeSpinnerDefaults.dayWidth.toPx() }
    val minuteWidthPx = with(density) { DateTimeSpinnerDefaults.minuteWidth.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val selectedDate by remember(dateListState, dateRange, dayWidthPx) {
        derivedStateOf { dateListState.dateAtViewportCentre(dateRange, dayWidthPx) }
    }
    val selectedTime by remember(timeListState, timeRange, minuteWidthPx) {
        derivedStateOf { timeListState.timeAtViewportCentre(timeRange, minuteWidthPx) }
    }
    val displayedDate = selectedDate ?: initialDateTime.toLocalDate()
    val sunriseSunsetCalculator = remember { SunriseSunsetCalculator() }
    val sunriseSunset = remember(displayedDate, location, zoneId) {
        location?.let { sunriseSunsetCalculator.calculate(displayedDate, zoneId, it) }
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
                    onDateTimeChanged(
                        date.atTime(
                            selectedTime ?: initialDateTime.toLocalTime()
                        ).toEpochMillis(zoneId)
                    )
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
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(
                                millis
                            ).atZone(ZoneOffset.UTC).toLocalDate()
                            suppressDateCallback = true
                            coroutineScope.launch {
                                dateListState.centerOnDate(date, dateRange, dayWidthPx)
                                suppressDateCallback = false
                            }
                            onDateTimeChanged(
                                date.atTime(
                                    selectedTime ?: initialDateTime.toLocalTime()
                                ).toEpochMillis(zoneId)
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = dimensions.floatingControlMargin)
            .padding(vertical = dimensions.spacingXs)
            .widthIn(max = dimensions.floatingControlMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = dimensions.spacingXs)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = spinnerColors.surface,
            shadowElevation = DateTimeSpinnerDefaults.shadowElevation,
            tonalElevation = DateTimeSpinnerDefaults.tonalElevation
        ) {
            Column {
                if (sunriseSunset != null || onCollapse != null) {
                    DaylightSummary(
                        sunriseSunset = sunriseSunset,
                        zoneId = zoneId,
                        colors = spinnerColors,
                        onCollapse = onCollapse
                    )
                }
                SpinnerHeader(
                    selectedDate = displayedDate,
                    selectedTime = selectedTime ?: initialDateTime.toLocalTime(),
                    onReset = {
                        suppressDateCallback = true
                        suppressTimeCallback = true
                        val now = java.time.ZonedDateTime.now(zoneId)
                        coroutineScope.launch {
                            dateListState.centerOnDate(now.toLocalDate(), dateRange, dayWidthPx)
                            timeListState.centerOnTime(
                                now.toLocalDateTime(),
                                timeRange,
                                minuteWidthPx
                            )
                            suppressDateCallback = false
                            suppressTimeCallback = false
                        }
                        onNowSelected()
                    },
                    onCalendar = {
                        if (isDateTimeChangeEnabled) {
                            showDatePicker = true
                        } else {
                            onLockedInteraction()
                        }
                    },
                    colors = spinnerColors
                )
                DateRuler(
                    state = dateListState,
                    dates = dateRange,
                    dayWidth = DateTimeSpinnerDefaults.dayWidth,
                    colors = spinnerColors,
                    userScrollEnabled = isDateTimeChangeEnabled,
                    onLockedInteraction = onLockedInteraction
                )
                TimeRuler(
                    timeListState,
                    timeRange,
                    DateTimeSpinnerDefaults.minuteWidth,
                    spinnerColors
                )
            }
        }
    }
}

@Composable
private fun DaylightSummary(
    sunriseSunset: SunriseSunset?,
    zoneId: ZoneId,
    colors: SpinnerColors,
    onCollapse: (() -> Unit)?
) {
    val collapseDescription = stringResource(R.string.hide_date_time)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onCollapse != null) {
                    Modifier
                        .clickable(onClick = onCollapse)
                        .semantics { contentDescription = collapseDescription }
                } else {
                    Modifier
                }
            )
    ) {
        sunriseSunset?.let { daylight ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = ShadowMapDesign.dimensions.spacingMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DaylightTime(
                    symbol = "↑",
                    label = stringResource(R.string.sunrise),
                    instant = daylight.sunrise,
                    zoneId = zoneId,
                    colors = colors
                )
                DaylightTime(
                    symbol = "↓",
                    label = stringResource(R.string.sunset),
                    instant = daylight.sunset,
                    zoneId = zoneId,
                    colors = colors
                )
            }
        }
        if (onCollapse != null) {
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.content.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(18.dp)
            )
        }
    }
}

/** Compact summary shown while the full date/time spinner is collapsed. */
@Composable
fun DateTimeSpinnerCollapsed(
    selectedEpochMillis: Long,
    timeZoneId: String,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    location: GeoPoint? = null
) {
    val zoneId = remember(timeZoneId) {
        runCatching { ZoneId.of(timeZoneId) }.getOrElse { ZoneId.systemDefault() }
    }
    val selectedDateTime = remember(selectedEpochMillis, zoneId) {
        Instant.ofEpochMilli(selectedEpochMillis).atZone(zoneId)
    }
    val summary = remember(selectedDateTime) {
        selectedDateTime.format(DateTimeFormatter.ofPattern("d MMM · h:mm a"))
    }
    val sunriseSunset = remember(selectedDateTime, location, zoneId) {
        location?.let {
            SunriseSunsetCalculator().calculate(selectedDateTime.toLocalDate(), zoneId, it)
        }
    }
    Surface(
        onClick = onExpand,
        modifier = modifier.heightIn(
            min = ShadowMapDesign.dimensions.minimumTouchTarget
        ),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = DateTimeSpinnerDefaults.shadowElevation,
        tonalElevation = DateTimeSpinnerDefaults.tonalElevation
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = ShadowMapDesign.dimensions.spacingMedium,
                vertical = ShadowMapDesign.dimensions.spacingXs
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    ShadowMapDesign.dimensions.spacingXs
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandLess,
                    contentDescription = stringResource(R.string.show_date_time),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.size(18.dp)
                )
            }
            sunriseSunset?.let { daylight ->
                val sunriseLabel = stringResource(R.string.sunrise)
                val sunsetLabel = stringResource(R.string.sunset)
                Text(
                    text = buildString {
                        append(sunriseLabel)
                        append(' ')
                        append(daylight.sunrise.atZone(zoneId).format(DAYLIGHT_TIME_FORMATTER))
                        append(" · ")
                        append(sunsetLabel)
                        append(' ')
                        append(daylight.sunset.atZone(zoneId).format(DAYLIGHT_TIME_FORMATTER))
                    },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun DaylightTime(
    symbol: String,
    label: String,
    instant: Instant,
    zoneId: ZoneId,
    colors: SpinnerColors
) {
    Text(
        text = "$symbol $label ${instant.atZone(zoneId).format(DAYLIGHT_TIME_FORMATTER)}",
        color = colors.content.copy(alpha = 0.78f),
        style = MaterialTheme.typography.labelLarge
    )
}

private fun buildDateRange(referenceDate: LocalDate): List<LocalDate> {
    val start = LocalDate.of(referenceDate.year, 1, 1).minusYears(DATE_RANGE_YEARS)
    return (0..(DATE_RANGE_YEARS * 24).toInt()).map { start.plusMonths(it.toLong()) }
}

private fun buildTimeRange(): List<LocalDateTime> {
    val anchor = LocalDateTime.of(2000, 1, 1, 0, 0)
    return (0..72).map { anchor.plusHours(it.toLong()) }
}

@Composable
private fun SpinnerHeader(
    selectedDate: LocalDate,
    selectedTime: LocalTime,
    onReset: () -> Unit,
    onCalendar: () -> Unit,
    colors: SpinnerColors
) {
    val dimensions = ShadowMapDesign.dimensions
    Box(modifier = Modifier.fillMaxWidth().height(dimensions.minimumTouchTarget)) {
        Text(
            text = selectedDate.format(DateTimeFormatter.ISO_DATE),
            color = colors.content,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
        Text(
            text = selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
            color = colors.content,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = dimensions.spacingXs)
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = onReset) {
                Icon(
                    painter = painterResource(R.drawable.baseline_settings_backup_restore_24),
                    contentDescription = stringResource(R.string.reset_to_now),
                    tint = colors.content,
                    modifier = Modifier.size(dimensions.iconSize)
                )
            }
            IconButton(onClick = onCalendar) {
                Icon(
                    painter = painterResource(R.drawable.baseline_calendar_month_24),
                    contentDescription = stringResource(R.string.choose_date),
                    tint = colors.content,
                    modifier = Modifier.size(dimensions.iconSize)
                )
            }
        }
    }
}

@Composable
private fun DateRuler(
    state: LazyListState,
    dates: List<LocalDate>,
    dayWidth: Dp,
    colors: SpinnerColors,
    userScrollEnabled: Boolean,
    onLockedInteraction: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(DateTimeSpinnerDefaults.rulerHeight)) {
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = userScrollEnabled
        ) {
            items(count = dates.size, key = { dates[it].toString() }) { index ->
                MonthRulerItem(dates[index], dayWidth, colors)
            }
        }
        RulerEdgeFades(colors.surface)
        CentreArrow(colors.arrow)
        if (!userScrollEnabled) {
            PremiumFeatureBadge(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = ShadowMapDesign.dimensions.spacingLarge)
            )
            LockedRulerOverlay(onLockedInteraction)
        }
    }
}

@Composable
private fun TimeRuler(
    state: LazyListState,
    times: List<LocalDateTime>,
    minuteWidth: Dp,
    colors: SpinnerColors
) {
    Box(modifier = Modifier.fillMaxWidth().height(DateTimeSpinnerDefaults.rulerHeight)) {
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(count = times.size, key = { times[it].toString() }) { index ->
                HourRulerItem(times[index], minuteWidth, colors)
            }
        }
        RulerEdgeFades(colors.surface)
        CentreArrow(colors.arrow)
    }
}

@Composable
private fun BoxScope.LockedRulerOverlay(onClick: () -> Unit) {
    val description = stringResource(R.string.premium)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
    )
}

@Composable
private fun BoxScope.RulerEdgeFades(surfaceColor: Color) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .width(DateTimeSpinnerDefaults.edgeFadeWidth)
            .fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(surfaceColor, Color.Transparent)))
    )
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .width(DateTimeSpinnerDefaults.edgeFadeWidth)
            .fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(Color.Transparent, surfaceColor)))
    )
}

@Composable
private fun BoxScope.CentreArrow(arrowColor: Color) {
    Icon(
        painter = painterResource(R.drawable.sharp_arrow_drop_down_24),
        contentDescription = null,
        tint = arrowColor,
        modifier = Modifier
            .size(
                width = DateTimeSpinnerDefaults.arrowWidth,
                height = DateTimeSpinnerDefaults.arrowHeight
            )
            .align(Alignment.TopCenter)
    )
}

@Composable
private fun MonthRulerItem(month: LocalDate, dayWidth: Dp, colors: SpinnerColors) {
    Column(
        modifier = Modifier
            .width(dayWidth * month.lengthOfMonth())
            .padding(top = DateTimeSpinnerDefaults.rulerTopPadding)
    ) {
        RulerTicks(count = 5, color = colors.tick)
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMM")),
            fontSize = 10.sp,
            color = colors.content,
            modifier = Modifier.centerLabelOnStart()
        )
    }
}

@Composable
private fun HourRulerItem(hour: LocalDateTime, minuteWidth: Dp, colors: SpinnerColors) {
    Column(
        modifier = Modifier
            .width(minuteWidth * 60)
            .padding(top = DateTimeSpinnerDefaults.rulerTopPadding)
    ) {
        RulerTicks(count = 12, color = colors.tick)
        Text(
            text = hour.format(DateTimeFormatter.ofPattern("hh:mm a")),
            fontSize = 8.sp,
            color = colors.content,
            modifier = Modifier.centerLabelOnStart()
        )
    }
}

@Composable
private fun RulerTicks(count: Int, color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(DateTimeSpinnerDefaults.tickHeight)) {
        val strokeWidth = DateTimeSpinnerDefaults.tickStrokeWidth.toPx()
        val spacing = size.width / count
        repeat(count) { index ->
            val x = spacing * index + strokeWidth / 2f
            val lineHeight = if (index == 0) size.height else size.height / 2f
            drawLine(color, Offset(x, 0f), Offset(x, lineHeight), strokeWidth)
        }
    }
}

private fun Modifier.centerLabelOnStart(): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    layout(placeable.width, placeable.height) {
        placeable.place(-placeable.width / 2, 0)
    }
}

private fun LazyListState.dateAtViewportCentre(
    dates: List<LocalDate>,
    dayWidthPx: Float
): LocalDate? {
    val centre = layoutInfo.viewportSize.width / 2f
    if (centre == 0f || dayWidthPx == 0f) return null
    return layoutInfo.visibleItemsInfo
        .firstOrNull { centre >= it.offset && centre < it.offset + it.size }
        ?.let { item ->
            val month = dates[item.index]
            val day = ceil(
                (centre - item.offset) / dayWidthPx
            ).toInt().coerceIn(1, month.lengthOfMonth())
            month.withDayOfMonth(day)
        }
}

private fun LazyListState.timeAtViewportCentre(
    times: List<LocalDateTime>,
    minuteWidthPx: Float
): LocalTime? {
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
    dayWidthPx: Float
) {
    val viewportWidth = layoutInfo.viewportSize.width
    if (viewportWidth == 0) return
    val monthIndex = dates.indexOfFirst { it.year == date.year && it.month == date.month }
    if (monthIndex < 0) return
    val pixelsBeforeMonth =
        dates.take(monthIndex).sumOf { it.lengthOfMonth().toDouble() }.toFloat() * dayWidthPx
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
    minuteWidthPx: Float
) {
    val viewportWidth = layoutInfo.viewportSize.width
    if (viewportWidth == 0) return
    val hourIndex = times.indexOfFirst { it.hour == dateTime.hour }
    if (hourIndex < 0) return
    val hourWidth = minuteWidthPx * 60f
    val target = (
        hourIndex * hourWidth + (dateTime.minute + 0.5f) * minuteWidthPx -
            viewportWidth / 2f
        )
        .coerceAtLeast(0f)
    val itemIndex = (target / hourWidth).toInt().coerceIn(times.indices)
    scrollToItem(itemIndex, (target - itemIndex * hourWidth).toInt())
}

private fun LocalDateTime.toEpochMillis(zoneId: ZoneId): Long =
    atZone(zoneId).toInstant().toEpochMilli()

@Preview(
    name = "Date and time spinner · Light",
    widthDp = 360,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
private fun DateTimeSpinnerLightPreview() {
    DateTimeSpinnerPreviewContent(darkTheme = false)
}

@Preview(
    name = "Date and time spinner · Dark",
    widthDp = 360,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun DateTimeSpinnerDarkPreview() {
    DateTimeSpinnerPreviewContent(darkTheme = true)
}

@Composable
private fun DateTimeSpinnerPreviewContent(darkTheme: Boolean) {
    val zoneId = ZoneId.of("Australia/Brisbane")
    val dateTime = LocalDateTime.of(2026, 7, 13, 14, 30)

    ShadowMapTheme(darkTheme = darkTheme, dynamicColor = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DateTimeSpinner(
                selectedEpochMillis = dateTime.atZone(zoneId).toInstant().toEpochMilli(),
                timeZoneId = zoneId.id,
                location = GeoPoint(longitude = 153.0251, latitude = -27.4698),
                onCollapse = {},
                onDateTimeChanged = {},
                onNowSelected = {},
                isDateTimeChangeEnabled = false
            )
            DateTimeSpinnerCollapsed(
                selectedEpochMillis = dateTime.atZone(zoneId).toInstant().toEpochMilli(),
                timeZoneId = zoneId.id,
                onExpand = {},
                modifier = Modifier.align(Alignment.CenterHorizontally),
                location = GeoPoint(longitude = 153.0251, latitude = -27.4698)
            )
        }
    }
}
