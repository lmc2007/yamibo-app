package io.github.littlesurvival.fetch.post.util

import com.fleeksoft.ksoup.Ksoup

/**
 * Utility for parsing Discuz POST response bodies.
 *
 * Discuz POST endpoints (rate, favorite, etc.) respond with XML containing a CDATA section with
 * HTML. The actual message is inside `#messagetext p` (text content only, excluding script tags).
 *
 * Example response body:
 * ```xml
 * <?xml version="1.0" encoding="utf-8"?>
 * <root>
 *     <![CDATA[<div class="tip">
 *     <dt id="messagetext">
 *         <p>
 *             感谢您的参与，现在将转入评分前页面
 *             <script type="text/javascript" reload="1">...</script>
 *         </p>
 *     </dt>
 * </div>]]>
 * </root>
 * ```
 *
 * Success responses contain `succeedhandle` in the script tag. Error responses contain
 * `errorhandle` in the script tag.
 */
object PostResponseUtils {

    /**
     * Parse the message text from a Discuz POST response body.
     *
     * @param body The raw response body (XML with CDATA HTML).
     * @return The extracted message text, or null if parsing fails.
     */
    fun parseMessageText(body: String): String? {
        // Strip XML wrapper to get the HTML inside CDATA
        val html = body.substringAfter("<![CDATA[", "").substringBefore("]]>", "").ifEmpty { body }

        val doc = Ksoup.parse(html)
        val messageEl = doc.select("#messagetext p").firstOrNull()
            ?: return null
        // Remove script tags to get just the message text
        messageEl.select("script").remove()
        return messageEl.text().trim().ifEmpty { null }
    }

    /**
     * Check whether the response body indicates a success.
     *
     * Success responses contain `succeedhandle` in their script callback. Error responses contain
     * `errorhandle`.
     *
     * @param body The raw response body.
     * @return `true` if the response indicates success, `false` otherwise.
     */
    fun isSuccess(body: String): Boolean {
        return body.contains("succeedhandle")
    }

    /**
     * Check whether the response is reload the page.
     * ```html
     * <?xml version="1.0" encoding="utf-8"?>
     * <root>
     *  <![CDATA[<script type="text/javascript" reload="1">window.location.href='forum.php?mod=viewthread&tid=559877&mobile=2';</script>]]>
     * </root>
     * ```
     */
    fun isVoteSuccess(body: String): Boolean {
        return body.contains("<root><![CDATA[<script type=\"text/javascript\" reload=\"1\">")
    }

    /**
     * Check whether the response is illegal body response.
     * Which contains
     * ```html
     * <h1>Discuz! System Error</h1>
     * <p>Time: yyyy-mm-dd hh:mm:ss +0000 IP: ... BackTraceID: ...</p>
     * <div class="info">
     *   <li>您当前的访问请求当中含有非法字符，已经被系统拒绝</li>
     * </div>
     * ```
     */
    fun isIllegal(body: String?): Boolean {
        return body?.contains("您当前的访问请求当中含有非法字符，已经被系统拒绝") ?: false
    }
}