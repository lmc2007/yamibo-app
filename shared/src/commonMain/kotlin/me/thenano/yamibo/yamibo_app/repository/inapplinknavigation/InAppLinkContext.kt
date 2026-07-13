package me.thenano.yamibo.yamibo_app.repository.inapplinknavigation

import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository

/**
 * Context from the current screen that helps resolve Yamibo links with fewer fetches.
 *
 * Thread reader screens should pass their current tid/title/fid/author/thread type when possible.
 * Generic HTML surfaces can pass the default empty context; URL parsing still works, but ambiguous
 * thread and findpost links may need one extra fetch to determine forum type or novel author id.
 */
data class InAppLinkContext(
    val currentTid: ThreadId? = null,
    val currentTitle: String? = null,
    val currentFid: ForumId? = null,
    val currentAuthorId: UserId? = null,
    val currentThreadType: ReadHistoryRepository.ThreadEntryType? = null,
)