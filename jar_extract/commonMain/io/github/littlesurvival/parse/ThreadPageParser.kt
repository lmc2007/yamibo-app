package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.*
import io.github.littlesurvival.dto.page.*
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.parse.util.ParseUtils

class ThreadPageParser : Parser<ThreadPage> {

    override suspend fun parse(html: String): ParseResult<ThreadPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            // Thread info
            val viewTit = doc.selectFirst(".view_tit")
            val categoryTag = viewTit?.selectFirst("em")?.text()?.trim()?.ifEmpty { null }
            val fullTitle = viewTit?.text()?.trim() ?: ""
            val title =
                if (categoryTag != null) {
                    fullTitle.removePrefix(categoryTag).trim()
                } else {
                    fullTitle
                }

            val canonicalUrl = doc.selectFirst("link[rel=canonical]")?.attr("href") ?: ""
            val tid =
                ParseUtils.extractTid(canonicalUrl)
                    ?: doc.selectFirst(".plc")
                        ?.attr("id")
                        ?.removePrefix("pid")
                        ?.toIntOrNull()
                        ?.let { ThreadId(it) }
                    ?: ThreadId(0)

            val forumLink = doc.selectFirst(".header h2 a")
            val forumName = forumLink?.text()?.trim() ?: ""
            val forumUrl = forumLink?.attr("href") ?: ""
            val forumFid = ParseUtils.extractFid(forumUrl) ?: ForumId(0)

            // Total views and replies (from first post's metadata)
            val firstPostMtime = doc.selectFirst(".plc .mtime .y")
            val viewReplyEms = firstPostMtime?.select("em")
            val totalViews = viewReplyEms?.getOrNull(0)?.text()?.trim()?.toIntOrNull()
            val totalReplies = viewReplyEms?.getOrNull(1)?.text()?.trim()?.toIntOrNull()

            // Reply URL
            val replyUrl = doc.selectFirst("a.viewt-reply")?.attr("href")?.ifEmpty { null }

            val threadInfo =
                ThreadInfo(
                    tid = tid,
                    title = title,
                    forum = ForumSummary(fid = forumFid, name = forumName, url = forumUrl),
                    categoryTag = categoryTag,
                    totalReplies = totalReplies,
                    totalViews = totalViews,
                    replyUrl = replyUrl
                )

