package me.thenano.yamibo.yamibo_app.store.auth

import io.github.littlesurvival.dto.page.ProfilePage
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.UserId

interface UserStore {
    val prefName: String
        get() = "session_store"

    fun load(): ProfilePage?
    fun save(userInfo: ProfilePage)
    fun clear()

    companion object {
        val Preview =
            ProfilePage(
                uid = UserId(656626),
                username = "thenano",
                userGroup = "百合花蕾",
                points = 239,
                partner = 0,
                totalPoints = 239,
                avatarUrl =
                    "https://bbs.yamibo.com/uc_server/data/avatar/000/65/66/26_avatar_middle.jpg?ts=1725126833",
                avatarBackgroundUrl = null,
                gender = "保密",
                birthday = "-",
                onlineHours = 172,
                registerTime = TimeInfo.parse("2024-8-14 20:23"),
                lastVisit = TimeInfo.parse("2026-2-24 00:49"),
                formHash = FormHash("dummy")
            )
    }
}
