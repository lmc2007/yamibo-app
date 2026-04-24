package me.thenano.yamibo.yamibo_app.navigation

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.thenano.yamibo.yamibo_app.IMainScreen
import me.thenano.yamibo.yamibo_app.Logger
import me.thenano.yamibo.yamibo_app.favorite.IFavoriteCategoryEditorScreen
import me.thenano.yamibo.yamibo_app.favorite.IFavoriteCategoryManageScreen
import me.thenano.yamibo.yamibo_app.favorite.sync.IFavoriteSyncProgressScreen
import me.thenano.yamibo.yamibo_app.forum.IForumScreen
import me.thenano.yamibo.yamibo_app.profile.ILoginScreen
import me.thenano.yamibo.yamibo_app.profile.settings.ISettingsCategoryScreen
import me.thenano.yamibo.yamibo_app.profile.settings.ISettingsScreen
import me.thenano.yamibo.yamibo_app.profile.settings.access.IBackgroundAccessSetupScreen
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.detail.tag.ITagDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.ICommentReaderScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IImageReaderScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.thread.reader.components.tag.ITagListScreen
import kotlin.reflect.KClass

@Serializable
data class RestorableScreenSnapshot(
    val type: String,
    val payload: String = "",
)

interface RestorableNavigatable : Navigatable {
    val restoreDecoder: RestorableNavigatableDecoder
    fun toRestoreSnapshot(): RestorableScreenSnapshot
}

interface RestorableNavigatableDecoder {
    val screenClass: KClass<out RestorableNavigatable>
    val type: String
        get() = defaultRestoreType(screenClass)
    fun decode(payload: String): RestorableNavigatable
}

abstract class TypedRestorableNavigatableDecoder<T : RestorableNavigatable>(
    final override val screenClass: KClass<T>,
) : RestorableNavigatableDecoder

interface NavigationRestoreLogger {
    fun onSnapshotStart(stackSize: Int)
    fun onSnapshotItemSkipped(screenId: String, reason: String)
    fun onSnapshotBuilt(restorableStackSize: Int)
    fun onSnapshotSaved(restorableStackSize: Int, bytes: Int)
    fun onRestoreStart(snapshotSize: Int)
    fun onRestoreItemDecoded(type: String)
    fun onRestoreItemFailed(type: String, reason: String)
    fun onRestoreFinished(restoredStackSize: Int)
}

object NoOpNavigationRestoreLogger : NavigationRestoreLogger {
    override fun onSnapshotStart(stackSize: Int) = Unit
    override fun onSnapshotItemSkipped(screenId: String, reason: String) = Unit
    override fun onSnapshotBuilt(restorableStackSize: Int) = Unit
    override fun onSnapshotSaved(restorableStackSize: Int, bytes: Int) = Unit
    override fun onRestoreStart(snapshotSize: Int) = Unit
    override fun onRestoreItemDecoded(type: String) = Unit
    override fun onRestoreItemFailed(type: String, reason: String) = Unit
    override fun onRestoreFinished(restoredStackSize: Int) = Unit
}

object LoggerNavigationRestoreLogger : NavigationRestoreLogger {
    private const val TAG = "NavigationRestore"

    override fun onSnapshotStart(stackSize: Int) {
        Logger.d(TAG, "snapshot_start stackSize=$stackSize")
    }

    override fun onSnapshotItemSkipped(screenId: String, reason: String) {
        Logger.d(TAG, "snapshot_skip screenId=$screenId reason=$reason")
    }

    override fun onSnapshotBuilt(restorableStackSize: Int) {
        Logger.d(TAG, "snapshot_built restorableStackSize=$restorableStackSize")
    }

    override fun onSnapshotSaved(restorableStackSize: Int, bytes: Int) {
        Logger.d(TAG, "snapshot_saved restorableStackSize=$restorableStackSize bytes=$bytes")
    }

    override fun onRestoreStart(snapshotSize: Int) {
        Logger.d(TAG, "restore_start snapshotSize=$snapshotSize")
    }

    override fun onRestoreItemDecoded(type: String) {
        Logger.d(TAG, "restore_item_decoded type=$type")
    }

    override fun onRestoreItemFailed(type: String, reason: String) {
        Logger.w(TAG, "restore_item_failed type=$type reason=$reason")
    }

    override fun onRestoreFinished(restoredStackSize: Int) {
        Logger.d(TAG, "restore_finished restoredStackSize=$restoredStackSize")
    }
}

internal val navigationRestoreJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

internal fun defaultRestoreType(screenClass: KClass<out RestorableNavigatable>): String =
    requireNotNull(screenClass.simpleName) { "Missing simpleName for $screenClass" }.lowercase()

internal fun Navigatable.toRestoreSnapshotOrNull(): RestorableScreenSnapshot? =
    (this as? RestorableNavigatable)?.toRestoreSnapshot()

internal fun emptyRestoreSnapshot(decoder: RestorableNavigatableDecoder): RestorableScreenSnapshot =
    RestorableScreenSnapshot(type = decoder.type)

internal inline fun <reified T> restoreSnapshot(
    decoder: RestorableNavigatableDecoder,
    payload: T,
): RestorableScreenSnapshot = RestorableScreenSnapshot(
    type = decoder.type,
    payload = navigationRestoreJson.encodeToString(payload),
)

internal inline fun <reified T> decodeRestorePayload(payload: String): T =
    navigationRestoreJson.decodeFromString(payload)

internal object RestorableScreenRegistry {
    private val decoders: Map<String, RestorableNavigatableDecoder> = listOf(
        IMainScreen.Decoder,
        IForumScreen.Decoder,
        INovelThreadDetailScreen.Decoder,
        IThreadReaderScreen.Decoder,
        IImageReaderScreen.Decoder,
        ICommentReaderScreen.Decoder,
        IFavoriteCategoryManageScreen.Decoder,
        IFavoriteCategoryEditorScreen.Decoder,
        ISettingsScreen.Decoder,
        ISettingsCategoryScreen.Decoder,
        IBackgroundAccessSetupScreen.Decoder,
        ILoginScreen.Decoder,
        IFavoriteSyncProgressScreen.Decoder,
        ITagDetailScreen.Decoder,
        ITagListScreen.Decoder,
    ).associateBy { it.type }

    fun decode(
        snapshot: RestorableScreenSnapshot,
        logger: NavigationRestoreLogger,
    ): RestorableNavigatable? {
        val decoder = decoders[snapshot.type]
        if (decoder == null) {
            logger.onRestoreItemFailed(snapshot.type, "decoder_missing")
            return null
        }

        return try {
            decoder.decode(snapshot.payload).also {
                logger.onRestoreItemDecoded(snapshot.type)
            }
        } catch (throwable: Throwable) {
            logger.onRestoreItemFailed(
                type = snapshot.type,
                reason = throwable.message ?: throwable::class.simpleName ?: "unknown_error",
            )
            null
        }
    }
}