            // Posts
            val posts = mutableListOf<Post>()
            val postEls = doc.select(".plc")
            for (postEl in postEls) {
                val pidStr = postEl.attr("id").removePrefix("pid")
                val pid = PostId(pidStr.toIntOrNull() ?: continue)

                val floorText = postEl.selectFirst(".authi .mtit .y")?.text()?.trim() ?: ""
                val floor =
                    floorText.replace("#", "").replace(WHITESPACE_RE, "").toIntOrNull()
                        ?: continue

                val authorEl = postEl.selectFirst(".authi .z a")
                val authorUid =
                    authorEl?.attr("href")?.let { ParseUtils.extractUid(it) } ?: UserId(0)
                val authorName = authorEl?.text()?.trim() ?: ""
                val avatarUrl = postEl.selectFirst(".avatar img")?.attr("src")?.ifEmpty { null }
                val author = User(uid = authorUid, name = authorName, avatarUrl = avatarUrl)

                val timeEl = postEl.selectFirst(".authi .mtime")
                val timeText = timeEl?.ownText()?.trim() ?: timeEl?.text()?.trim() ?: ""

                val messageEl = postEl.selectFirst(".message")
                val pstatus = messageEl?.selectFirst(".pstatus")
                val editedText = pstatus?.text()?.trim()?.ifEmpty { null }

                // Post title extraction — operates
                // directly on the live DOM, no clone.
                val postTitle = extractPostTitle(messageEl)

                // Tags extraction (only for first floor)
                val tags = ParseTag().parseFromElement(postEl, messageEl)

                // Poll extraction
                var poll: Poll? = null
                val pollEl = messageEl?.selectFirst(".poll")
                if (pollEl != null && floor == 1) {
                    var pollType = PollType.SingleChoice
                    var pollInfoStr = ""
                    var endTimeStr = ""

                    pollEl.select(".poll_txt").forEach { pt ->
                        val text = pt.text().replace("\\s+".toRegex(), " ").trim()
                        if (text.contains("距结束还有:")) {
                            endTimeStr = text
                        } else {
                            pollInfoStr = text
                            if (text.contains("多选投票")) {
                                pollType = PollType.MultipleChoice
                            }
                        }
                    }

                    val pollBox = pollEl.selectFirst(".poll_box")
                    val isVoted = pollBox?.selectFirst("span.xi1")?.text()?.contains("您已经投过票") == true
                    val status = if (isVoted) PollStatus.Voted else PollStatus.NotVoted

                    val options = mutableListOf<PollOption>()
                    pollBox?.select("p")?.forEach { p ->
                        val label = p.selectFirst("label")
                        val optionNameStr = label?.text()?.trim() ?: ""

                        val input = p.selectFirst("input")
                        val optionIdStr = input?.attr("value")?.ifEmpty { null }
                        val optionId = optionIdStr?.toIntOrNull() ?: 0

                        var percentage: Float? = null
                        var totalVoted: Int? = null

                        val em = p.selectFirst("em")
                        if (em != null) {
                            val emText = em.text().trim() // e.g., "35.36% (64票)"
                            val percStr = emText.substringBefore("%").trim()
                            percentage = percStr.toFloatOrNull()

                            val voteCountStr = emText.substringAfter("(").substringBefore("票").trim()
                            totalVoted = voteCountStr.toIntOrNull()
                        }

                        if (optionNameStr.isNotEmpty()) {
                            options.add(
                                PollOption(
                                    option = PollOptionId(optionId),
                                    optionName = optionNameStr,
                                    percentage = percentage,
                                    totalVoted = totalVoted
                                )
                            )
                        }
                    }

                    poll = Poll(
                        status = status,
                        type = pollType,
                        endTime = endTimeStr,
                        pollInfo = pollInfoStr,
                        option = options
                    )
                }

                // Build contentHtml by uncovering the actual text
                // from inside .postmessage wrapper (if present).
                messageEl?.selectFirst("div[id^=postmessage_]")?.unwrap()

                // Remove .pstatus and .poll in-place.
                pstatus?.remove()
                pollEl?.remove()

                val contentHtml = messageEl?.html()?.trim() ?: ""

                val images = mutableListOf<PostImage>()
                val imgEls = messageEl?.select("img") ?: emptyList()
                for (img in imgEls) {
                    val src = img.attr("src")
                    if (src.isEmpty()) continue
                    if (src.contains("static/image/smiley") || src.contains("static/image/common")
                    ) {
                        continue
                    }
                    val alt = img.attr("alt").ifEmpty { null }
                    images.add(PostImage(url = src, alt = alt))
                }

                // Attachments
                val attachments = mutableListOf<Attachment>()
                val attachmentList = postEl.select("ul.post_attlist li.b_t.p5")
                for (attachmentItem in attachmentList) {
                    val aTag = attachmentItem.selectFirst("a") ?: continue
                    val url = aTag.attr("href")
                    val name = attachmentItem.selectFirst(".tit .link")?.text()?.trim() ?: ""

                    val pTags = attachmentItem.select(".tit p.pl5.f_9")
                    val timeUpload = pTags.first()?.text()?.substringBefore("上传")?.trim() ?: ""

                    val sizeAndDownloadsText = pTags.last()?.text()?.trim() ?: ""
                    val fileSize = sizeAndDownloadsText.substringBefore(",").trim()
                    val downloadTimes =
                        sizeAndDownloadsText
                            .substringAfter("下载次数: ")
                            .substringBefore("次")
                            .toIntOrNull()
                            ?: 0

                    if (name.isNotEmpty() && url.isNotEmpty()) {
                        attachments.add(
                            Attachment(
                                name = name,
                                url = url,
                                timeUpload = timeUpload,
                                fileSize = fileSize,
                                downloadTimes = downloadTimes
                            )
                        )
                    }
                }

                // Comments — search within sibling scope,
                // not entire document.
                val comments = mutableListOf<PostComment>()
                val commentContainer =
                    postEl.selectFirst("#comment_$pidStr")
                        ?: postEl.parent()?.selectFirst("#comment_$pidStr")
                if (commentContainer != null) {
                    val commentEls = commentContainer.select("[id^=commentdetail_]")
                    for (commentEl in commentEls) {
                        val commentAuthorEl = commentEl.selectFirst(".authi .z a")
                        val commentAuthorName = commentAuthorEl?.text()?.trim() ?: ""
                        val commentAuthorUid =
                            commentAuthorEl?.attr("href")?.let { ParseUtils.extractUid(it) }
                                ?: UserId(0)
                        val commentAvatarUrl =
                            commentEl
                                .selectFirst(".avatar img, .user_avatar")
                                ?.attr("src")
                                ?.ifEmpty { null }
                        val commentUser =
                            User(
                                uid = commentAuthorUid,
                                name = commentAuthorName,
                                avatarUrl = commentAvatarUrl
                            )
                        val commentTimeText = commentEl.selectFirst(".mtime")?.text()?.trim() ?: ""
                        val commentMessage = commentEl.selectFirst(".mtxt")?.text()?.trim() ?: ""
                        if (commentMessage.isNotEmpty()) {
                            comments.add(
                                PostComment(
                                    user = commentUser,
                                    timeText = commentTimeText,
                                    message = commentMessage
                                )
                            )
                        }
                    }
                }

                // Rates — search within sibling scope,
                // not entire document.
                val rates = mutableListOf<PostRate>()
                var rateParticipatePeople = 0
                var rateTotalScore = 0
                val rateContainer =
                    postEl.selectFirst("#ratelog_$pidStr")
                        ?: postEl.parent()?.selectFirst("#ratelog_$pidStr")
                if (rateContainer != null) {
                    val rateItems = rateContainer.select("li.flex-box.mli.p0")
                    for (rateItem in rateItems) {
                        // Skip header row and footer row
                        val headerCheck = rateItem.select(".xw1").text()
                        if (headerCheck.contains("参与人数") || headerCheck.contains("理由")) {
                            if (headerCheck.contains("参与人数")) {
                                val spans = rateItem.select("span.xi1")
                                rateParticipatePeople =
                                    spans.getOrNull(0)
                                        ?.text()
                                        ?.replace(NON_DIGIT_RE, "")
                                        ?.toIntOrNull()
                                        ?: 0
                                rateTotalScore =
                                    spans.getOrNull(1)
                                        ?.text()
                                        ?.replace(NON_DIGIT_SIGN_RE, "")
                                        ?.toIntOrNull()
                                        ?: 0
                            }
                            continue
                        }
                        if (rateItem.selectFirst(".dialog") != null) continue

                        val rateUserName =
                            rateItem.selectFirst(".flex-2 a")?.text()?.trim() ?: continue
                        val rateScoreText =
                            rateItem.selectFirst(".xi1")?.text()?.trim()?.ifEmpty {
                                rateItem.selectFirst(".flex-2.xs1.xi1")?.text()?.trim() ?: ""
                            }
                                ?: ""
                        val rateScore =
                            rateScoreText.replace(NON_DIGIT_MINUS_RE, "").toIntOrNull() ?: 0
                        val rateReason =
                            rateItem.selectFirst(".flex-3")?.text()?.trim()?.ifEmpty { "" }
                                ?: ""
                        rates.add(
                            PostRate(
                                userName = rateUserName,
                                score = rateScore,
                                reason = rateReason
                            )
                        )
                    }
                }

                posts.add(
                    Post(
                        pid = pid,
                        floor = floor,
                        title = postTitle,
                        author = author,
                        timeText = timeText,
                        editedText = editedText,
                        contentHtml = contentHtml,
                        images = images,
                        tags = tags,
                        poll = poll,
                        attachments = attachments,
                        comments = comments,
                        rateBlock =
                            RateBlock(
                                rates = rates,
                                rateParticipatePeople = rateParticipatePeople,
                                rateTotalScore = rateTotalScore
                            )
                    )
                )
            }

