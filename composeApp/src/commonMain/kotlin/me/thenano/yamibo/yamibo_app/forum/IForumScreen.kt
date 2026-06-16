package me.thenano.yamibo.yamibo_app.forum

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.ForumId
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot

@Serializable
private data class ForumScreenRestorePayload(
    val fid: Int,
    val name: String,
    val initialPage: Int = 1,
    val filterTypeId: Int? = null,
    val orderFilter: String? = null,
    val orderBy: String? = null,
)

/** Navigatable screen for a specific forum page. */
@RestorableScreenEntry
class IForumScreen(
    val fid: ForumId,
    val name: String,
    val initialPage: Int = 1,
    val filterTypeId: Int? = null,
    val orderFilter: String? = null,
    val orderBy: String? = null,
) : RestorableNavigatable {
    override val id = buildId(
        fid.value,
        initialPage.coerceAtLeast(1),
        filterTypeId ?: 0,
        orderFilter.orEmpty(),
        orderBy.orEmpty(),
    )
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = ForumScreenRestorePayload(
            fid = fid.value,
            name = name,
            initialPage = initialPage.coerceAtLeast(1),
            filterTypeId = filterTypeId,
            orderFilter = orderFilter,
            orderBy = orderBy,
        ),
    )

    @Composable
    override fun Content() {
        ForumPageScreen(
            fid = fid,
            name = name,
            initialPage = initialPage,
            initialFilterTypeId = filterTypeId,
            initialOrderFilter = orderFilter,
            initialOrderBy = orderBy,
        )
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IForumScreen>(IForumScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<ForumScreenRestorePayload>(payload)
            return IForumScreen(
                fid = ForumId(data.fid),
                name = data.name,
                initialPage = data.initialPage,
                filterTypeId = data.filterTypeId,
                orderFilter = data.orderFilter,
                orderBy = data.orderBy,
            )
        }
    }
}
