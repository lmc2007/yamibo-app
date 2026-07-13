package me.thenano.yamibo.yamibo_app.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import me.thenano.yamibo.yamibo_app.Database

class ReadHistorySqlTest {
    @Test
    fun catalogOriginThreadHistoryIsRestorableButHiddenFromCombinedQueries() {
        val db = inMemoryDatabase()
        val queries = db.readingHistoryQueries

        queries.upsert(
            threadId = 100,
            threadType = "Normal",
            threadName = "Direct",
            threadCover = null,
            forumName = "Forum",
            forumId = 10,
            authorId = 0,
            page = 1,
            postId = 101,
            postTitle = "Direct post",
            anchorPostId = 101,
            anchorPostRatio = null,
            anchorBlockId = null,
            anchorBlockType = null,
            anchorBlockRatio = null,
            globalScrollY = null,
            viewportHeight = null,
            firstVisibleItemIndex = null,
            firstVisibleItemOffset = null,
            historyOrigin = "Direct",
            lastVisitTime = 1_000,
            lastUpdatedTime = null,
        )
        queries.upsert(
            threadId = 100,
            threadType = "Normal",
            threadName = "Catalog",
            threadCover = null,
            forumName = "Forum",
            forumId = 10,
            authorId = 0,
            page = 3,
            postId = 103,
            postTitle = "Catalog post",
            anchorPostId = 103,
            anchorPostRatio = 0.5,
            anchorBlockId = "block-3",
            anchorBlockType = "Text",
            anchorBlockRatio = 0.25,
            globalScrollY = null,
            viewportHeight = 900,
            firstVisibleItemIndex = 7,
            firstVisibleItemOffset = 80,
            historyOrigin = "RssCatalog",
            lastVisitTime = 2_000,
            lastUpdatedTime = null,
        )

        assertEquals(1, queries.countAll().executeAsOne())
        assertEquals("Direct", queries.getPage(10, 0).executeAsList().single().threadName)

        val latest = queries.getByThreadKey(
            threadId = 100,
            threadType = "Normal",
            authorId = 0,
        ).executeAsOne()
        assertEquals("Catalog", latest.threadName)
        assertEquals("RssCatalog", latest.historyOrigin)
        assertEquals(7L, latest.firstVisibleItemIndex)
    }

    @Test
    fun catalogHistoryKeepsThreadAnchorFields() {
        val db = inMemoryDatabase()

        db.rssCatalogReadingHistoryQueries.upsert(
            subscriptionId = 5,
            subscriptionTitle = "姐妹",
            subscriptionQuery = "姐妹",
            subscriptionPage = 1,
            threadId = 571444,
            threadTitle = "Thread",
            threadPage = 2,
            postId = 41543364,
            postTitle = "Post",
            authorId = null,
            anchorPostId = 41543364,
            anchorPostRatio = 0.4,
            anchorBlockId = "anchor",
            anchorBlockType = "Text",
            anchorBlockRatio = 0.3,
            viewportHeight = 1200,
            firstVisibleItemIndex = 9,
            firstVisibleItemOffset = 64,
            lastVisitTime = 3_000,
            coverUrl = null,
        )

        val history = db.rssCatalogReadingHistoryQueries.getBySubscriptionId(5).executeAsOne()
        assertEquals(41543364L, history.anchorPostId)
        assertEquals("anchor", history.anchorBlockId)
        assertEquals(9L, history.firstVisibleItemIndex)
        assertEquals(64L, history.firstVisibleItemOffset)
    }

    private fun inMemoryDatabase(): Database {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        return Database(driver)
    }
}
