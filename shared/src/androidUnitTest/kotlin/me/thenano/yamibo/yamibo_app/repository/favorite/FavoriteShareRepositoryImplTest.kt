package me.thenano.yamibo.yamibo_app.repository.favorite

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.ThreadId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.repository.FavoriteShareRepository
import me.thenano.yamibo.yamibo_app.repository.RssSearchSubscriptionRepository

class FavoriteShareRepositoryImplTest {
    @Test
    fun rejectsWrongSchema() {
        runBlocking {
        val repository = repository(inMemoryDatabase(), FakeRssRepository())
        val payload = FavoriteShareRepository.Package(
            schema = "wrong",
            exportedAt = 1L,
            folders = listOf(FavoriteShareRepository.Folder(name = "A", items = emptyList())),
        )

        assertFailsWith<IllegalArgumentException> {
            repository.encode(payload)
        }
        }
    }

    @Test
    fun rejectsUnsupportedVersion() {
        runBlocking {
        val repository = repository(inMemoryDatabase(), FakeRssRepository())
        val payload = FavoriteShareRepository.Package(
            schemaVersion = 999,
            exportedAt = 1L,
            folders = listOf(FavoriteShareRepository.Folder(name = "A", items = emptyList())),
        )

        assertFailsWith<IllegalArgumentException> {
            repository.encode(payload)
        }
        }
    }

    @Test
    fun exportUsesSelectedFoldersAndMergesNestedCollectionItems() = runBlocking {
        val db = inMemoryDatabase()
        val favoriteRepository = FavoriteStoreRepositoryImpl(db)
        val defaultCategory = favoriteRepository.getDefaultCategory()
        val secondCategory = favoriteRepository.createCategory("Second")
        val nested = favoriteRepository.createCollection(defaultCategory.id, "Nested", "brown")
        favoriteRepository.addNormalThreadFavorite(
            tid = ThreadId(1),
            title = "Direct",
            coverUrl = null,
            lastUpdatedTime = null,
            forumId = null,
            forumName = null,
            categoryIds = listOf(defaultCategory.id),
        )
        favoriteRepository.addNormalThreadFavorite(
            tid = ThreadId(2),
            title = "Nested",
            coverUrl = null,
            lastUpdatedTime = null,
            forumId = null,
            forumName = null,
            collectionIds = listOf(nested.id),
        )
        favoriteRepository.addNormalThreadFavorite(
            tid = ThreadId(3),
            title = "Ignored",
            coverUrl = null,
            lastUpdatedTime = null,
            forumId = null,
            forumName = null,
            categoryIds = listOf(secondCategory.id),
        )
        val repository = repository(db, FakeRssRepository())

        val exported = repository.export(
            FavoriteShareRepository.ExportSelection(categoryIds = setOf(defaultCategory.id)),
        )

        assertEquals(listOf(defaultCategory.name), exported.folders.map { it.name })
        assertEquals(setOf(1L, 2L), exported.folders.single().items.map { it.targetId }.toSet())
    }

    @Test
    fun previewCountsDuplicateUnsupportedAndInvalidItems() = runBlocking {
        val db = inMemoryDatabase()
        val favoriteRepository = FavoriteStoreRepositoryImpl(db)
        favoriteRepository.addNormalThreadFavorite(
            tid = ThreadId(1),
            title = "Existing",
            coverUrl = null,
            lastUpdatedTime = null,
            forumId = null,
            forumName = null,
        )
        val repository = repository(db, FakeRssRepository())
        val json = repository.encode(
            FavoriteShareRepository.Package(
                exportedAt = 1L,
                folders = listOf(
                    FavoriteShareRepository.Folder(
                        name = "Shared",
                        items = listOf(
                            item("ThreadNormal", 1, "Existing"),
                            item("UnknownType", 2, "Unsupported"),
                            item("ThreadNormal", 3, ""),
                        ),
                    ),
                ),
            )
        )

        val preview = repository.previewImport(json)

        assertEquals(1, preview.folderCount)
        assertEquals(3, preview.itemCount)
        assertEquals(1, preview.duplicateCount)
        assertEquals(1, preview.unsupportedCount)
        assertEquals(1, preview.invalidCount)
    }

    @Test
    fun previewAcceptsUtf8Bom() = runBlocking {
        val db = inMemoryDatabase()
        val repository = repository(db, FakeRssRepository())
        val json = repository.encode(
            FavoriteShareRepository.Package(
                exportedAt = 1L,
                folders = listOf(
                    FavoriteShareRepository.Folder(
                        name = "Shared",
                        items = emptyList(),
                    ),
                ),
            )
        )

        val preview = repository.previewImport("\uFEFF$json")

        assertEquals(1, preview.folderCount)
        assertEquals(listOf("Shared"), preview.folders.map { it.name })
    }

    @Test
    fun createFoldersSuffixesNameAndReusesExistingItems() = runBlocking {
        val db = inMemoryDatabase()
        val favoriteRepository = FavoriteStoreRepositoryImpl(db)
        favoriteRepository.createCategory("Shared")
        favoriteRepository.addNormalThreadFavorite(
            tid = ThreadId(1),
            title = "Existing",
            coverUrl = null,
            lastUpdatedTime = null,
            forumId = null,
            forumName = null,
        )
        val repository = repository(db, FakeRssRepository())
        val json = repository.encode(
            FavoriteShareRepository.Package(
                exportedAt = 1L,
                folders = listOf(
                    FavoriteShareRepository.Folder(
                        name = "Shared",
                        items = listOf(
                            item("ThreadNormal", 1, "Existing"),
                            item("ThreadNormal", 2, "Missing"),
                        ),
                    ),
                ),
            )
        )

        val result = repository.importFavorites(
            json,
            FavoriteShareRepository.ImportTarget(FavoriteShareRepository.ImportMode.CreateFolders),
        )
        val createdCategory = favoriteRepository.getCategories().single { it.name == "Shared (2)" }
        val createdContent = favoriteRepository.getCategoryContent(createdCategory.id)

        assertEquals(1, result.createdFolderCount)
        assertEquals(1, result.createdItemCount)
        assertEquals(1, result.reusedItemCount)
        assertEquals(setOf(1L, 2L), createdContent.directItems.map { it.targetId }.toSet())
    }

