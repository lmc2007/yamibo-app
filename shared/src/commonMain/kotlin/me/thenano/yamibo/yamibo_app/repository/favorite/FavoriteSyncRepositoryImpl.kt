package me.thenano.yamibo.yamibo_app.repository.favorite

import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.FavoriteType
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.FavoriteId
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ThreadId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.repository.AuthRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncActionResult
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncBulkDeleteResult
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncDeleteResult
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncPhase
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncSnapshot
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncState
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncStatus
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.ThreadRepository
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import me.thenano.yamibo.yamibo_app.util.time.epochMillisOrNull
import me.thenano.yamibo.yamiboapp.FavoriteSyncTask
import kotlin.random.Random

class FavoriteSyncRepositoryImpl(
    db: Database,
    private val authRepository: AuthRepository,
    private val favoriteRepository: FavoriteRepository,
    private val localFavoriteRepository: LocalFavoriteRepository,
    private val threadRepository: ThreadRepository,
) : FavoriteSyncRepository {
    private val taskQueries = db.favoriteSyncTaskQueries
    private val mappingQueries = db.favoriteSyncRemoteThreadQueries
    private val itemQueries = db.localFavoriteItemQueries
    private val stateFlow = MutableStateFlow<FavoriteSyncState>(FavoriteSyncState.Idle)
    private val interruptRequestedRunIds = linkedSetOf<String>()

    override val state: StateFlow<FavoriteSyncState> = stateFlow.asStateFlow()

    init {
        val now = currentTimeMillis()
        taskQueries.markRunningTasksInterrupted(
            updatedAt = now,
            elapsedDurationMs = 0L,
            message = "上次同步在 App 關閉時中斷，可重新同步。",
        )
        stateFlow.value = taskQueries
            .getLatestRecoverable()
            .executeAsOneOrNull()
            ?.toSnapshot()
            ?.toState()
            ?: FavoriteSyncState.Idle
    }

    override suspend fun startRemoteImport(targetCategoryId: Long): String {
        val now = currentTimeMillis()
        val runId = "favorite-sync-$now-${Random.nextInt(1000, 9999)}"
        val snapshot = FavoriteSyncSnapshot(
            runId = runId,
            status = FavoriteSyncStatus.RUNNING,
            phase = FavoriteSyncPhase.PREPARING,
            targetCategoryId = targetCategoryId,
            startedAt = now,
            updatedAt = now,
            lastCompletedAt = null,
            elapsedDurationMs = 0L,
            currentPage = 0,
            totalPages = null,
            scannedCount = 0,
            importedCount = 0,
            uploadedCount = 0,
            uploadTargetCount = 0,
            skippedCount = 0,
            failedCount = 0,
            logMessage = "準備同步任務",
            warningMessage = null,
            errorMessage = null,
        )
        interruptRequestedRunIds.remove(runId)
        persistSnapshot(snapshot)
        stateFlow.value = FavoriteSyncState.Running(snapshot)
        return runId
    }

    override fun observeRun(runId: String): Flow<FavoriteSyncState> {
        return state.map { current ->
            when (current) {
                FavoriteSyncState.Idle -> FavoriteSyncState.Idle
                is FavoriteSyncState.Running ->
                    if (current.snapshot.runId == runId) current else FavoriteSyncState.Idle
                is FavoriteSyncState.Interrupted ->
                    if (current.snapshot.runId == runId) current else FavoriteSyncState.Idle
                is FavoriteSyncState.Failed ->
                    if (current.snapshot.runId == runId) current else FavoriteSyncState.Idle
                is FavoriteSyncState.Completed ->
                    if (current.snapshot.runId == runId) current else FavoriteSyncState.Idle
            }
        }
    }

    override suspend fun resumeInterruptedRun(): String? {
        val latest = getLatestSnapshot() ?: return null
        return when (latest.status) {
            FavoriteSyncStatus.RUNNING -> latest.runId
            FavoriteSyncStatus.INTERRUPTED,
            FavoriteSyncStatus.FAILED,
            FavoriteSyncStatus.COMPLETED -> startRemoteImport(latest.targetCategoryId)
        }
    }

    override suspend fun cancelUiAttachment(runId: String) {
        val current = currentSnapshotOrNull()
        if (current?.runId == runId) {
            stateFlow.value = current.toState()
        }
    }

    override suspend fun interruptRun(runId: String) {
        interruptRequestedRunIds += runId
        val snapshot = taskQueries.getByRunId(runId).executeAsOneOrNull()?.toSnapshot() ?: return
        if (snapshot.status == FavoriteSyncStatus.RUNNING) {
            interruptRun(snapshot, "同步已取消。")
        }
    }

    override suspend fun getLatestSnapshot(): FavoriteSyncSnapshot? {
        return taskQueries.getLatestRecoverable().executeAsOneOrNull()?.toSnapshot()
    }


    override suspend fun syncLocalFavoriteItem(itemId: Long): FavoriteSyncActionResult {
        val item = itemQueries.getById(itemId).executeAsOneOrNull()
            ?: return FavoriteSyncActionResult(false, "找不到要同步的收藏。")
        val threadId = item.asThreadIdOrNull()
            ?: return FavoriteSyncActionResult(false, "這種類型的收藏目前不支援同步到百合會。")
        val existingMapping = mappingQueries.getByThreadId(threadId.value.toLong()).executeAsOneOrNull()
        if (existingMapping?.remoteFavoriteId != null) {
            upsertRemoteMapping(
                threadId = threadId,
                remoteFavoriteId = FavoriteId(existingMapping.remoteFavoriteId.toInt()),
                remoteFavoritedOrder = existingMapping.remoteFavoritedOrder ?: existingMapping.remoteFavoriteId,
                itemId = item.id,
            )
            return FavoriteSyncActionResult(true, "此收藏已同步到百合會。")
        }

        val formHash = when (val formHashResult = ensureFormHash()) {
            is YamiboResult.Success -> formHashResult.value
            is YamiboResult.NotLoggedIn -> return FavoriteSyncActionResult(false, "目前未登入百合會，無法同步收藏。")
            is YamiboResult.NoPermission -> return FavoriteSyncActionResult(false, formHashResult.reason)
            is YamiboResult.Maintenance -> return FavoriteSyncActionResult(false, "百合會目前維護中，請稍後再試。")
            is YamiboResult.Failure -> return FavoriteSyncActionResult(false, truncateLogLine(formHashResult.reason))
        }

        when (val addResult = threadRepository.addFavorite(threadId, formHash)) {
            is YamiboResult.Success -> Unit
            is YamiboResult.NotLoggedIn -> return FavoriteSyncActionResult(false, "目前未登入百合會，無法同步收藏。")
            is YamiboResult.NoPermission -> return FavoriteSyncActionResult(false, addResult.reason)
            is YamiboResult.Maintenance -> return FavoriteSyncActionResult(false, "百合會目前維護中，請稍後再試。")
            is YamiboResult.Failure -> return FavoriteSyncActionResult(false, truncateLogLine(addResult.reason))
        }

        return when (val reconcile = fetchRemoteFavoritesSilently()) {
            is RemoteFetchResult.Success -> {
                val remoteItem = reconcile.items[threadId]
                if (remoteItem != null) {
                    upsertRemoteMapping(
                        threadId = threadId,
                        remoteFavoriteId = remoteItem.favoriteId,
                        remoteFavoritedOrder = remoteItem.favoriteId.value.toLong(),
                        itemId = item.id,
                    )
                    FavoriteSyncActionResult(true, "已同步到百合會。")
                } else {
                    FavoriteSyncActionResult(true, "已同步到百合會，但暫時無法回填收藏順序，下一次同步會補齊。")
                }
            }

            is RemoteFetchResult.Failure -> FavoriteSyncActionResult(
                true,
                "已同步到百合會，但暫時無法回填收藏順序：${truncateLogLine(reconcile.reason)}",
            )
        }
    }

    override suspend fun hasRemoteFavorite(itemId: Long): Boolean {
        return mappingQueries.getByItemId(itemId).executeAsOneOrNull()?.remoteFavoriteId != null
    }

    override suspend fun getRemoteFavoriteOrderMap(itemIds: Set<Long>): Map<Long, Long> {
        if (itemIds.isEmpty()) return emptyMap()
        return mappingQueries.getAll().executeAsList()
            .mapNotNull { mapping ->
                val itemId = mapping.itemId ?: return@mapNotNull null
                if (itemId !in itemIds) return@mapNotNull null
                val order = mapping.remoteFavoritedOrder ?: mapping.remoteFavoriteId ?: return@mapNotNull null
                itemId to order
            }
            .toMap()
    }

    override suspend fun runImport(runId: String) {
        interruptRequestedRunIds.remove(runId)
        val initial = taskQueries.getByRunId(runId).executeAsOneOrNull()?.toSnapshot() ?: return
        if (shouldStop(runId)) {
            interruptRun(initial, "同步已取消。")
            return
        }

        val category = localFavoriteRepository.getCategories().firstOrNull { it.id == initial.targetCategoryId }
        if (category == null) {
            failRun(initial, "同步目標類別不存在，請重新選擇。")
            return
        }

        val warnings = linkedSetOf<String>()
        val logs = mutableListOf<String>()

        var current = updateSnapshot(
            initial.copy(
                phase = FavoriteSyncPhase.PREPARING,
                errorMessage = null,
                warningMessage = null,
                logMessage = null,
            ),
            warnings = warnings,
            logs = logs,
        )

        appendLog(logs, "準備同步任務")
        appendLog(logs, "開始同步")
        appendLog(logs, "頁數 正在取得收藏頁")
        current = updateSnapshot(
            current.copy(phase = FavoriteSyncPhase.FETCHING_REMOTE),
            warnings = warnings,
            logs = logs,
        )

        val remoteItems = linkedMapOf<ThreadId, RemoteFavoriteItem>()
        var page = 1
        var totalPages: Int? = null

        while (true) {
            if (shouldStop(runId)) {
                interruptRun(current, "同步已取消。")
                return
            }

            when (val result = favoriteRepository.fetchFavorites(type = FavoriteType.Thread, page = page)) {
                is YamiboResult.Success -> {
                    val pageTotal = result.value.pageNav?.totalPages
                    if (totalPages != null && pageTotal != null && totalPages != pageTotal) {
                        warnings += truncateLogLine("同步期間網站收藏頁數發生變動，建議再同步一次。")
                    }
                    totalPages = pageTotal ?: totalPages ?: page

                    result.value.items.forEach { remote ->
                        val threadId = remote.toThreadId()
                        if (threadId == null) {
                            warnings += truncateLogLine("收藏項目缺少帖子 ID：${remote.name}")
                            return@forEach
                        }
                        if (remoteItems.containsKey(threadId)) {
                            warnings += truncateLogLine("網站收藏列表出現重複帖子：${formatPostLabel(threadId, remote.name)}")
                        }
                        remoteItems[threadId] = RemoteFavoriteItem(
                            threadId = threadId,
                            favoriteId = remote.favId,
                            title = remote.name,
                        )
                    }

                    appendLog(logs, "頁數 $page/${totalPages} 頁")
                    appendLog(logs, "已取得 ${remoteItems.size} 項收藏")
                    current = updateSnapshot(
                        current.copy(
                            phase = FavoriteSyncPhase.FETCHING_REMOTE,
                            currentPage = page,
                            totalPages = totalPages,
                            scannedCount = remoteItems.size,
                        ),
                        warnings = warnings,
                        logs = logs,
                    )

                    val reachedLastPage = page >= totalPages
                    val hasMoreByNav = result.value.pageNav?.nextUrl != null
                    if (reachedLastPage || (!hasMoreByNav && result.value.items.isEmpty())) {
                        break
                    }
                    page += 1
                }

                is YamiboResult.NotLoggedIn -> {
                    failRun(current.copy(failedCount = current.failedCount + 1), "目前未登入百合會，請重新登入後再同步。")
                    return
                }

                is YamiboResult.NoPermission -> {
                    failRun(current.copy(failedCount = current.failedCount + 1), result.reason)
                    return
                }

                is YamiboResult.Maintenance -> {
                    failRun(current.copy(failedCount = current.failedCount + 1), "百合會目前維護中，請稍後再試。")
                    return
                }

                is YamiboResult.Failure -> {
                    failRun(current.copy(failedCount = current.failedCount + 1), truncateLogLine(result.reason))
                    return
                }
            }
        }

        if (shouldStop(runId)) {
            interruptRun(current, "同步已取消。")
            return
        }

        appendLog(logs, "匯入網站帖子")
        current = updateSnapshot(
            current.copy(
                phase = FavoriteSyncPhase.IMPORTING_REMOTE,
                scannedCount = remoteItems.size,
            ),
            warnings = warnings,
            logs = logs,
        )

        val duplicateSyncedPathCounts = linkedMapOf<String, Int>()
        val remoteThreadIds = remoteItems.keys.toSet()

        for (remoteItem in remoteItems.values) {
            if (shouldStop(runId)) {
                interruptRun(current, "同步已取消。")
                return
            }

            val existingItem =
                itemQueries.findAnyThreadByTargetId(remoteItem.threadId.value.toLong()).executeAsOneOrNull()
            val existingMapping = mappingQueries.getByThreadId(remoteItem.threadId.value.toLong()).executeAsOneOrNull()

            val itemId = when {
                existingItem != null && existingMapping?.itemId != null -> {
                    val path = localFavoriteRepository
                        .getFavoritePaths(existingItem.id)
                        .firstOrNull()
                        ?: category.name
                    duplicateSyncedPathCounts[path] = (duplicateSyncedPathCounts[path] ?: 0) + 1
                    current = updateSnapshot(
                        current.copy(importedCount = current.importedCount + 1),
                        warnings = warnings,
                        logs = logs,
                    )
                    existingItem.id
                }

                existingItem != null -> {
                    localFavoriteRepository.addItemsToLocations(
                        itemIds = setOf(existingItem.id),
                        categoryIds = setOf(current.targetCategoryId),
                    )
                    current = updateSnapshot(
                        current.copy(importedCount = current.importedCount + 1),
                        warnings = warnings,
                        logs = logs,
                    )
                    existingItem.id
                }

                else -> {
                    when (val threadResult = fetchThreadSummary(remoteItem.threadId)) {
                        is YamiboResult.Success -> {
                            persistRemoteThreadIntoLocal(threadResult.value, current.targetCategoryId)
                            current = updateSnapshot(
                                current.copy(importedCount = current.importedCount + 1),
                                warnings = warnings,
                                logs = logs,
                            )
                            itemQueries.findAnyThreadByTargetId(remoteItem.threadId.value.toLong())
                                .executeAsOneOrNull()
                                ?.id
                        }

                        is YamiboResult.NotLoggedIn -> {
                            failRun(current.copy(failedCount = current.failedCount + 1), "目前未登入百合會，請重新登入後再同步。")
                            return
                        }

                        is YamiboResult.NoPermission -> {
                            warnings += importFailureMessage(remoteItem, threadResult.reason)
                            current = updateSnapshot(
                                current.copy(failedCount = current.failedCount + 1),
                                warnings = warnings,
                                logs = logs,
                            )
                            null
                        }

                        is YamiboResult.Maintenance -> {
                            failRun(current.copy(failedCount = current.failedCount + 1), "百合會目前維護中，請稍後再試。")
                            return
                        }

                        is YamiboResult.Failure -> {
                            warnings += importFailureMessage(remoteItem, threadResult.reason)
                            current = updateSnapshot(
                                current.copy(failedCount = current.failedCount + 1),
                                warnings = warnings,
                                logs = logs,
                            )
                            null
                        }
                    }
                }
            }

            upsertRemoteMapping(
                threadId = remoteItem.threadId,
                remoteFavoriteId = remoteItem.favoriteId,
                remoteFavoritedOrder = remoteItem.favoriteId.value.toLong(),
                itemId = itemId,
            )
        }

        duplicateSyncedPathCounts.forEach { (path, count) ->
            appendLog(logs, "有 $count 項收藏已曾同步過至 $path，不進行重複同步")
        }
        current = updateSnapshot(current, warnings = warnings, logs = logs)

        val formHash = when (val formHashResult = ensureFormHash()) {
            is YamiboResult.Success -> formHashResult.value
            is YamiboResult.NotLoggedIn -> {
                failRun(current, "目前未登入百合會，請重新登入後再同步。")
                return
            }

            is YamiboResult.NoPermission -> {
                failRun(current, formHashResult.reason)
                return
            }

            is YamiboResult.Maintenance -> {
                failRun(current, "百合會目前維護中，請稍後再試。")
                return
            }

            is YamiboResult.Failure -> {
                failRun(current, truncateLogLine(formHashResult.reason))
                return
            }
        }

        current = updateSnapshot(
            current.copy(phase = FavoriteSyncPhase.UPLOADING_LOCAL),
            warnings = warnings,
            logs = logs,
        )

        val categoryThreadItems = collectCategoryThreadItems(current.targetCategoryId)
        val uploadTargets = categoryThreadItems.filter { ThreadId(it.targetId.toInt()) !in remoteThreadIds }

        current = updateSnapshot(
            current.copy(uploadTargetCount = uploadTargets.size),
            warnings = warnings,
            logs = logs,
        )

        for (item in uploadTargets) {
            if (shouldStop(runId)) {
                interruptRun(current, "同步已取消。")
                return
            }

            val threadId = ThreadId(item.targetId.toInt())
            when (val addResult = threadRepository.addFavorite(threadId, formHash)) {
                is YamiboResult.Success -> {
                    current = updateSnapshot(
                        current.copy(uploadedCount = current.uploadedCount + 1),
                        warnings = warnings,
                        logs = logs,
                    )
                }

                is YamiboResult.NotLoggedIn -> {
                    failRun(current, "目前未登入百合會，請重新登入後再同步。")
                    return
                }

                is YamiboResult.NoPermission -> {
                    warnings += uploadFailureMessage(item.title, threadId, addResult.reason)
                    current = updateSnapshot(
                        current.copy(failedCount = current.failedCount + 1),
                        warnings = warnings,
                        logs = logs,
                    )
                }

                is YamiboResult.Maintenance -> {
                    failRun(current, "百合會目前維護中，請稍後再試。")
                    return
                }

                is YamiboResult.Failure -> {
                    warnings += uploadFailureMessage(item.title, threadId, addResult.reason)
                    current = updateSnapshot(
                        current.copy(failedCount = current.failedCount + 1),
                        warnings = warnings,
                        logs = logs,
                    )
                }
            }
        }

        if (current.uploadedCount > 0) {
            when (val reconcile = fetchRemoteFavoritesSilently()) {
                is RemoteFetchResult.Success -> {
                    reconcile.items.values.forEach { remoteItem ->
                        val localItem =
                            itemQueries.findAnyThreadByTargetId(remoteItem.threadId.value.toLong()).executeAsOneOrNull()
                        upsertRemoteMapping(
                            threadId = remoteItem.threadId,
                            remoteFavoriteId = remoteItem.favoriteId,
                            remoteFavoritedOrder = remoteItem.favoriteId.value.toLong(),
                            itemId = localItem?.id,
                        )
                    }
                }

                is RemoteFetchResult.Failure -> {
                    warnings += truncateLogLine("重新對齊網站收藏資料失敗：${reconcile.reason}")
                }
            }
        }

        val completedAt = currentTimeMillis()
        val completed = current.copy(
            status = FavoriteSyncStatus.COMPLETED,
            phase = FavoriteSyncPhase.COMPLETED,
            updatedAt = completedAt,
            lastCompletedAt = completedAt,
            elapsedDurationMs = (completedAt - current.startedAt).coerceAtLeast(0L),
            warningMessage = warnings.takeIf { it.isNotEmpty() }?.joinToString("\n"),
            logMessage = logs.takeIf { it.isNotEmpty() }?.joinToString("\n"),
            errorMessage = null,
        )
        persistSnapshot(completed)
        stateFlow.value = FavoriteSyncState.Completed(completed)
        interruptRequestedRunIds.remove(runId)
    }

    override suspend fun removeLocalFavoriteItem(itemId: Long, removeRemote: Boolean): FavoriteSyncDeleteResult {
        val item = itemQueries.getById(itemId).executeAsOneOrNull()
            ?: return FavoriteSyncDeleteResult(success = true)
        val mapping = mappingQueries.getByItemId(itemId).executeAsOneOrNull()

        if (removeRemote) {
            if (mapping?.remoteFavoriteId != null) {
                val formHash = when (val formHashResult = ensureFormHash()) {
                    is YamiboResult.Success -> formHashResult.value
                    is YamiboResult.NotLoggedIn ->
                        return FavoriteSyncDeleteResult(false, "目前未登入百合會，無法同步刪除網站收藏。")
                    is YamiboResult.NoPermission ->
                        return FavoriteSyncDeleteResult(false, formHashResult.reason)
                    is YamiboResult.Maintenance ->
                        return FavoriteSyncDeleteResult(false, "百合會目前維護中，請稍後再試。")
                    is YamiboResult.Failure ->
                        return FavoriteSyncDeleteResult(false, truncateLogLine(formHashResult.reason))
                }

                when (val remoteResult =
                    favoriteRepository.removeFavorite(FavoriteId(mapping.remoteFavoriteId.toInt()), formHash)) {
                    is YamiboResult.Success -> Unit
                    is YamiboResult.NotLoggedIn ->
                        return FavoriteSyncDeleteResult(false, "目前未登入百合會，無法同步刪除網站收藏。")
                    is YamiboResult.NoPermission ->
                        return FavoriteSyncDeleteResult(false, remoteResult.reason)
                    is YamiboResult.Maintenance ->
                        return FavoriteSyncDeleteResult(false, "百合會目前維護中，請稍後再試。")
                    is YamiboResult.Failure ->
                        return FavoriteSyncDeleteResult(false, truncateLogLine(remoteResult.reason))
                }
            } else if (mapping != null) {
                return FavoriteSyncDeleteResult(
                    success = false,
                    message = "這筆收藏缺少網站 favorite id，暫時無法同步刪除網站端資料。",
                )
            }
        }

        localFavoriteRepository.deleteFavoriteItems(setOf(item.id))
        if (!removeRemote && mapping?.remoteFavoriteId != null) {
            mappingQueries.upsertMapping(
                threadId = mapping.threadId,
                remoteFavoriteId = mapping.remoteFavoriteId,
                remoteFavoritedOrder = mapping.remoteFavoritedOrder,
                itemId = null,
                lastSeenAt = mapping.lastSeenAt,
                lastSyncedAt = currentTimeMillis(),
            )
        } else {
            mappingQueries.deleteByItemId(item.id)
        }
        return FavoriteSyncDeleteResult(success = true)
    }

    override suspend fun removeLocalFavoriteItems(
        itemIds: Set<Long>,
        removeRemote: Boolean,
    ): FavoriteSyncBulkDeleteResult {
        var deletedCount = 0
        val messages = mutableListOf<String>()
        for (itemId in itemIds) {
            val result = removeLocalFavoriteItem(itemId, removeRemote = removeRemote)
            if (result.success) {
                deletedCount += 1
            } else {
                messages += result.message ?: "刪除失敗"
            }
        }
        return FavoriteSyncBulkDeleteResult(
            deletedCount = deletedCount,
            failedCount = itemIds.size - deletedCount,
            messages = messages,
        )
    }

    private suspend fun persistRemoteThreadIntoLocal(threadPage: ThreadPage, categoryId: Long) {
        val isNovel = YamiboForum.isNovelForum(threadPage.thread.forum.fid)
        val authorId = threadPage.posts.firstOrNull()?.author?.uid
        val coverUrl = extractCoverUrl(threadPage)
        val lastUpdatedTime = extractLastUpdatedTime(threadPage)
        if (isNovel) {
            localFavoriteRepository.addNovelThreadFavorite(
                tid = threadPage.thread.tid,
                title = threadPage.thread.title,
                authorId = authorId,
                coverUrl = coverUrl,
                lastUpdatedTime = lastUpdatedTime,
                forumId = threadPage.thread.forum.fid,
                forumName = threadPage.thread.forum.name,
                categoryIds = listOf(categoryId),
            )
        } else {
            localFavoriteRepository.addNormalThreadFavorite(
                tid = threadPage.thread.tid,
                title = threadPage.thread.title,
                coverUrl = coverUrl,
                lastUpdatedTime = lastUpdatedTime,
                forumId = threadPage.thread.forum.fid,
                forumName = threadPage.thread.forum.name,
                categoryIds = listOf(categoryId),
            )
        }
    }

    private suspend fun collectCategoryThreadItems(categoryId: Long): List<LocalFavoriteRepository.FavoriteItem> {
        val content = localFavoriteRepository.getCategoryContent(categoryId)
        return buildList {
            addAll(content.directItems)
            content.collections.forEach { addAll(it.items) }
        }
            .distinctBy { it.id }
            .filter {
                it.targetType == LocalFavoriteRepository.FavoriteTargetType.ThreadNormal ||
                    it.targetType == LocalFavoriteRepository.FavoriteTargetType.ThreadNovel
            }
    }

    private suspend fun ensureFormHash(): YamiboResult<FormHash> {
        authRepository.currentUser()?.formHash?.let { return YamiboResult.Success(it) }
        return when (val authResult = authRepository.fetchStatus()) {
            is YamiboResult.Success -> {
                authRepository.currentUser()?.formHash?.let { YamiboResult.Success(it) }
                    ?: YamiboResult.Failure("登入狀態已更新，但仍無法取得 formHash。")
            }

            is YamiboResult.NotLoggedIn -> YamiboResult.NotLoggedIn
            is YamiboResult.NoPermission -> YamiboResult.NoPermission(authResult.reason)
            is YamiboResult.Maintenance -> YamiboResult.Maintenance
            is YamiboResult.Failure -> YamiboResult.Failure(authResult.reason, authResult.exception)
        }
    }

    private fun extractCoverUrl(threadPage: ThreadPage): String? {
        val firstPagePosts = threadPage.posts
        val threadAuthorId = firstPagePosts.firstOrNull()?.author?.uid ?: return null
        val authorPosts = firstPagePosts.filter { it.author.uid == threadAuthorId }
        val isMangaThread = YamiboForum.isMangaForum(threadPage.thread.forum.fid)
        val attachedImageUrl = if (isMangaThread) {
            val candidateImages = authorPosts.take(2).flatMap { it.images }
            candidateImages.getOrNull(1)?.url ?: candidateImages.getOrNull(0)?.url
        } else {
            authorPosts.firstOrNull()?.images?.firstOrNull()?.url
        } ?: return null

        if (
            attachedImageUrl.contains("none.gif") ||
            attachedImageUrl.contains("smiley/") ||
            attachedImageUrl.contains("face")
        ) {
            return null
        }

        return if (attachedImageUrl.startsWith("http")) {
            attachedImageUrl
        } else {
            "${YamiboRoute.Domain.build()}$attachedImageUrl"
        }
    }

    private fun extractLastUpdatedTime(threadPage: ThreadPage): Long? {
        val firstPost = threadPage.posts.firstOrNull { it.floor == 1 } ?: threadPage.posts.firstOrNull()
        return firstPost?.lastEditedTime?.epochMillisOrNull() ?: firstPost?.timeCreate?.epochMillisOrNull()
    }

    private suspend fun fetchThreadSummary(threadId: ThreadId): YamiboResult<ThreadPage> {
        repeat(2) { attempt ->
            val result = threadRepository.fetchThread(threadId)
            if (result !is YamiboResult.Failure || attempt == 1) {
                return result
            }
        }
        return threadRepository.fetchThread(threadId)
    }

    private suspend fun fetchRemoteFavoritesSilently(): RemoteFetchResult {
        val items = linkedMapOf<ThreadId, RemoteFavoriteItem>()
        var page = 1
        var totalPages: Int? = null

        while (true) {
            when (val result = favoriteRepository.fetchFavorites(type = FavoriteType.Thread, page = page)) {
                is YamiboResult.Success -> {
                    totalPages = result.value.pageNav?.totalPages ?: totalPages ?: page
                    result.value.items.forEach { remote ->
                        val threadId = remote.toThreadId() ?: return@forEach
                        items[threadId] = RemoteFavoriteItem(
                            threadId = threadId,
                            favoriteId = remote.favId,
                            title = remote.name,
                        )
                    }

                    val reachedLastPage = page >= totalPages
                    val hasMoreByNav = result.value.pageNav?.nextUrl != null
                    if (reachedLastPage || (!hasMoreByNav && result.value.items.isEmpty())) {
                        return RemoteFetchResult.Success(items, page, totalPages)
                    }
                    page += 1
                }

                is YamiboResult.NotLoggedIn -> return RemoteFetchResult.Failure("目前未登入百合會。")
                is YamiboResult.NoPermission -> return RemoteFetchResult.Failure(result.reason)
                is YamiboResult.Maintenance -> return RemoteFetchResult.Failure("百合會目前維護中。")
                is YamiboResult.Failure -> return RemoteFetchResult.Failure(result.reason)
            }
        }
    }

    private fun upsertRemoteMapping(
        threadId: ThreadId,
        remoteFavoriteId: FavoriteId?,
        remoteFavoritedOrder: Long? = remoteFavoriteId?.value?.toLong(),
        itemId: Long?,
    ) {
        val now = currentTimeMillis()
        mappingQueries.upsertMapping(
            threadId = threadId.value.toLong(),
            remoteFavoriteId = remoteFavoriteId?.value?.toLong(),
            remoteFavoritedOrder = remoteFavoritedOrder,
            itemId = itemId,
            lastSeenAt = now,
            lastSyncedAt = now,
        )
    }

    private fun appendLog(logs: MutableList<String>, message: String) {
        logs += truncateLogLine(message)
    }

    private fun formatPostLabel(threadId: ThreadId, title: String): String {
        return "#${threadId.value} ${title.ifBlank { "未命名帖子" }}"
    }

    private fun importFailureMessage(
        remoteItem: RemoteFavoriteItem,
        reason: String,
    ): String {
        return truncateLogLine("無法匯入帖子 ${formatPostLabel(remoteItem.threadId, remoteItem.title)}：$reason")
    }

    private fun uploadFailureMessage(
        title: String,
        threadId: ThreadId,
        reason: String,
    ): String {
        return truncateLogLine("無法同步到百合會 ${formatPostLabel(threadId, title)}：$reason")
    }

    private fun truncateLogLine(message: String, maxChars: Int = 100): String {
        val normalized = message
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (normalized.length <= maxChars) normalized else normalized.take(maxChars) + "..."
    }

    private fun shouldStop(runId: String): Boolean = runId in interruptRequestedRunIds

    private fun currentSnapshotOrNull(): FavoriteSyncSnapshot? {
        return when (val current = stateFlow.value) {
            FavoriteSyncState.Idle -> null
            is FavoriteSyncState.Running -> current.snapshot
            is FavoriteSyncState.Interrupted -> current.snapshot
            is FavoriteSyncState.Failed -> current.snapshot
            is FavoriteSyncState.Completed -> current.snapshot
        }
    }

    private fun updateSnapshot(
        snapshot: FavoriteSyncSnapshot,
        warnings: Set<String>? = null,
        logs: List<String>? = null,
    ): FavoriteSyncSnapshot {
        val now = currentTimeMillis()
        val updated = snapshot.copy(
            updatedAt = now,
            elapsedDurationMs = (now - snapshot.startedAt).coerceAtLeast(0L),
            warningMessage = warnings?.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: snapshot.warningMessage,
            logMessage = logs?.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: snapshot.logMessage,
        )
        persistSnapshot(updated)
        stateFlow.value = updated.toState()
        return updated
    }

    private fun interruptRun(snapshot: FavoriteSyncSnapshot, reason: String) {
        val now = currentTimeMillis()
        val interrupted = snapshot.copy(
            status = FavoriteSyncStatus.INTERRUPTED,
            phase = FavoriteSyncPhase.INTERRUPTED,
            updatedAt = now,
            elapsedDurationMs = (now - snapshot.startedAt).coerceAtLeast(0L),
            errorMessage = reason,
        )
        persistSnapshot(interrupted)
        stateFlow.value = FavoriteSyncState.Interrupted(interrupted)
        interruptRequestedRunIds.remove(snapshot.runId)
    }

    private fun failRun(snapshot: FavoriteSyncSnapshot, reason: String) {
        val now = currentTimeMillis()
        val failed = snapshot.copy(
            status = FavoriteSyncStatus.FAILED,
            phase = FavoriteSyncPhase.FAILED,
            updatedAt = now,
            elapsedDurationMs = (now - snapshot.startedAt).coerceAtLeast(0L),
            errorMessage = reason,
        )
        persistSnapshot(failed)
        stateFlow.value = FavoriteSyncState.Failed(failed)
    }

    private fun persistSnapshot(snapshot: FavoriteSyncSnapshot) {
        taskQueries.insertOrReplace(
            runId = snapshot.runId,
            status = snapshot.status.name,
            targetCategoryId = snapshot.targetCategoryId,
            startedAt = snapshot.startedAt,
            updatedAt = snapshot.updatedAt,
            lastCompletedAt = snapshot.lastCompletedAt,
            elapsedDurationMs = snapshot.elapsedDurationMs,
            phase = snapshot.phase.name,
            currentPage = snapshot.currentPage.toLong(),
            totalPages = snapshot.totalPages?.toLong(),
            scannedCount = snapshot.scannedCount.toLong(),
            importedCount = snapshot.importedCount.toLong(),
            uploadedCount = snapshot.uploadedCount.toLong(),
            uploadTargetCount = snapshot.uploadTargetCount.toLong(),
            skippedCount = snapshot.skippedCount.toLong(),
            failedCount = snapshot.failedCount.toLong(),
            logMessage = snapshot.logMessage,
            warningMessage = snapshot.warningMessage,
            errorMessage = snapshot.errorMessage,
        )
    }

    private fun FavoriteSyncTask.toSnapshot(): FavoriteSyncSnapshot {
        return FavoriteSyncSnapshot(
            runId = runId,
            status = FavoriteSyncStatus.valueOf(status),
            phase = FavoriteSyncPhase.valueOf(phase),
            targetCategoryId = targetCategoryId,
            startedAt = startedAt,
            updatedAt = updatedAt,
            lastCompletedAt = lastCompletedAt,
            elapsedDurationMs = elapsedDurationMs,
            currentPage = currentPage.toInt(),
            totalPages = totalPages?.toInt(),
            scannedCount = scannedCount.toInt(),
            importedCount = importedCount.toInt(),
            uploadedCount = uploadedCount.toInt(),
            uploadTargetCount = uploadTargetCount.toInt(),
            skippedCount = skippedCount.toInt(),
            failedCount = failedCount.toInt(),
            logMessage = logMessage,
            warningMessage = warningMessage,
            errorMessage = errorMessage,
        )
    }

    private fun FavoriteSyncSnapshot.toState(): FavoriteSyncState {
        return when (status) {
            FavoriteSyncStatus.RUNNING -> FavoriteSyncState.Running(this)
            FavoriteSyncStatus.INTERRUPTED -> FavoriteSyncState.Interrupted(this)
            FavoriteSyncStatus.FAILED -> FavoriteSyncState.Failed(this)
            FavoriteSyncStatus.COMPLETED -> FavoriteSyncState.Completed(this)
        }
    }



    private fun me.thenano.yamibo.yamiboapp.LocalFavoriteItem.asThreadIdOrNull(): ThreadId? {
        return when (LocalFavoriteRepository.FavoriteTargetType.fromStorage(targetType)) {
            LocalFavoriteRepository.FavoriteTargetType.ThreadNormal,
            LocalFavoriteRepository.FavoriteTargetType.ThreadNovel -> ThreadId(targetId.toInt())
            LocalFavoriteRepository.FavoriteTargetType.TagManga -> null
        }
    }

    private sealed interface RemoteFetchResult {
        data class Success(
            val items: LinkedHashMap<ThreadId, RemoteFavoriteItem>,
            val currentPage: Int,
            val totalPages: Int?,
        ) : RemoteFetchResult

        data class Failure(val reason: String) : RemoteFetchResult
    }

    private data class RemoteFavoriteItem(
        val threadId: ThreadId,
        val favoriteId: FavoriteId,
        val title: String,
    )
}
