package me.thenano.yamibo.yamibo_app.repository.localnovel

import me.thenano.yamibo.yamibo_app.repository.LocalNovelChapterInfo
import java.nio.ByteBuffer
import java.nio.charset.*

class TxtFileParser(
    private val fileOps: PlatformFileOperations,
) {
    data class TxtParseResult(
        val encoding: String,
        val totalChars: Long,
        val chapters: List<LocalNovelChapterInfo>,
    )

    companion object {
        private val CANDIDATE_ENCODINGS = listOf("UTF-8", "GBK", "GB18030", "Big5")

        /** Unicode range for CJK Unified Ideographs (U+4E00–U+9FFF) */
        private fun isCjkChar(c: Char): Boolean = c in '\u4E00'..'\u9FFF'

        // ---- Chapter detection patterns adapted from 阅读 (YueDu) app ----
        // Each pattern is tested against individual lines; the first match for a line wins.

        private val CN_NUM = "[\\d〇零一二两三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]"
        private val PREFIX = "(?:序章|楔子|正文(?!完|结)|终章|后记|尾声|番外|简介|文案|前言)"

        private val CHAPTER_PATTERNS = listOf(
            // 1. 第X章/节/卷/部/篇 (standard Chinese chapter)
            Regex("""^\s*第\s*${CN_NUM}+\s*(?:[章節节卷部篇回])\s*.{0,40}$"""),
            // 2. 序章/楔子/终章/后记/尾声/番外/简介/文案/前言 (special labels)
            Regex("""^\s*(?:${PREFIX})\s*.{0,40}$"""),
            // 3. 第X章 - with bracket prefix 【第一章】
            Regex("""^\s*[【〔〖「『〈［\[]\s*第\s*${CN_NUM}+\s*[章節节卷].{0,30}$"""),
            // 4. 正文 + space + title
            Regex("""^\s*正文\s+.{1,30}$"""),
            // 5. 卷X / 章X (prefix without 第)
            Regex("""^\s*[卷章]\s*${CN_NUM}+\s*.{0,30}$"""),
            // 6. Chapter/Section/Part/Episode + number (English)
            Regex("""^\s*(?:[Cc]hapter|[Ss]ection|[Pp]art|[Ee]pisode)\s*\d{1,4}.{0,30}$"""),
            // 7. Number with delimiter: 1、标题 / 1. 标题 / 1,标题 / 1:标题
            Regex("""^\s*\d{1,5}\s*[：:,.，、_.—\-]\s*.{1,40}$"""),
            // 8. Chinese number with delimiter: 一、标题
            Regex("""^\s*[零一二两三四五六七八九十百千万]{1,8}章?\s*[、_—\-]\s*.{1,40}$"""),
            // 9. Special symbol prefix: ☆标题 / ★标题
            Regex("""^\s*[☆★✦✧].{1,40}$"""),
            // 10. 分节阅读 / 分页阅读 / 第一页
            Regex("""^\s*(?:.{0,10}分[页节章段]阅读|第\s*${CN_NUM}{1,6}\s*[页节]).{0,30}$"""),
            // 11. Book title with bracketed number: 标题(12)
            Regex("""^\s*.{1,20}\s*[(（]\s*${CN_NUM}{1,8}\s*[)）]\s*$"""),
            // 12. Generic: number or CN number at line start followed by text (20 chars max, for simple numbered chapters)
            Regex("""^\s*\d{1,5}\s*$"""),
            Regex("""^\s*[零一二两三四五六七八九十百千万]{1,10}\s*$"""),
        )
    }

    fun parse(fileUri: String, novelId: Long = 0): TxtParseResult {
        val bytes = fileOps.readFileBytes(fileUri)
        val (encoding, text) = detectAndDecode(bytes)
        val chapters = splitChapters(text, novelId)
        return TxtParseResult(
            encoding = encoding,
            totalChars = text.length.toLong(),
            chapters = chapters,
        )
    }

    fun readChapterText(fileUri: String, encoding: String, startOffset: Long, endOffset: Long): String {
        val text = fileOps.readFileText(fileUri, encoding)
        val safeStart = startOffset.coerceIn(0, text.length.toLong())
        val safeEnd = endOffset.coerceIn(safeStart, text.length.toLong())
        return text.substring(safeStart.toInt(), safeEnd.toInt())
    }

    // ---- encoding detection ----

    private fun detectAndDecode(bytes: ByteArray): Pair<String, String> {
        // 1. BOM detection
        val bomEncoding = detectBom(bytes)
        if (bomEncoding != null) {
            val bomOffset = when (bomEncoding) {
                "UTF-8" -> 3
                "UTF-16LE", "UTF-16BE" -> 2
                else -> 0
            }
            return bomEncoding to String(bytes, bomOffset, bytes.size - bomOffset, charset(bomEncoding))
        }

        // 2. Strict decoding (REPORT on malformed input)
        for (enc in CANDIDATE_ENCODINGS) {
            val decoded = tryStrictDecode(bytes, enc)
            if (decoded != null) return enc to decoded
        }

        // 3. Heuristic: pick encoding with highest CJK character ratio
        val best = heuristicDetect(bytes)
        return best.first to best.second
    }

    private fun detectBom(bytes: ByteArray): String? {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte())
            return "UTF-8"
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte())
            return "UTF-16LE"
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte())
            return "UTF-16BE"
        return null
    }

    private fun tryStrictDecode(bytes: ByteArray, encoding: String): String? {
        return try {
            val cs = charset(encoding)
            val decoder = cs.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val charBuffer = decoder.decode(ByteBuffer.wrap(bytes))
            charBuffer.toString()
        } catch (_: CharacterCodingException) {
            null
        }
    }

    private fun heuristicDetect(bytes: ByteArray): Pair<String, String> {
        var bestEncoding = "UTF-8"
        var bestText = ""
        var bestScore = -1.0

        for (enc in CANDIDATE_ENCODINGS) {
            val text = try {
                bytes.toString(charset(enc))
            } catch (_: Exception) {
                continue
            }
            val cjkCount = text.count { isCjkChar(it) }
            val totalChars = text.length.coerceAtLeast(1)
            val score = cjkCount.toDouble() / totalChars
            if (score > bestScore) {
                bestScore = score
                bestText = text
                bestEncoding = enc
            }
        }

        // If all encodings produce near-zero CJK, just use UTF-8 leniently
        if (bestScore < 0.01) {
            bestEncoding = "UTF-8"
            bestText = bytes.toString(Charsets.UTF_8)
        }

        return bestEncoding to bestText
    }

    // ---- chapter splitting ----

    private fun splitChapters(text: String, novelId: Long): List<LocalNovelChapterInfo> {
        // Split text into lines with their offsets
        val lines = text.split("\n")
        // Find lines that match chapter patterns
        val chapterLineIndices = mutableListOf<Int>()

        for ((i, line) in lines.withIndex()) {
            if (line.length > 50) continue // chapter titles are never very long
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            for (pattern in CHAPTER_PATTERNS) {
                if (pattern.matches(trimmed)) {
                    chapterLineIndices.add(i)
                    break
                }
            }
        }

        if (chapterLineIndices.isEmpty()) {
            return listOf(
                LocalNovelChapterInfo(
                    novelId = novelId, title = "全文", chapterIndex = 0,
                    startOffset = 0, endOffset = text.length.toLong(), internalPath = "",
                )
            )
        }

        // Build chapter boundaries based on line offsets
        val chapterStarts = chapterLineIndices.map { idx ->
            // Calculate the byte/char offset of this line's start
            var offset = 0L
            for (j in 0 until idx) {
                offset += lines[j].length + 1 // +1 for the \n
            }
            offset
        }

        val chapters = mutableListOf<LocalNovelChapterInfo>()
        var chapterIndex = 0

        // Text before first chapter
        val firstStart = chapterStarts.first()
        val preText = text.substring(0, firstStart.toInt()).trim()
        if (preText.isNotEmpty() && preText.length > 50) {
            chapters.add(
                LocalNovelChapterInfo(
                    novelId = novelId, title = "前言", chapterIndex = chapterIndex++,
                    startOffset = 0, endOffset = firstStart, internalPath = "",
                )
            )
        }

        for (i in chapterLineIndices.indices) {
            val title = lines[chapterLineIndices[i]].trim().take(80)
            val bodyStart = chapterStarts[i]
            val bodyEnd = if (i + 1 < chapterStarts.size) {
                chapterStarts[i + 1]
            } else {
                text.length.toLong()
            }

            if (bodyEnd > bodyStart) {
                chapters.add(
                    LocalNovelChapterInfo(
                        novelId = novelId, title = title, chapterIndex = chapterIndex++,
                        startOffset = bodyStart, endOffset = bodyEnd, internalPath = "",
                    )
                )
            }
        }

        if (chapters.isEmpty()) {
            chapters.add(
                LocalNovelChapterInfo(
                    novelId = novelId, title = "全文", chapterIndex = 0,
                    startOffset = 0, endOffset = text.length.toLong(), internalPath = "",
                )
            )
        }

        return chapters
    }
}
