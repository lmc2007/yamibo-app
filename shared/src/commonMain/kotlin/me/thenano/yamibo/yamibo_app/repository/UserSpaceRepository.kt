package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.AddFriendPopoutScreen
import io.github.littlesurvival.dto.page.PrivateMessagePage
import io.github.littlesurvival.dto.page.ProfilePage
import io.github.littlesurvival.dto.page.UserSpaceBlogPage
import io.github.littlesurvival.dto.page.UserSpaceFriendPage
import io.github.littlesurvival.dto.page.UserSpaceNoticePage
import io.github.littlesurvival.dto.page.UserSpacePrivateMessagePage
import io.github.littlesurvival.dto.page.UserSpaceThreadPage
import io.github.littlesurvival.dto.page.UserSpaceThreadReplyPage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.PrivateMessageId
import io.github.littlesurvival.dto.value.UserId

interface UserSpaceRepository {
    data class UserPageCacheKey(val userId: Int?, val page: Int) {
        fun toCacheKey(): String = "${userId ?: "self"}_$page"
        companion object {
            fun keyPrefix(userId: Int?): String = "${userId ?: "self"}_"
        }
    }

    data class TypedPageCacheKey(val type: String, val userId: Int?, val page: Int) {
        fun toCacheKey(): String = "${type}_${userId ?: "self"}_$page"
        companion object {
            fun keyPrefix(type: String, userId: Int?): String = "${type}_${userId ?: "self"}_"
        }
    }

    suspend fun fetchProfile(userId: UserId? = null): YamiboResult<ProfilePage>
    suspend fun fetchThreads(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceThreadPage>
    suspend fun fetchReplies(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceThreadReplyPage>
    suspend fun fetchMyBlogs(userId: UserId? = null, page: Int = 1): YamiboResult<UserSpaceBlogPage>
    suspend fun fetchFriendBlogs(page: Int = 1): YamiboResult<UserSpaceBlogPage>
    suspend fun fetchViewAllBlogs(
        type: YamiboRoute.UserSpace.Blog.ViewAllType = YamiboRoute.UserSpace.Blog.ViewAllType.Latest,
        page: Int = 1
    ): YamiboResult<UserSpaceBlogPage>
    suspend fun fetchFriends(
        type: YamiboRoute.UserSpace.FriendPageType,
        page: Int = 1
    ): YamiboResult<UserSpaceFriendPage>
    suspend fun fetchPrivateMessages(page: Int = 1): YamiboResult<UserSpacePrivateMessagePage>
    suspend fun fetchAddFriendPopoutScreen(userId: UserId): YamiboResult<AddFriendPopoutScreen>
    suspend fun addFriend(
        userId: UserId,
        formHash: FormHash,
        note: String = "",
        groupId: Int = 1,
    ): YamiboResult<String>
    suspend fun fetchPrivateMessagePage(toUser: UserId, page: Int? = null): YamiboResult<PrivateMessagePage>
    suspend fun sendPrivateMessage(
        privateMessageId: PrivateMessageId,
        toUser: UserId,
        message: String,
        formHash: FormHash,
    ): YamiboResult<String>
    suspend fun fetchNotices(page: Int = 1): YamiboResult<UserSpaceNoticePage>

    fun getCachedProfile(userId: UserId? = null): ProfilePage?
    fun getCachedThreads(userId: UserId? = null, page: Int = 1): UserSpaceThreadPage?
    fun getCachedReplies(userId: UserId? = null, page: Int = 1): UserSpaceThreadReplyPage?
    fun getCachedMyBlogs(userId: UserId? = null, page: Int = 1): UserSpaceBlogPage?
    fun getCachedFriendBlogs(page: Int = 1): UserSpaceBlogPage?
    fun getCachedViewAllBlogs(type: YamiboRoute.UserSpace.Blog.ViewAllType, page: Int = 1): UserSpaceBlogPage?
    fun getCachedFriends(type: YamiboRoute.UserSpace.FriendPageType, page: Int = 1): UserSpaceFriendPage?
    fun getCachedPrivateMessages(page: Int = 1): UserSpacePrivateMessagePage?
    fun getCachedPrivateMessagePage(toUser: UserId, page: Int? = null): PrivateMessagePage?
    fun getCachedNotices(page: Int = 1): UserSpaceNoticePage?

    fun clearUserPages(userId: UserId?)
    fun clearFriendPages(type: YamiboRoute.UserSpace.FriendPageType? = null)
    fun clearMessagePages()
    fun clearPrivateMessagePages(toUser: UserId)
    fun clearNoticePages()
}
