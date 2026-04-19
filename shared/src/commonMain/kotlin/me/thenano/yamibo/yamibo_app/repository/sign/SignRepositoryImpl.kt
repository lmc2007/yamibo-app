package me.thenano.yamibo.yamibo_app.repository.sign

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.fetch.FetchFactory
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.client.statement.bodyAsText
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.repository.AuthRepository
import me.thenano.yamibo.yamibo_app.repository.SignRepository
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.util.time.currentLocalDateKey
import me.thenano.yamibo.yamibo_app.util.time.currentLocalDateKeyAt
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import me.thenano.yamibo.yamiboapp.SignDailyRecord

class SignRepositoryImpl(
    private val db: Database,
    private val authRepository: AuthRepository,
    private val appSettingsRepository: AppSettingsRepository,
) : SignRepository {
    private companion object {
        const val FETCH_TIMEOUT_MILLIS = 60_000L
        const val SIGN_WEBVIEW_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    private val fetcher = FetchFactory(FetchFactory.Device.MOBILE, FETCH_TIMEOUT_MILLIS)
    private val recordQueries = db.signDailyRecordQueries

    override suspend fun fetchPageInfo(): YamiboResult<SignRepository.SignPageInfo> {
        if (!authRepository.isLoggedIn()) return YamiboResult.NotLoggedIn
        applyCookies()
        val cookie = authRepository.cookieStore.load().orEmpty()

        return when (val result = performSignRequest(YamiboRoute.Sign.build(), cookie)) {
            is FetchResult.Success -> {
                if (isCloudflarePage(result.value)) {
                    YamiboResult.Failure("尚未通過簽到頁的 Cloudflare 驗證，請先在 WebView 完成驗證。")
                } else {
                    val info = cacheObservedHtml(result.value) ?: parseSignPage(result.value)
                    YamiboResult.Success(info)
                }
            }

            is FetchResult.Failure.HttpError ->
                YamiboResult.Failure(formatHttpFailure(result.statusCode, result.bodyPreview))

            is FetchResult.Failure.NetworkError ->
                YamiboResult.Failure(result.exception.message ?: "讀取簽到頁時發生網路錯誤", result.exception)

            is FetchResult.Failure.Timeout ->
                YamiboResult.Failure("讀取簽到頁逾時", result.exception)

            is FetchResult.Failure.Unknown ->
                YamiboResult.Failure(result.exception.message ?: "讀取簽到頁失敗", result.exception)
        }
    }

    override suspend fun runAutoSign(allowRepair: Boolean): YamiboResult<SignRepository.ActionResult> {
        val initialInfo = getCachedPageInfo() ?: when (val result = fetchPageInfo()) {
            is YamiboResult.Success -> result.value
            is YamiboResult.NotLoggedIn -> return result
            is YamiboResult.NoPermission -> return result
            is YamiboResult.Maintenance -> return result
            is YamiboResult.Failure -> return result
        }

        var pageInfo = initialInfo
        var repairCount = 0
        var lastMessage = ""
        var finalStatus = if (pageInfo.hasSignedToday) {
            SignRepository.ActionStatus.ALREADY_SIGNED
        } else {
            SignRepository.ActionStatus.SUCCESS
        }

        if (!pageInfo.hasSignedToday) {
            val signUrl = pageInfo.signActionUrl
                ?: return YamiboResult.Failure("已通過驗證，但找不到簽到按鈕，請改用手動模式。")
            val signAction = when (val action = executeAction(signUrl)) {
                is YamiboResult.Success -> action.value
                is YamiboResult.NotLoggedIn -> return action
                is YamiboResult.NoPermission -> return action
                is YamiboResult.Maintenance -> return action
                is YamiboResult.Failure -> return action
            }
            lastMessage = signAction.message
            finalStatus = signAction.status

            pageInfo = when (val refreshed = fetchPageInfo()) {
                is YamiboResult.Success -> refreshed.value
                is YamiboResult.NotLoggedIn -> return refreshed
                is YamiboResult.NoPermission -> return refreshed
                is YamiboResult.Maintenance -> return refreshed
                is YamiboResult.Failure -> return refreshed
            }
        } else {
            lastMessage = "今天已經打卡過了。"
        }

        if (allowRepair) {
            var seenRepairValues = emptySet<String>()
            while (pageInfo.repairOptions.isNotEmpty()) {
                val prefix = pageInfo.repairActionPrefix ?: break
                val repairOption = pageInfo.repairOptions.firstOrNull { it.value !in seenRepairValues } ?: break
                seenRepairValues += repairOption.value

                val repairUrl = buildAbsoluteUrl("$prefix${repairOption.value}")
                val repairAction = when (val action = executeAction(repairUrl)) {
                    is YamiboResult.Success -> action.value
                    is YamiboResult.NotLoggedIn -> return action
                    is YamiboResult.NoPermission -> return action
                    is YamiboResult.Maintenance -> return action
                    is YamiboResult.Failure -> return action
                }
                repairCount += 1
                lastMessage = repairAction.message
                finalStatus = repairAction.status

                pageInfo = when (val refreshed = fetchPageInfo()) {
                    is YamiboResult.Success -> refreshed.value
                    is YamiboResult.NotLoggedIn -> return refreshed
                    is YamiboResult.NoPermission -> return refreshed
                    is YamiboResult.Maintenance -> return refreshed
                    is YamiboResult.Failure -> return refreshed
                }
                seenRepairValues = emptySet()
            }
        }

        val message = when {
            repairCount > 0 -> "$lastMessage 已完成 $repairCount 次補簽。"
            else -> lastMessage
        }
        updateTodayRecord(pageInfo, message)
        return YamiboResult.Success(
            SignRepository.ActionResult(
                status = finalStatus,
                message = message,
                repairCount = repairCount,
                pageInfo = pageInfo,
            )
        )
    }

    override suspend fun getTodayRecord(): SignRepository.DailyRecord? {
        return recordQueries.getByDateKey(currentLocalDateKey()).executeAsOneOrNull()?.toModel()
    }

    override suspend fun isSignedToday(): Boolean {
        getCachedPageInfo()?.let { cached ->
            if (cached.hasSignedToday) return true
        }
        return getTodayRecord()?.isSigned == true
    }

    override fun getCachedPageInfo(): SignRepository.SignPageInfo? {
        val cacheUpdatedAt = appSettingsRepository.signPageHtmlCacheUpdatedAt.getValue().toLongOrNull()
            ?: return null
        if (currentLocalDateKeyAt(cacheUpdatedAt) != currentLocalDateKey()) {
            return null
        }
        val html = appSettingsRepository.signPageHtmlCache.getValue().trim()
        if (html.isBlank() || isCloudflarePage(html) || !isSignPageLikeHtml(html)) return null
        return runCatching { parseSignPage(html) }.getOrNull()
    }

    override fun cacheObservedHtml(html: String): SignRepository.SignPageInfo? {
        if (html.isBlank() || isCloudflarePage(html) || !isSignPageLikeHtml(html)) return null
        val info = runCatching { parseSignPage(html) }.getOrNull() ?: return null
        appSettingsRepository.signPageHtmlCache.setValue(html)
        appSettingsRepository.signPageHtmlCacheUpdatedAt.setValue(currentTimeMillis().toString())
        updateTodayRecord(info)
        return info
    }

    private fun applyCookies() {
        fetcher.setCookies(authRepository.cookieStore.load() ?: "")
    }

    private suspend fun executeAction(url: String): YamiboResult<ParsedActionResult> {
        applyCookies()
        val absoluteUrl = buildAbsoluteUrl(url)
        val cookie = authRepository.cookieStore.load().orEmpty()

        when (val result = performSignRequest(absoluteUrl, cookie)) {
            is FetchResult.Success ->
                return YamiboResult.Success(parseActionResult(result.value))

            is FetchResult.Failure.HttpError ->
                return YamiboResult.Failure("簽到操作失敗（HTTP ${result.statusCode}）")

            is FetchResult.Failure.NetworkError ->
                return YamiboResult.Failure(result.exception.message ?: "簽到操作失敗", result.exception)

            is FetchResult.Failure.Timeout ->
                return YamiboResult.Failure("簽到操作逾時", result.exception)

            is FetchResult.Failure.Unknown ->
                return YamiboResult.Failure(result.exception.message ?: "簽到操作失敗", result.exception)
        }
    }

    private suspend fun performSignRequest(url: String, cookie: String): FetchResult<String> {
        return try {
            val response = fetcher.perform(HttpMethod.Get, url) {
                headers[HttpHeaders.UserAgent] = SIGN_WEBVIEW_USER_AGENT
                headers["Referer"] = YamiboRoute.Sign.build()
                headers[HttpHeaders.Accept] =
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                headers["Upgrade-Insecure-Requests"] = "1"
                if (cookie.isNotBlank()) {
                    headers[HttpHeaders.Cookie] = cookie
                }
            }
            val text = response.bodyAsText()
            if (response.status.value in 200..299) {
                FetchResult.Success(value = text, statusCode = response.status.value, url = url)
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = text
                )
            }
        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }

    private fun parseActionResult(html: String): ParsedActionResult {
        val document = Ksoup.parse(html)
        val message = document.selectFirst(".jump_c p")?.text()?.trim().orEmpty()
        val status = when {
            message.contains("已经打过卡") -> SignRepository.ActionStatus.ALREADY_SIGNED
            message.contains("补签") -> SignRepository.ActionStatus.REPAIR_SUCCESS
            else -> SignRepository.ActionStatus.SUCCESS
        }
        return ParsedActionResult(
            status = status,
            message = if (message.isNotBlank()) message else "操作完成"
        )
    }

    private fun parseSignPage(html: String): SignRepository.SignPageInfo {
        val document = Ksoup.parse(html)
        val currentDateText = document.select(".hui-wrap .hui-content span.y").firstOrNull()?.text()?.trim()
        val monthLabel = document.select("#tablehead th").firstOrNull()?.ownText()?.trim()?.ifBlank { null }
        val notice = extractNotice(document)
        val calendarDays = parseCalendarDays(document)
        val repairOptions = document.select("#repairday option").mapNotNull { option ->
            val value = option.attr("value").trim()
            val label = option.text().trim()
            if (value.isBlank() || label.isBlank()) null else SignRepository.RepairOption(value, label)
        }
        val myActivity = extractSectionItems(document, "我的打卡动态")
        val statistics = extractSectionItems(document, "打卡统计")
        val extraSections = listOf(
            buildSection(document, "天天打卡固定奖励"),
            buildSection(document, "节日额外奖励"),
            buildSection(document, "打卡等级"),
        ).filterNotNull()
        val signActionUrl = document.selectFirst(".signbtn a.btna")?.attr("href")?.trim()?.takeIf { it.isNotBlank() }
            ?.let(::buildAbsoluteUrl)
        val repairActionPrefix = parseRepairActionPrefix(document)
        val currentDateKey = currentDateText?.let(::normalizeDateKey)
        val lastSignDateKey = myActivity.firstNotNullOfOrNull(::extractDateKey)
        val hasSignedToday = calendarDays.firstOrNull { it.isToday }?.isSigned == true ||
            (currentDateKey != null && currentDateKey == lastSignDateKey)

        return SignRepository.SignPageInfo(
            currentDateText = currentDateText,
            monthLabel = monthLabel,
            notice = notice,
            calendarDays = calendarDays,
            repairOptions = repairOptions,
            myActivity = myActivity,
            statistics = statistics,
            extraSections = extraSections,
            signActionUrl = signActionUrl,
            repairActionPrefix = repairActionPrefix,
            hasSignedToday = hasSignedToday,
            lastSignDateKey = lastSignDateKey,
        )
    }

    private fun extractNotice(document: Document): String? {
        val title = document.select(".hui-common-title-txt").firstOrNull {
            it.text().contains("打卡公告")
        } ?: return null
        return title.parent()?.nextElementSibling()?.text()?.trim()?.ifBlank { null }
    }

    private fun parseCalendarDays(document: Document): List<SignRepository.CalendarDay> {
        return document.select("#tablebody .day").mapNotNull { element ->
            val day = element.text().trim().toIntOrNull() ?: return@mapNotNull null
            val classes = element.classNames()
            SignRepository.CalendarDay(
                day = day,
                isSigned = classes.contains("on"),
                isToday = classes.contains("today"),
            )
        }
    }

    private fun extractSectionItems(document: Document, title: String): List<String> {
        return findSectionList(document, title)
            ?.select("li .hui-list-text")
            ?.mapNotNull { it.text().trim().ifBlank { null } }
            .orEmpty()
    }

    private fun buildSection(document: Document, title: String): SignRepository.InfoSection? {
        val items = extractSectionItems(document, title)
        return if (items.isEmpty()) null else SignRepository.InfoSection(title, items)
    }

    private fun findSectionList(document: Document, title: String): Element? {
        val sectionTitle = document.select(".hui-common-title-txt").firstOrNull {
            it.text().trim() == title
        } ?: return null
        return sectionTitle.parent()?.nextElementSibling()
    }

    private fun parseRepairActionPrefix(document: Document): String? {
        val onclick = document.selectFirst(".repairbtn")?.attr("onclick") ?: return null
        val repairToken = Regex("""repair=([^&'"]+)""").find(onclick)?.groupValues?.getOrNull(1) ?: return null
        return buildAbsoluteUrl("plugin.php?id=zqlj_sign&repair=$repairToken&repairday=")
    }

    private fun isCloudflarePage(html: String): Boolean {
        val body = html.lowercase()
        return body.contains("cloudflare") || body.contains("cf-chl") || body.contains("just a moment")
    }

    private fun formatHttpFailure(statusCode: Int, bodyPreview: String?): String {
        val preview = bodyPreview?.trim().orEmpty()
        if (preview.contains("<html", ignoreCase = true) || isCloudflarePage(preview)) {
            return "簽到頁被 Cloudflare 或站點頁面攔截，請先在 WebView 完成驗證。"
        }
        return "讀取簽到頁失敗（HTTP $statusCode）"
    }

    private fun isSignPageLikeHtml(html: String): Boolean {
        return html.contains("打卡公告") ||
            html.contains("我的打卡动态") ||
            html.contains("打卡统计") ||
            html.contains("点击打卡") ||
            html.contains("repairday")
    }

    private fun extractDateKey(text: String): String? {
        val normalized = text.replace('：', ':')
        val match = Regex("""(\d{4})[-年](\d{1,2})[-月](\d{1,2})""").find(normalized) ?: return null
        val year = match.groupValues[1]
        val month = match.groupValues[2].padStart(2, '0')
        val day = match.groupValues[3].padStart(2, '0')
        return "$year-$month-$day"
    }

    private fun normalizeDateKey(text: String): String? = extractDateKey(text)

    private fun buildAbsoluteUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val base = YamiboRoute.Domain.build()
        return if (url.startsWith("/")) "$base${url.removePrefix("/")}" else "$base$url"
    }

    private fun updateTodayRecord(
        info: SignRepository.SignPageInfo,
        message: String? = null,
    ) {
        val dateKey = currentLocalDateKey()
        recordQueries.upsert(
            dateKey = dateKey,
            isSigned = if (info.hasSignedToday) 1L else 0L,
            updatedAt = currentTimeMillis(),
            lastMessage = message,
        )
    }

    private fun SignDailyRecord.toModel(): SignRepository.DailyRecord {
        return SignRepository.DailyRecord(
            dateKey = dateKey,
            isSigned = isSigned != 0L,
            updatedAt = updatedAt,
            lastMessage = lastMessage,
        )
    }

    private data class ParsedActionResult(
        val status: SignRepository.ActionStatus,
        val message: String,
    )
}
