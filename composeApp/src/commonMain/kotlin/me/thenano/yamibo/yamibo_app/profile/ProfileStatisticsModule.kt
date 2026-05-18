package me.thenano.yamibo.yamibo_app.profile

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.LocalBookMarkRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.LocalReadHistoryRepository
import me.thenano.yamibo.yamibo_app.components.YamiboTopBar
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.FavoriteCategory
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.FavoriteTargetType
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.time.currentLocalDateKeyAt
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun ProfileStatisticsModule() {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val favoriteRepository = LocalFavoriteRepository.current
    val readHistoryRepository = LocalReadHistoryRepository.current
    val bookMarkRepository = LocalBookMarkRepository.current

    var chartType by rememberSaveable { mutableStateOf(ProfileChartType.Bar) }
    var chartRange by rememberSaveable { mutableStateOf(ProfileStatsRange.Week) }
    var state by remember { mutableStateOf(ProfileStatisticsState.loading(chartRange)) }

    LaunchedEffect(chartRange, favoriteRepository, readHistoryRepository, bookMarkRepository) {
        state = ProfileStatisticsState.loading(chartRange)
        state = loadProfileStatistics(
            range = chartRange,
            favoriteRepository = favoriteRepository,
            readHistoryRepository = readHistoryRepository,
            bookMarkRepository = bookMarkRepository,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.creamBackground),
    ) {
        YamiboTopBar(title = appString(Res.string.ui_reading_statistics), onBack = navigator::pop)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProfileSectionCard(title = appString(Res.string.ui_works_chapters)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMetricTile(appString(Res.string.ui_bookcase), "${state.shelfCount}", appString(Res.string.ui_work), Modifier.weight(1f))
                    ProfileMetricTile(appString(Res.string.ui_finished_reading), "${state.finishedWorkCount}", appString(Res.string.ui_work), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                ProfileMetricTile(appString(Res.string.ui_chapter), "${state.readChapterCount}/${state.chapterCount}", appString(Res.string.ui_read_total))
            }

            ProfileSectionCard(title = appString(Res.string.ui_reading_hours)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMetricTile(appString(Res.string.ui_total_hours), formatDuration(state.allReadingMillis), appString(Res.string.common_all), Modifier.weight(1f))
                    ProfileMetricTile(appString(Res.string.ui_hours_week), formatDuration(state.weekReadingMillis), appString(Res.string.ui_7_days), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                ProfileMetricTile(appString(Res.string.ui_hours_month), formatDuration(state.monthReadingMillis), appString(Res.string.ui_this_month))
            }

            ProfileSectionCard(title = appString(Res.string.ui_reading_charts)) {
                ProfileControlRow(
                    title = appString(Res.string.ui_interval),
                    options = ProfileStatsRange.entries,
                    selected = chartRange,
                    label = { it.label },
                    onSelect = { chartRange = it },
                )
                Spacer(Modifier.height(8.dp))
                ProfileControlRow(
                    title = appString(Res.string.ui_chart),
                    options = ProfileChartType.entries,
                    selected = chartType,
                    label = { it.label },
                    onSelect = { chartType = it },
                )
                Spacer(Modifier.height(12.dp))
                ProfileReadingChart(
                    chartType = chartType,
                    points = state.readingDays,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                )
            }

            ProfileSectionCard(title = appString(Res.string.ui_favorite_module_ratio)) {
                FavoriteRatioList(
                    ratios = state.favoriteRatios,
                    fallbackTypeRatios = state.favoriteTypeRatios,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = colors.orangeAccent.copy(alpha = 0.18f),
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = YamiboIcons.Statistics,
                        contentDescription = null,
                        tint = colors.brownPrimary,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    color = colors.textDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ProfileMetricTile(title: String, value: String, hint: String, modifier: Modifier = Modifier) {
    val colors = YamiboTheme.colors
    Column(
        modifier = modifier
            .background(colors.creamBackground.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = title, color = colors.brownPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(text = value, color = colors.textDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = hint, color = colors.brownLight, fontSize = 11.sp)
    }
}

@Composable
private fun <T> ProfileControlRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, color = colors.brownPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) colors.brownPrimary else colors.creamBackground,
                    modifier = Modifier.clickable { onSelect(option) },
                ) {
                    Text(
                        text = label(option),
                        color = if (isSelected) colors.creamSurface else colors.brownPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileReadingChart(
    chartType: ProfileChartType,
    points: List<ReadingDayPoint>,
    modifier: Modifier = Modifier,
) {
    val colors = YamiboTheme.colors
    val values = points.map { it.durationMillis.toFloat() }
    val maxValue = values.maxOrNull() ?: 0f
    Box(
        modifier = modifier.background(colors.creamBackground.copy(alpha = 0.54f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (points.isEmpty() || maxValue <= 0f) {
            Text(text = appString(Res.string.ui_no_reading_hours_data_yet), color = colors.brownLight, fontSize = 13.sp)
            return@Box
        }
        Canvas(modifier = Modifier.matchParentSize().padding(horizontal = 12.dp, vertical = 16.dp)) {
            val palette = listOf(
                colors.brownPrimary,
                colors.orangeAccent,
                colors.redAccent,
                colors.brownLight,
            )
            when (chartType) {
                ProfileChartType.Bar -> {
                    val slot = size.width / values.size
                    val barWidth = max(5f, slot * 0.48f)
                    values.forEachIndexed { index, value ->
                        val barHeight = (value / maxValue) * size.height
                        drawRoundRect(
                            color = colors.brownPrimary,
                            topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8f, 8f),
                        )
                    }
                }

                ProfileChartType.Histogram -> {
                    val slot = size.width / values.size
                    values.forEachIndexed { index, value ->
                        val barHeight = (value / maxValue) * size.height
                        drawRect(
                            color = colors.brownPrimary.copy(alpha = 0.78f),
                            topLeft = Offset(index * slot + 1f, size.height - barHeight),
                            size = Size(max(1f, slot - 2f), barHeight),
                        )
                    }
                }

                ProfileChartType.Line -> {
                    val step = if (values.size <= 1) size.width else size.width / values.lastIndex
                    val path = Path()
                    values.forEachIndexed { index, value ->
                        val point = Offset(index * step, size.height - (value / maxValue) * size.height)
                        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                        drawCircle(color = colors.orangeAccent, radius = 4.5f, center = point)
                    }
                    drawPath(
                        path = path,
                        color = colors.brownPrimary,
                        style = Stroke(width = 4f, cap = StrokeCap.Round),
                    )
                }

                ProfileChartType.Pie -> {
                    val total = values.sum().coerceAtLeast(1f)
                    var startAngle = -90f
                    val side = minOf(size.width, size.height)
                    val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
                    values.forEachIndexed { index, value ->
                        val sweep = value / total * 360f
                        drawArc(
                            color = palette[index % palette.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = topLeft,
                            size = Size(side, side),
                        )
                        startAngle += sweep
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteRatioList(
    ratios: List<RatioItem>,
    fallbackTypeRatios: List<RatioItem>,
) {
    val colors = YamiboTheme.colors
    val displayRatios = ratios.ifEmpty { fallbackTypeRatios }
    if (displayRatios.isEmpty()) {
        Text(text = appString(Res.string.ui_no_favorites_yet), color = colors.brownLight, fontSize = 13.sp)
        return
    }
    val total = displayRatios.sumOf { it.count }.coerceAtLeast(1)
    val palette = listOf(colors.brownPrimary, colors.orangeAccent, colors.redAccent, colors.brownLight)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        displayRatios.take(6).forEachIndexed { index, item ->
            val ratio = ceil(item.count.toDouble() * 100.0 / total.toDouble()).toInt().coerceAtMost(100)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(palette[index % palette.size], CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.label,
                    color = colors.textDark,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(text = "${item.count} / ${ratio}%", color = colors.brownPrimary, fontSize = 12.sp)
            }
        }
    }
}

private suspend fun loadProfileStatistics(
    range: ProfileStatsRange,
    favoriteRepository: me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository,
    readHistoryRepository: ReadHistoryRepository,
    bookMarkRepository: me.thenano.yamibo.yamibo_app.repository.LocalBookMarkRepository,
): ProfileStatisticsState {
    val chartRangeKeys = dateKeysForRange(range, readHistoryRepository)
    val chartDurationByDate = readHistoryRepository.getReadingDurationDays(
        chartRangeKeys.startDateKey,
        chartRangeKeys.endDateKey,
    ).associateBy { it.dateKey }
    val favoriteItems = favoriteRepository.getAllFavoriteItems()
    val bookmarkEntries = bookMarkRepository.getAllEntries()
    val favoriteRatios = favoriteRepository.getCategories()
        .mapNotNull { category -> category.toRatioItem(favoriteRepository) }
        .filter { it.count > 0 }
        .sortedByDescending { it.count }

    val favoriteTypeRatios = favoriteItems
        .groupingBy { it.targetType }
        .eachCount()
        .map { (type, count) -> RatioItem(type.label, count) }
        .sortedByDescending { it.count }

    val finishedWorkCount = bookmarkEntries
        .groupBy { "${it.targetType.name}:${it.parentId}" }
        .values
        .count { entries -> entries.isNotEmpty() && entries.all { it.read } }

    return ProfileStatisticsState(
        shelfCount = favoriteItems.size,
        allReadingMillis = readHistoryRepository.getReadingDurationTotal(ALL_START_DATE_KEY, chartRangeKeys.endDateKey),
        weekReadingMillis = readHistoryRepository.getReadingDurationTotal(dateKeysForLast(7).first(), chartRangeKeys.endDateKey),
        monthReadingMillis = readHistoryRepository.getReadingDurationTotal(currentMonthStartDateKey(), chartRangeKeys.endDateKey),
        chapterCount = bookmarkEntries.size,
        readChapterCount = bookmarkEntries.count { it.read },
        finishedWorkCount = finishedWorkCount,
        favoriteRatios = favoriteRatios,
        favoriteTypeRatios = favoriteTypeRatios,
        readingDays = chartRangeKeys.dateKeys.map { dateKey ->
            ReadingDayPoint(dateKey = dateKey, durationMillis = chartDurationByDate[dateKey]?.durationMillis ?: 0L)
        },
        loading = false,
    )
}

private suspend fun FavoriteCategory.toRatioItem(
    favoriteRepository: me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository,
): RatioItem? {
    val content = favoriteRepository.getCategoryContent(id)
    val itemIds = (
        content.directItems.map { it.id } +
            content.collections.flatMap { collection -> collection.items.map { it.id } }
        ).distinct()
    return RatioItem(name, itemIds.size)
}

private suspend fun dateKeysForRange(
    range: ProfileStatsRange,
    readHistoryRepository: ReadHistoryRepository,
): DateKeyRange {
    val endDateKey = currentLocalDateKeyAt(currentTimeMillis())
    if (range == ProfileStatsRange.All) {
        val recordedDays = readHistoryRepository.getReadingDurationDays(ALL_START_DATE_KEY, endDateKey)
            .map { it.dateKey }
        return DateKeyRange(
            startDateKey = recordedDays.firstOrNull() ?: ALL_START_DATE_KEY,
            endDateKey = endDateKey,
            dateKeys = recordedDays.ifEmpty { dateKeysForLast(7) },
        )
    }
    val dateKeys = dateKeysForLast(range.days ?: 7)
    return DateKeyRange(
        startDateKey = dateKeys.first(),
        endDateKey = endDateKey,
        dateKeys = dateKeys,
    )
}

private fun dateKeysForLast(days: Int): List<String> {
    val now = currentTimeMillis()
    return (days - 1 downTo 0).map { offset ->
        currentLocalDateKeyAt(now - offset * DAY_MILLIS)
    }
}

private fun currentMonthStartDateKey(): String {
    val current = currentLocalDateKeyAt(currentTimeMillis())
    return current.take(8) + "01"
}

private fun formatDuration(durationMillis: Long): String {
    val minutes = durationMillis / 60_000L
    val hours = minutes / 60L
    val remainingMinutes = minutes % 60L
    return when {
        hours > 0L -> "${hours}h ${remainingMinutes}m"
        minutes > 0L -> "${minutes}m"
        else -> "0m"
    }
}

private val FavoriteTargetType.label: String
    get() = when (this) {
        FavoriteTargetType.ThreadNormal -> appString(Res.string.ui_post)
        FavoriteTargetType.ThreadNovel -> appString(Res.string.ui_novel)
        FavoriteTargetType.TagManga -> appString(Res.string.ui_tag_comics)
    }

private enum class ProfileStatsRange(val label: String, val days: Int?) {
    Week(appString(Res.string.ui_7_days_week), 7),
    Month(appString(Res.string.ui_30_days), 30),
    Season(appString(Res.string.ui_90_days), 90),
    Year(appString(Res.string.ui_one_year), 365),
    All(appString(Res.string.common_all), null),
}

private enum class ProfileChartType(val label: String) {
    Bar(appString(Res.string.ui_bar_chart)),
    Histogram(appString(Res.string.ui_histogram)),
    Pie(appString(Res.string.ui_pie_chart)),
    Line(appString(Res.string.ui_line_chart)),
}

private data class ProfileStatisticsState(
    val shelfCount: Int,
    val allReadingMillis: Long,
    val weekReadingMillis: Long,
    val monthReadingMillis: Long,
    val chapterCount: Int,
    val readChapterCount: Int,
    val finishedWorkCount: Int,
    val favoriteRatios: List<RatioItem>,
    val favoriteTypeRatios: List<RatioItem>,
    val readingDays: List<ReadingDayPoint>,
    val loading: Boolean,
) {
    companion object {
        fun loading(range: ProfileStatsRange): ProfileStatisticsState {
            return ProfileStatisticsState(
                shelfCount = 0,
                allReadingMillis = 0L,
                weekReadingMillis = 0L,
                monthReadingMillis = 0L,
                chapterCount = 0,
                readChapterCount = 0,
                finishedWorkCount = 0,
                favoriteRatios = emptyList(),
                favoriteTypeRatios = emptyList(),
                readingDays = dateKeysForLast(range.days ?: 7).map { ReadingDayPoint(it, 0L) },
                loading = true,
            )
        }
    }
}

private data class DateKeyRange(
    val startDateKey: String,
    val endDateKey: String,
    val dateKeys: List<String>,
)

private data class ReadingDayPoint(
    val dateKey: String,
    val durationMillis: Long,
)

private data class RatioItem(
    val label: String,
    val count: Int,
)

private const val DAY_MILLIS = 86_400_000L
private const val ALL_START_DATE_KEY = "0000-01-01"

