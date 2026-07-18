package me.thenano.yamibo.yamibo_app.repository.localnovel

import com.fleeksoft.ksoup.Ksoup
import me.thenano.yamibo.yamibo_app.repository.LocalNovelChapterInfo

class EpubFileParser(
    private val fileOps: PlatformFileOperations,
) {
    data class EpubMetadata(
        val title: String,
        val author: String,
        val coverInternalPath: String?,
    )

    data class EpubParseResult(
        val metadata: EpubMetadata,
        val chapters: List<LocalNovelChapterInfo>,
        val extractDir: String,
    )

    companion object {
        private const val CONTAINER_PATH = "META-INF/container.xml"
    }

    fun parse(fileUri: String, extractDir: String, novelId: Long = 0): EpubParseResult {
        fileOps.extractZipToDir(fileUri, extractDir)

        // Parse container.xml
        val containerPath = "$extractDir/$CONTAINER_PATH"
        val opfRelativePath = if (fileOps.localFileExists(containerPath)) {
            val containerDoc = Ksoup.parse(fileOps.readLocalFileText(containerPath))
            containerDoc.getElementsByTag("rootfile").firstOrNull()?.attr("full-path")
                ?: throw IllegalStateException("Invalid EPUB: cannot find OPF path in container.xml")
        } else {
            throw IllegalStateException("Invalid EPUB: container.xml not found")
        }

        val opfDir = opfRelativePath.substringBeforeLast("/", "")
        val opfPath = "$extractDir/$opfRelativePath"
        if (!fileOps.localFileExists(opfPath)) {
            throw IllegalStateException("Invalid EPUB: OPF file not found: $opfRelativePath")
        }

        val opfDoc = Ksoup.parse(fileOps.readLocalFileText(opfPath))

        // Parse metadata — ksoup parses XML as HTML, so namespaced tags like dc:title
        // become tags with colon in their name
        val title = opfDoc.getElementsByTag("dc:title").firstOrNull()?.text()
            ?: opfDoc.getElementsByTag("title").firstOrNull()?.text()
            ?: "Unknown Title"

        val author = opfDoc.getElementsByTag("dc:creator").firstOrNull()?.text()
            ?: opfDoc.getElementsByTag("creator").firstOrNull()?.text()
            ?: ""

        // Parse cover image
        val coverId = opfDoc.getElementsByTag("meta").firstOrNull { it.attr("name") == "cover" }?.attr("content")
        val manifestItems = opfDoc.getElementsByTag("manifest").firstOrNull()
            ?.getElementsByTag("item")
            ?.associateBy { it.attr("id") }
            ?: emptyMap()

        val coverHref: String? = if (!coverId.isNullOrEmpty()) {
            manifestItems[coverId]?.attr("href")
        } else {
            // Try to find cover by properties attribute
            opfDoc.getElementsByTag("item").firstOrNull { it.attr("properties") == "cover-image" }?.attr("href")
                ?: opfDoc.getElementsByTag("item").firstOrNull {
                    it.attr("media-type")?.startsWith("image/") == true
                }?.attr("href")
        }

        val coverPath = if (coverHref != null) {
            resolvePath(extractDir, opfDir, coverHref)
        } else null

        // Parse spine (chapter order)
        val spineElement = opfDoc.getElementsByTag("spine").firstOrNull()
        val spineItemRefs = spineElement?.getElementsByTag("itemref") ?: emptyList()

        val chapters = mutableListOf<LocalNovelChapterInfo>()
        var chapterIndex = 0

        for (itemref in spineItemRefs) {
            val idref = itemref.attr("idref")
            val manifestItem = manifestItems[idref] ?: continue
            val href = manifestItem.attr("href")
            val mediaType = manifestItem.attr("media-type") ?: ""

            if ("xhtml" in mediaType || "html" in mediaType || "xml" in mediaType) {
                val chapterPath = resolvePath(extractDir, opfDir, href)
                val chapterTitle = resolveChapterTitle(chapterPath) ?: "Chapter ${chapterIndex + 1}"

                chapters.add(
                    LocalNovelChapterInfo(
                        novelId = novelId,
                        title = chapterTitle,
                        chapterIndex = chapterIndex++,
                        startOffset = 0,
                        endOffset = 0,
                        internalPath = chapterPath,
                    )
                )
            }
        }

        if (chapters.isEmpty()) {
            throw IllegalStateException("Invalid EPUB: no chapters found")
        }

        return EpubParseResult(
            metadata = EpubMetadata(
                title = title,
                author = author,
                coverInternalPath = coverPath,
            ),
            chapters = chapters,
            extractDir = extractDir,
        )
    }

    fun readChapterHtml(internalPath: String): String {
        return if (fileOps.localFileExists(internalPath)) {
            fileOps.readLocalFileText(internalPath)
        } else ""
    }

    private fun resolvePath(extractDir: String, opfDir: String, href: String): String {
        return if (opfDir.isNotEmpty() && !href.startsWith("/")) {
            "$extractDir/$opfDir/$href"
        } else {
            "$extractDir/$href"
        }.replace("//", "/")
    }

    private fun resolveChapterTitle(internalPath: String): String? {
        if (!fileOps.localFileExists(internalPath)) return null

        val content = fileOps.readLocalFileText(internalPath)
        val doc = Ksoup.parse(content)
        val titleTag = doc.getElementsByTag("title").firstOrNull()
        if (titleTag != null && titleTag.text().isNotBlank()) {
            return titleTag.text().trim()
        }

        for (tag in listOf("h1", "h2", "h3")) {
            val heading = doc.getElementsByTag(tag).firstOrNull()
            if (heading != null && heading.text().isNotBlank()) {
                return heading.text().trim().take(100)
            }
        }

        return null
    }
}
