package me.thenano.yamibo.yamibo_app.repository.forumnovel

import io.github.littlesurvival.dto.page.PostImage
import io.github.littlesurvival.dto.page.ThreadPage
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import me.thenano.yamibo.yamibo_app.repository.DownloadRepository
import me.thenano.yamibo.yamibo_app.repository.download.DownloadedContentGroup
import me.thenano.yamibo.yamibo_app.repository.download.DownloadedContentGroupType
import me.thenano.yamibo.yamibo_app.repository.download.ThreadPageDownloadKey
import me.thenano.yamibo.yamibo_app.repository.download.normalizeDownloadImageUrl
import me.thenano.yamibo.yamibo_app.repository.localnovel.PlatformFileOperations
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

const val FORUM_NOVEL_PACKAGE_TYPE = "forum-novel"
const val FORUM_NOVEL_PACKAGE_FORMAT_VERSION = 1

@Serializable
data class ForumNovelPackageManifest(
    val formatVersion: Int = FORUM_NOVEL_PACKAGE_FORMAT_VERSION,
    val packageType: String = FORUM_NOVEL_PACKAGE_TYPE,
    val tid: Int,
    val authorId: Int? = null,
    val title: String,
    val forumId: Int? = null,
    val forumName: String? = null,
    val sourceTotalPages: Int,
    val exportedAt: Long,
    val pages: List<Int> = emptyList(),
    val missingPages: List<Int> = emptyList(),
    val images: List<ForumNovelPackageImage> = emptyList(),
)

@Serializable
data class ForumNovelPackageImage(
    val sourceUrl: String,
    val fileName: String,
)

sealed interface ForumNovelPackageExportResult {
    data class Success(
        val zipPath: String,
        val title: String,
        val missingPages: List<Int>,
    ) : ForumNovelPackageExportResult

    data class Failure(val message: String) : ForumNovelPackageExportResult
}

sealed interface ForumNovelImportResult {
    data class Success(val entry: ForumNovelShelfEntry) : ForumNovelImportResult
    data class Duplicate(val existing: ForumNovelShelfEntry) : ForumNovelImportResult
    data class Failure(val message: String) : ForumNovelImportResult
}

class ForumNovelPackageExporter(
    private val downloadRepository: DownloadRepository,
    private val packageWriter: ForumNovelPackageWriter,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    },
) {
    suspend fun export(
        group: DownloadedContentGroup,
        zipPath: String,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): ForumNovelPackageExportResult {
        if (group.type != DownloadedContentGroupType.Thread) {
            return ForumNovelPackageExportResult.Failure("只有论坛小说可以打包分享")
        }
        val keys = group.items
            .mapNotNull { it.key as? ThreadPageDownloadKey }
            .sortedBy { it.page }
        if (keys.isEmpty()) {
            return ForumNovelPackageExportResult.Failure("没有已下载的页面")
        }

        val pages = mutableMapOf<Int, String>()
        val images = linkedMapOf<String, ForumNovelPackageImage>()
        val imageBytes = mutableMapOf<String, ByteArray>()
        val missingPages = mutableListOf<Int>()
        val firstKey = keys.first()
        var forumId: Int? = null
        var forumName: String? = null
        var sourceTotalPages = keys.size

        keys.forEachIndexed { index, key ->
            onProgress(index + 1, keys.size)
            val page = downloadRepository.getDownloadedRawPage(key)
            if (page == null) {
                missingPages += key.page
                return@forEachIndexed
            }
            val manifest = downloadRepository.getManifest(key)
            if (manifest != null && forumId == null) {
                forumId = manifest.forumId
                forumName = manifest.forumName
                sourceTotalPages = manifest.sourceTotalPages
            }
            pages[key.page] = json.encodeToString(ThreadPage.serializer(), page)
            manifest?.images.orEmpty().forEach { image ->
                if (images.containsKey(image.fileName)) return@forEach
                images[image.fileName] = ForumNovelPackageImage(image.sourceUrl, image.fileName)
                downloadRepository.getThreadPageImageBytes(key, image.fileName)?.let { bytes ->
                    imageBytes[image.fileName] = bytes
                }
            }
        }

        val packageManifest = ForumNovelPackageManifest(
            tid = firstKey.tid,
            authorId = firstKey.authorId,
            title = group.title,
            forumId = forumId,
            forumName = forumName,
            sourceTotalPages = sourceTotalPages,
            exportedAt = currentTimeMillis(),
            pages = pages.keys.sorted(),
            missingPages = missingPages.sorted(),
            images = images.values.toList(),
        )

        val entries = buildList {
            add(ForumNovelPackageZipEntry("manifest.json", json.encodeToString(packageManifest).encodeToByteArray()))
            pages.forEach { (page, pageJson) ->
                add(ForumNovelPackageZipEntry("pages/$page.json", pageJson.encodeToByteArray()))
            }
            imageBytes.forEach { (fileName, bytes) ->
                add(ForumNovelPackageZipEntry("images/$fileName", bytes))
            }
        }
        packageWriter.writeZip(zipPath, entries)

        return ForumNovelPackageExportResult.Success(
            zipPath = zipPath,
            title = group.title,
            missingPages = missingPages.sorted(),
        )
    }
}

