package me.thenano.yamibo.yamibo_app.thread.reader.components.post

import io.github.littlesurvival.dto.page.ManageButton
import io.github.littlesurvival.dto.page.PollStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.shouldShowVotersButton

class PostActionPresentationTest {
    @Test
    fun manageButtonUsesActionNameOnlyWhenThereIsOneAction() {
        assertNull(manageButtonLabel(emptyList()))
        assertEquals("編輯", manageButtonLabel(listOf(action("編輯"))))
        assertEquals("管理", manageButtonLabel(listOf(action("編輯"), action("置頂"))))
    }

    @Test
    fun votersButtonRequiresVotedPollAndLoader() {
        assertTrue(shouldShowVotersButton(PollStatus.Voted, canLoadVoters = true))
        assertFalse(shouldShowVotersButton(PollStatus.NotVoted, canLoadVoters = true))
        assertFalse(shouldShowVotersButton(PollStatus.Voted, canLoadVoters = false))
    }

    private fun action(name: String) = ManageButton(name, "forum.php?mod=post")
}
