package me.thenano.yamibo.yamibo_app.repository.download

const val DOWNLOADED_CONTENT_FILTER_ALL = "all"
const val DOWNLOADED_CONTENT_FILTER_TAG_MANGA = "tag_manga"
const val DOWNLOADED_CONTENT_FILTER_UNKNOWN_FORUM = "forum_unknown"

enum class DownloadedContentSortMode {
    TotalSize,
    DownloadedAt,
}

data class DownloadedContentFilterOption(
    val key: String,
    val label: String,
    val count: Int,
)

fun buildDownloadedContentFilterOptions(groups: List<DownloadedContentGroup>): List<DownloadedContentFilterOption> =
    groups
        .groupBy { it.normalizedFilterKey() to it.normalizedFilterLabel() }
        .map { (filter, grouped) ->
            DownloadedContentFilterOption(
                key = filter.first,
                label = filter.second,
                count = grouped.sumOf { it.itemCount },
            )
        }
        .sortedWith(
            compareBy<DownloadedContentFilterOption> { option ->
                when (option.key) {
                    DOWNLOADED_CONTENT_FILTER_TAG_MANGA -> 2
                    DOWNLOADED_CONTENT_FILTER_UNKNOWN_FORUM -> 1
                    else -> 0
                }
            }.thenBy { it.label }
        )

fun filterAndSortDownloadedContentGroups(
    groups: List<DownloadedContentGroup>,
    selectedFilterKeys: Set<String>,
    sortMode: DownloadedContentSortMode,
    descending: Boolean,
): List<DownloadedContentGroup> {
    val activeKeys = selectedFilterKeys - DOWNLOADED_CONTENT_FILTER_ALL
    val filtered = if (activeKeys.isEmpty()) {
        groups
    } else {
        groups.filter { it.normalizedFilterKey() in activeKeys }
    }
    val comparator = when (sortMode) {
        DownloadedContentSortMode.TotalSize -> compareBy<DownloadedContentGroup> { it.imageBytes }
        DownloadedContentSortMode.DownloadedAt -> compareBy { it.effectiveLatestDownloadedAt() }
    }.thenBy { it.title }
    return if (descending) filtered.sortedWith(comparator.reversed()) else filtered.sortedWith(comparator)
}

private fun DownloadedContentGroup.normalizedFilterKey(): String =
    filterKey.ifBlank {
        when (type) {
            DownloadedContentGroupType.TagManga -> DOWNLOADED_CONTENT_FILTER_TAG_MANGA
            DownloadedContentGroupType.RssManga -> DOWNLOADED_CONTENT_FILTER_TAG_MANGA
            DownloadedContentGroupType.Thread -> DOWNLOADED_CONTENT_FILTER_UNKNOWN_FORUM
        }
    }

private fun DownloadedContentGroup.normalizedFilterLabel(): String =
    filterLabel.ifBlank {
        when (type) {
            DownloadedContentGroupType.TagManga -> "標籤漫畫"
            DownloadedContentGroupType.RssManga -> "RSS 漫畫"
            DownloadedContentGroupType.Thread -> "未知板塊"
        }
    }

private fun DownloadedContentGroup.effectiveLatestDownloadedAt(): Long =
    latestDownloadedAt.takeIf { it > 0L } ?: items.maxOfOrNull { it.downloadedAt } ?: 0L
