package me.thenano.yamibo.yamibo_app.history.components

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository.ThreadReadingHistory
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

internal fun itemKey(history: ReadHistoryRepository.AnyReadingHistory): String {
    return when (history) {
        is ThreadReadingHistory -> "thread_${history.threadType}_${history.threadId.value}_${history.authorId?.value ?: 0}"
        is ReadHistoryRepository.TagMangaReadingHistory -> "tag_${history.tagId.value}_${history.threadId.value}"
        else -> "history_${history.lastVisitTime}"
    }
}

internal fun groupByDate(
    items: List<ReadHistoryRepository.AnyReadingHistory>,
): List<Pair<String, List<ReadHistoryRepository.AnyReadingHistory>>> {
    val now = currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L
    val grouped = mutableMapOf<String, MutableList<ReadHistoryRepository.AnyReadingHistory>>()
    for (item in items) {
        val diffMs = now - item.lastVisitTime
        val label = when {
            diffMs < oneDayMs -> appString(Res.string.ui_today_2)
            diffMs < 2 * oneDayMs -> appString(Res.string.ui_yesterday)
            diffMs < 3 * oneDayMs -> appString(Res.string.ui_the_day_yesterday)
            diffMs < 7 * oneDayMs -> appString(Res.string.time_days_ago, (diffMs / oneDayMs).toInt())
            else -> formatDate(item.lastVisitTime)
        }
        grouped.getOrPut(label) { mutableListOf() }.add(item)
    }
    return grouped.toList()
}

private fun formatDate(timestamp: Long): String {
    val totalDays = timestamp / (24 * 60 * 60 * 1000L)
    var year = 1970
    var remainingDays = totalDays + (8 * 60 * 60 * 1000L / (24 * 60 * 60 * 1000L))
    while (true) {
        val daysInYear = if (isLeapYear(year)) 366L else 365L
        if (remainingDays < daysInYear) break
        remainingDays -= daysInYear
        year++
    }
    val monthDays = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 1
    for (days in monthDays) {
        if (remainingDays < days) break
        remainingDays -= days
        month++
    }
    val day = remainingDays.toInt() + 1
    return "$year/$month/$day"
}

internal fun formatTime(timestamp: Long): String {
    val adjustedMs = timestamp + 8 * 60 * 60 * 1000L
    val totalMinutes = (adjustedMs / (60 * 1000L)) % (24 * 60)
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}


