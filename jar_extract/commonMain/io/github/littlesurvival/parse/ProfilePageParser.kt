package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.ProfilePage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.parse.util.ParseUtils

class ProfilePageParser : Parser<ProfilePage> {

    override suspend fun parse(html: String): ParseResult<ProfilePage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            // --- Username from avatar section ---
            val username = doc.selectFirst(".avatar_bg .name")?.text()?.trim() ?: ""

            // --- Avatar URL ---
            val avatarUrl =
                doc.selectFirst(".avatar_m img")?.attr("src")?.substringBefore("?")?.ifEmpty {
                    null
                }

            // --- Credits / Points from user_box ---
            val creditItems = doc.select(".user_box li")
            var points = 0
            var partner = 0
            var totalPoints = 0

            for (li in creditItems) {
                val text = li.text()
                val spanText = li.selectFirst("span")?.text()?.trim() ?: ""
                val value = spanText.replace("点", "").trim().toIntOrNull() ?: 0

                when {
                    text.contains("总积分") || text.contains("總積分") -> totalPoints = value
                    text.contains("对象") || text.contains("對象") -> partner = value
                    text.contains("积分") || text.contains("積分") -> points = value
                }
            }

            // --- Personal info from myinfo_list ---
            val infoItems = doc.select(".myinfo_list li")
            var uid = UserId(0)
            var userGroup = ""
            var gender: String? = null
            var birthday: String? = null
            var onlineHours = 0
            var registerTime: String? = null
            var lastVisit: String? = null

            for (li in infoItems) {
                val label = li.ownText().trim()
                val value = li.selectFirst("span")?.text()?.trim() ?: ""

                when {
                    label == "UID" -> uid = UserId(value.toIntOrNull() ?: 0)
                    label.contains("用户组") || label.contains("用戶組") ->
                        userGroup =
                            li.selectFirst("span font")?.text()?.trim()?.ifEmpty { value }
                                ?: value

                    label.contains("性别") || label.contains("性別") -> gender = value.ifEmpty { null }
                    label.contains("生日") -> birthday = value.takeIf { it != "-" && it.isNotEmpty() }
                    label.contains("在线时间") || label.contains("在線時間") ->
                        onlineHours =
                            value.replace("小时", "").replace("小時", "").trim().toIntOrNull()
                                ?: 0

                    label.contains("注册时间") || label.contains("註冊時間") ->
                        registerTime = value.ifEmpty { null }

                    label.contains("最后访问") || label.contains("最後訪問") ->
                        lastVisit = value.ifEmpty { null }
                }
            }

            // --- Form Hash from input or logout link ---
            val formHash =
                doc.selectFirst("input[name=formhash]")?.attr("value")?.ifEmpty { null }
                    ?: doc.selectFirst(".btn_exit a")?.attr("href")?.let {
                        FORMHASH_RE.find(it)?.groupValues?.get(1)
                    }
            val formHashValue = formHash?.let { FormHash(it) }

            ParseResult.Success(
                ProfilePage(
                    uid = uid,
                    username = username,
                    userGroup = userGroup,
                    points = points,
                    partner = partner,
                    totalPoints = totalPoints,
                    avatarUrl = avatarUrl,
                    gender = gender,
                    birthday = birthday,
                    onlineHours = onlineHours,
                    registerTime = registerTime,
                    lastVisit = lastVisit,
                    formHash = formHashValue
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse profile page", e)
        }
    }

    companion object {
        private val FORMHASH_RE = Regex("formhash=([a-f0-9]+)")
    }
}