            // --- Pagination ---
            val pageNav = ParseUtils.parsePageNav(doc)

            ParseResult.Success(ThreadPage(thread = threadInfo, posts = posts, pageNav = pageNav))
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse thread page", e)
        }
    }

    class ParseTag : Parser<Tags> {

        override suspend fun parse(html: String): ParseResult<Tags> {
            return try {
                val doc = Ksoup.parse(html)
                ParseResult.Success(parseFromElement(doc, doc))
            } catch (e: Exception) {
                ParseResult.Failure("Failed to parse tags", e)
            }
        }

        fun parseFromElement(postEl: Element?, messageEl: Element?): Tags {
            if (postEl == null && messageEl == null) return Tags()

            val tagElements = buildList {
                postEl?.select(TAG_LINK_SELECTOR)?.let(::addAll)
                messageEl?.select(TAG_LINK_SELECTOR)?.let(::addAll)
            }

            val tags = tagElements
                .asSequence()
                .mapNotNull(::toTagValue)
                .distinctBy { it.id }
                .toList()

            return Tags(value = tags)
        }

        private fun toTagValue(a: Element): TagValue? {
            val href = a.attr("href")
            val id = extractTagId(href) ?: return null

            val name = extractTagName(a)
            if (name.isBlank()) return null

            return TagValue(
                id = id,
                name = name
            )
        }

        private fun extractTagName(a: Element): String {
            return a.text()
                .orEmpty()
                .replace('\u00A0', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
                .removePrefix(",")
                .trim()
        }

        private fun extractTagId(url: String): TagId? {
            return TAG_ID_REGEX
                .find(url)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?.let(::TagId)
        }

        private companion object {
            const val TAG_LINK_SELECTOR = """a[href*="mod=tag"]"""
            val TAG_ID_REGEX = Regex("""(?:\?|&)id=(\d+)""")
        }
    }

    companion object {
        // Pre-compiled regex — avoids recompilation per call.
        private val WHITESPACE_RE = Regex("\\s")
        private val NON_DIGIT_RE = Regex("\\D")
        private val NON_DIGIT_SIGN_RE = Regex("[^\\d\\-+]")
        private val NON_DIGIT_MINUS_RE = Regex("[^\\d\\-]")

        // Regex for text that is only decorative symbols
        private val DECORATIVE_ONLY = Regex("^[#&*\\-=~_./|\\\\·•◆◇■□▲△○●★☆]+$")

        // Regex for chapter number (e.g. "1", "42", "A44")
        private val CHAPTER_NUMBER = Regex("^[A-Za-z]?\\d{1,4}$")

        // Inline tags that are content wrappers
        private val INLINE_WRAPPERS = setOf("span", "font", "div", "p")

        // Tags to skip during title scanning
        private val SKIP_CLASSES = setOf("pstatus", "showcollapse_box", "quote")

        /**
         * Extract post title directly from the live message element — no clone/removal needed.
         *
         * Walks top-level children, skipping .pstatus, .showcollapse_box, .quote, and <br> tags.
         * Single-pass: checks <strong>/<h2> first, then collects fragments for chapter pattern.
         */
        internal fun extractPostTitle(messageEl: Element?): String {
            if (messageEl == null) return ""

            // Single pass: collect fragments while
            // also checking for <strong>/<h2> title.
            val fragments = mutableListOf<String>()
            var foundStrongTitle: String? = null

            scanForTitle(
                messageEl,
                fragments,
                maxFragments = 6,
                onStrongFound = { title ->
                    if (foundStrongTitle == null) {
                        foundStrongTitle = title
                    }
                }
            )

            // <strong>/<h2> title takes priority
            foundStrongTitle?.let {
                return it
            }

            if (fragments.isEmpty()) return ""

            // Detect "#N#" chapter pattern
            detectChapterPattern(fragments)?.let {
                return it
            }

            // Fall back to first meaningful fragment
            return fragments.firstOrNull { isMeaningfulTitle(it) } ?: ""
        }

        /**
         * Single-pass scan of element children. Skips .pstatus, .showcollapse_box, .quote. Reports
         * <strong>/<h2> hits via callback. Collects text fragments for pattern matching.
         */
        private fun scanForTitle(
            el: Element,
            fragments: MutableList<String>,
            maxFragments: Int,
            onStrongFound: (String) -> Unit,
            strongSearchActive: Boolean = true
        ) {
            for (node in el.childNodes()) {
                if (fragments.size >= maxFragments) return

                // Skip <br>
                if (node is Element && node.tagName() == "br") continue

                // Skip noise elements by class
                if (node is Element && shouldSkipElement(node)) continue

                if (node is TextNode) {
                    val t = node.text().trim()
                    if (t.isEmpty()) continue
                    fragments.add(t)
                    // If we hit meaningful text before
                    // finding <strong>, stop looking.
                    // (decorative text like "#" is ok)
                    continue
                }

                if (node is Element) {
                    val tag = node.tagName()

                    // Check for <strong> / <h2> title
                    if (strongSearchActive) {
                        if (tag == "strong" || tag == "h2") {
                            val t = node.text().trim()
                            if (isMeaningfulTitle(t)) {
                                onStrongFound(t)
                                return
                            }
                        }
                        // Check nested <strong>/<h2>
                        val strong = node.selectFirst("strong, h2")
                        if (strong != null) {
                            val t = strong.text().trim()
                            if (isMeaningfulTitle(t)) {
                                onStrongFound(t)
                                return
                            }
                        }
                    }

                    // For inline wrappers, recurse into
                    // children for fragment collection
                    if (tag in INLINE_WRAPPERS) {
                        scanForTitle(
                            node,
                            fragments,
                            maxFragments,
                            onStrongFound,
                            strongSearchActive = strongSearchActive
                        )
                    } else {
                        val t = node.text().trim()
                        if (t.isNotEmpty()) fragments.add(t)
                    }
                }
            }
        }

        /** Check if an element should be skipped */
        private fun shouldSkipElement(el: Element): Boolean {
            val cls = el.className()
            if (cls.isEmpty()) return false
            return SKIP_CLASSES.any { cls.contains(it) }
        }

        /** Detect "#N#" or "#N" chapter patterns */
        private fun detectChapterPattern(fragments: List<String>): String? {
            if (fragments.size < 2) return null
            if (fragments.size >= 3 &&
                DECORATIVE_ONLY.matches(fragments[0]) &&
                CHAPTER_NUMBER.matches(fragments[1]) &&
                DECORATIVE_ONLY.matches(fragments[2])
            ) {
                return fragments[1]
            }
            if (DECORATIVE_ONLY.matches(fragments[0]) && CHAPTER_NUMBER.matches(fragments[1])) {
                return fragments[1]
            }
            return null
        }

        private fun Char.isCJK(): Boolean {
            val code = this.code
            return code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF || code in 0xF900..0xFAFF
        }

        /** Check if text is meaningful enough to be a title */
        private fun isMeaningfulTitle(text: String): Boolean {
            if (text.isEmpty()) return false
            if (DECORATIVE_ONLY.matches(text)) return false
            if (text.length == 1) return text[0].isCJK()
            return true
        }
    }
}