    @Test
    fun addToExistingFoldersSkipsExistingItems() = runBlocking {
        val db = inMemoryDatabase()
        val favoriteRepository = FavoriteStoreRepositoryImpl(db)
        favoriteRepository.getDefaultCategory()
        val targetCategory = favoriteRepository.createCategory("Target")
        favoriteRepository.addNormalThreadFavorite(
            tid = ThreadId(1),
            title = "Existing",
            coverUrl = null,
            lastUpdatedTime = null,
            forumId = null,
            forumName = null,
        )
        val repository = repository(db, FakeRssRepository())
        val json = repository.encode(
            FavoriteShareRepository.Package(
                exportedAt = 1L,
                folders = listOf(
                    FavoriteShareRepository.Folder(
                        name = "Shared",
                        items = listOf(
                            item("ThreadNormal", 1, "Existing"),
                            item("ThreadNormal", 2, "Missing"),
                        ),
                    ),
                ),
            )
        )

        val result = repository.importFavorites(
            json,
            FavoriteShareRepository.ImportTarget(
                mode = FavoriteShareRepository.ImportMode.AddToExistingFolders,
                categoryIds = setOf(targetCategory.id),
            ),
        )
        val targetItems = favoriteRepository.getCategoryContent(targetCategory.id).directItems

        assertEquals(1, result.createdItemCount)
        assertEquals(1, result.skippedDuplicateCount)
        assertEquals(setOf(2L), targetItems.map { it.targetId }.toSet())
    }

    private fun item(type: String, targetId: Long, title: String): FavoriteShareRepository.Item {
        return FavoriteShareRepository.Item(
            targetType = type,
            targetId = targetId,
            title = title,
            forumId = 10,
            forumName = "Forum",
        )
    }

    private fun repository(
        db: Database,
        rssRepository: RssSearchSubscriptionRepository,
    ): FavoriteShareRepositoryImpl {
        return FavoriteShareRepositoryImpl(FavoriteStoreRepositoryImpl(db), rssRepository)
    }

    private fun inMemoryDatabase(): Database {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        return Database(driver)
    }

    private class FakeRssRepository : RssSearchSubscriptionRepository {
        private var nextId = 1L
        private val subscriptionByKey = mutableMapOf<Pair<String, Int?>, RssSearchSubscriptionRepository.SubscriptionSummary>()
        override val subscriptions: StateFlow<List<RssSearchSubscriptionRepository.SubscriptionSummary>> =
            MutableStateFlow(emptyList())
        override val unreadCount: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun createFromSearch(
            title: String,
            query: String,
            forumId: ForumId?,
            forumName: String?,
            searchPage: SearchPage,
        ): YamiboResult<Long> = ensureSubscription(query, forumId, forumName)

        override suspend fun ensureSubscription(
            query: String,
            forumId: ForumId?,
            forumName: String?,
        ): YamiboResult<Long> {
            val key = query.trim().lowercase() to forumId?.value
            subscriptionByKey[key]?.let { return YamiboResult.Success(it.id) }
            val id = nextId++
            subscriptionByKey[key] = RssSearchSubscriptionRepository.SubscriptionSummary(
                id = id,
                title = query,
                query = query,
                forumId = forumId,
                forumName = forumName,
                enabled = true,
                createdAt = 1L,
                updatedAt = 1L,
                lastRefreshStartedAt = null,
                lastRefreshFinishedAt = null,
                lastRefreshStatus = null,
                lastRefreshMessage = null,
                lastTotalCount = 0,
                unreadCount = 0,
            )
            return YamiboResult.Success(id)
        }

        override suspend fun findBySearch(
            query: String,
            forumId: ForumId?,
        ): RssSearchSubscriptionRepository.SubscriptionSummary? {
            return subscriptionByKey[query.trim().lowercase() to forumId?.value]
        }

        override suspend fun getSubscription(subscriptionId: Long): RssSearchSubscriptionRepository.SubscriptionSummary? {
            return subscriptionByKey.values.firstOrNull { it.id == subscriptionId }
        }

        override suspend fun refresh(subscriptionId: Long): YamiboResult<RssSearchSubscriptionRepository.RefreshSummary> =
            error("unused")
        override suspend fun refreshAllEnabled(): List<YamiboResult<RssSearchSubscriptionRepository.RefreshSummary>> =
            error("unused")
        override suspend fun getCatalogPage(subscriptionId: Long, page: Int, pageSize: Int): RssSearchSubscriptionRepository.CatalogPage? =
            error("unused")
        override suspend fun getCachedCatalogPage(subscriptionId: Long, page: Int, pageSize: Int): RssSearchSubscriptionRepository.CatalogPage? =
            error("unused")
        override suspend fun markRead(subscriptionId: Long, threadId: Long) = Unit
        override suspend fun markUnread(subscriptionId: Long, threadId: Long) = Unit
        override suspend fun rename(subscriptionId: Long, title: String) = Unit
        override suspend fun setEnabled(subscriptionId: Long, enabled: Boolean) = Unit
        override suspend fun delete(subscriptionId: Long) = Unit
    }
}
