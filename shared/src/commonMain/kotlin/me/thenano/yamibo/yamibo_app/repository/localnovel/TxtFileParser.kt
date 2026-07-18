package me.thenano.yamibo.yamibo_app.repository.localnovel

import me.thenano.yamibo.yamibo_app.repository.LocalNovelChapterInfo
import java.nio.ByteBuffer
import java.nio.CharBuffer
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
        private val CHAPTER_REGEX = Regex(
            """(?:^|\n)\s*((?:第)[\s]*[0-9零一二三四五六七八九十百千万]+[\s]*[章節节卷])(?:\s+(.*?))?\s*(?:\n|$)"""
        )
        /** Unicode range for CJK Unified Ideographs (U+4E00–U+9FFF) */
        private fun isCjkChar(c: Char): Boolean = c in '\u4E00'..'\u9FFF'
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

    // ---- chapter splitting (unchanged from before) ----

    private fun splitChapters(text: String, novelId: Long): List<LocalNovelChapterInfo> {
        val matches = CHAPTER_REGEX.findAll(text).toList()
        if (matches.isEmpty()) {
            return listOf(
                LocalNovelChapterInfo(
                    novelId = novelId,
                    title = "全文",
                    chapterIndex = 0,
                    startOffset = 0,
                    endOffset = text.length.toLong(),
                    internalPath = "",
                )
            )
        }

        val chapters = mutableListOf<LocalNovelChapterInfo>()
        var chapterIndex = 0

        val firstMatch = matches.first()
        val preText = text.substring(0, firstMatch.range.first).trim()
        if (preText.isNotEmpty() && preText.length > 50) {
            chapters.add(
                LocalNovelChapterInfo(
                    novelId = novelId,
                    title = "前言",
                    chapterIndex = chapterIndex++,
                    startOffset = 0,
                    endOffset = firstMatch.range.first.toLong(),
                    internalPath = "",
                )
            )
        }

        for (i in matches.indices) {
            val match = matches[i]
            val title = buildString {
                append(match.groupValues[1].trim())
                val extra = match.groupValues.getOrNull(2)?.trim()?.take(50)
                if (!extra.isNullOrEmpty()) {
                    append(" ")
                    append(extra)
                }
            }
            val bodyStart = match.range.last + 1
            val bodyEnd = if (i + 1 < matches.size) {
                matches[i + 1].range.first
            } else {
                text.length
            }

            if (bodyEnd > bodyStart) {
                chapters.add(
                    LocalNovelChapterInfo(
                        novelId = novelId,
                        title = title,
                        chapterIndex = chapterIndex++,
                        startOffset = bodyStart.toLong(),
                        endOffset = bodyEnd.toLong(),
                        internalPath = "",
                    )
                )
            }
        }

        if (chapters.isEmpty()) {
            chapters.add(
                LocalNovelChapterInfo(
                    novelId = novelId,
                    title = "全文",
                    chapterIndex = 0,
                    startOffset = 0,
                    endOffset = text.length.toLong(),
                    internalPath = "",
                )
            )
        }

        return chapters
    }
}
