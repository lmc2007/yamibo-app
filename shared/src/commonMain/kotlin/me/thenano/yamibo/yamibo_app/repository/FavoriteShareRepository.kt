package me.thenano.yamibo.yamibo_app.repository

import kotlinx.serialization.Serializable

interface FavoriteShareRepository {
    data class ExportSelection(
        val categoryIds: Set<Long>,
    )

    enum class ImportMode {
        CreateFolders,
        AddToExistingFolders,
    }

    data class ImportTarget(
        val mode: ImportMode,
        val categoryIds: Set<Long> = emptySet(),
    )

    data class ImportPreview(
        val folderCount: Int,
        val itemCount: Int,
        val duplicateCount: Int,
        val unsupportedCount: Int,
        val invalidCount: Int,
        val folders: List<FolderPreview>,
    ) {
        @Suppress("Unused")
        val importableCount: Int = itemCount - duplicateCount - unsupportedCount - invalidCount
    }

    data class FolderPreview(
        val name: String,
        val itemCount: Int,
        val duplicateCount: Int,
        val unsupportedCount: Int,
        val invalidCount: Int,
    )

    data class ImportResult(
        val createdFolderCount: Int,
        val createdItemCount: Int,
        val reusedItemCount: Int,
        val skippedDuplicateCount: Int,
        val unsupportedCount: Int,
        val invalidCount: Int,
    )

    suspend fun export(selection: ExportSelection): Package
    fun encode(packageData: Package): String
    fun decode(jsonText: String): Package
    suspend fun previewImport(jsonText: String): ImportPreview
    suspend fun importFavorites(jsonText: String, target: ImportTarget): ImportResult

    @Serializable
    data class Package(
        val schema: String = Schema,
        val schemaVersion: Int = SchemaVersion,
        val exportedAt: Long,
        val folders: List<Folder>,
    )

    @Serializable
    data class Folder(
        val name: String,
        val items: List<Item>,
    )

    @Serializable
    data class Item(
        val targetType: String,
        val targetId: Long,
        val authorId: Long? = null,
        val title: String,
        val coverUrl: String? = null,
        val lastUpdatedTime: Long? = null,
        val forumId: Long? = null,
        val forumName: String? = null,
        val rssQuery: String? = null,
        val rssForumId: Long? = null,
        val rssForumName: String? = null,
    )

    @Suppress("ConstPropertyName")
    companion object {
        const val Schema = "yamibo.favorite-share"
        const val SchemaVersion = 2
    }
}