class ForumNovelPackageImporter(
    private val shelfRepository: ForumNovelShelfRepository,
    private val fileOps: PlatformFileOperations,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    },
) {
    suspend fun import(zipUri: String): ForumNovelImportResult {
        val extractDir = "${fileOps.getInternalFilesDir()}/forum_novel_${currentTimeMillis()}"
        return try {
            fileOps.extractZipToDir(zipUri, extractDir)
            val manifestPath = "$extractDir/manifest.json"
            if (!fileOps.localFileExists(manifestPath)) {
                fileOps.deleteDirectory(extractDir)
                return ForumNovelImportResult.Failure("不是有效的论坛小说包")
            }
            val manifest = runCatching {
                json.decodeFromString<ForumNovelPackageManifest>(fileOps.readLocalFileText(manifestPath))
            }.getOrElse { error ->
                fileOps.deleteDirectory(extractDir)
                return ForumNovelImportResult.Failure("无法解析论坛小说包：${error.message}")
            }
            if (manifest.packageType != FORUM_NOVEL_PACKAGE_TYPE) {
                fileOps.deleteDirectory(extractDir)
                return ForumNovelImportResult.Failure("不是论坛小说包")
            }

            val existing = shelfRepository.getByTid(manifest.tid.toLong(), manifest.authorId?.toLong())
            if (existing != null) {
                fileOps.deleteDirectory(extractDir)
                return ForumNovelImportResult.Duplicate(existing)
            }

            val entry = ForumNovelShelfEntry(
                source = ForumNovelShelfSource.Imported,
                tid = manifest.tid.toLong(),
                authorId = manifest.authorId?.toLong(),
                title = manifest.title,
                contentDir = extractDir,
                createdAt = currentTimeMillis(),
            )
            val id = shelfRepository.insert(entry)
            val inserted = shelfRepository.getById(id) ?: entry.copy(id = id)
            ForumNovelImportResult.Success(inserted)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            runCatching { fileOps.deleteDirectory(extractDir) }
            ForumNovelImportResult.Failure(error.message ?: "导入论坛小说失败")
        }
    }
}

/** Rewrites post image URLs in [ThreadPage] using [resolve], keeping HTML as-is otherwise. */
fun ThreadPage.withResolvedImageUrls(resolve: (String) -> String?): ThreadPage = copy(
    posts = posts.map { post ->
        post.copy(
            contentHtml = post.images.fold(post.contentHtml) { html, image ->
                val target = resolve(normalizeDownloadImageUrl(image.url)) ?: return@fold html
                html
                    .replace(image.url, target)
                    .replace(image.url.removePrefix("https://bbs.yamibo.com/"), target)
            },
            images = post.images.map { image ->
                PostImage(
                    url = resolve(normalizeDownloadImageUrl(image.url)) ?: image.url,
                    alt = image.alt,
                )
            },
        )
    },
)
