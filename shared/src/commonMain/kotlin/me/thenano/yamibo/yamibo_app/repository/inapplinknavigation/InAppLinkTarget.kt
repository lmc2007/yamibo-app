package me.thenano.yamibo.yamibo_app.repository.inapplinknavigation

import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository

sealed interface InAppLinkTarget {
    data class ForumTarget(val fid: ForumId, val title: String? = null, val page: Int? = null) : InAppLinkTarget
    data class ThreadReaderTarget(
        val tid: ThreadId,
        val title: String,
        val threadType: ReadHistoryRepository.ThreadEntryType,
        val authorId: UserId? = null,
        val initialPage: Int = 1,
        val targetPid: PostId? = null,
    ) : InAppLinkTarget

    data class NovelDetailTarget(
        val tid: ThreadId,
        val title: String,
        val authorId: UserId? = null,
        val notice: String? = null,
    ) : InAppLinkTarget

    data class CommentReaderTarget(
        val tid: ThreadId,
        val postTitle: String,
        val oPostId: PostId,
        val authorId: UserId,
        val targetCommentPid: PostId? = null,
    ) : InAppLinkTarget

    data class UserSpaceTarget(val userId: UserId, val titleHint: String? = null) : InAppLinkTarget
    data class BlogReaderTarget(val blogId: BlogId, val userId: UserId? = null, val titleHint: String? = null) : InAppLinkTarget
    data class TagDetailTarget(val tagId: TagId, val title: String, val page: Int? = null) : InAppLinkTarget
    data class WebOnlyTarget(val url: String) : InAppLinkTarget
    data class UnsupportedTarget(val url: String) : InAppLinkTarget
}