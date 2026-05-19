package me.thenano.yamibo.yamibo_app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import io.github.littlesurvival.core.YamiboResult
import kotlinx.coroutines.runBlocking
import me.thenano.yamibo.yamibo_app.i18n.AppMessage
import me.thenano.yamibo.yamibo_app.repository.settings.AppLanguage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

private const val AppStringCacheMaxSize = 1024

private data class AppStringCacheKey(
    val languageTag: String,
    val resourceKey: String,
    val args: List<String>,
)

private var currentAppLanguageTag: String = AppLanguage.TRADITIONAL_CHINESE.languageTag
private var appStringCache: Map<AppStringCacheKey, String> = emptyMap()

@Composable
fun AppLocaleProvider(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val localeKey = remember(language) {
        applyAppLanguage(language)
    }

    key(localeKey) {
        content()
    }
}

expect fun applyAppLocale(language: AppLanguage)

private fun applyAppLanguage(language: AppLanguage): String {
    if (currentAppLanguageTag != language.languageTag) {
        currentAppLanguageTag = language.languageTag
        appStringCache = emptyMap()
    }
    applyAppLocale(language)
    return language.languageTag
}

fun appString(resource: StringResource, vararg formatArgs: Any): String {
    val args = formatArgs.map { it.toString() }
    val key = AppStringCacheKey(currentAppLanguageTag, resource.key, args)
    appStringCache[key]?.let { return it }

    val value = runBlocking {
        getString(resource, *formatArgs)
    }
    appStringCache = if (appStringCache.size >= AppStringCacheMaxSize) {
        mapOf(key to value)
    } else {
        appStringCache + (key to value)
    }
    return value
}

fun localizedAppMessage(value: String?): String {
    if (value.isNullOrBlank()) return value.orEmpty()
    val parsed = AppMessage.parse(value) ?: return value
    fun arg(index: Int): String = parsed.args.getOrNull(index).orEmpty()
    fun argInt(index: Int): Int = arg(index).toIntOrNull() ?: 0
    return when (parsed.key) {
        "common.all" -> appString(Res.string.common_all)
        "auth.no_login_data" -> appString(Res.string.msg_auth_no_login_data)
        "auth.expired" -> appString(Res.string.msg_auth_expired)
        "auth.maintenance" -> appString(Res.string.msg_auth_maintenance)
        "auth.profile_failed" -> appString(Res.string.msg_auth_profile_failed, arg(0))
        "storage.images" -> appString(Res.string.msg_storage_images)
        "storage.pages" -> appString(Res.string.msg_storage_pages)
        "storage.userspace" -> appString(Res.string.msg_storage_userspace)
        "storage.other" -> appString(Res.string.msg_storage_other)
        "favorite.default_category" -> appString(Res.string.favorite_sort_default)
        "favorite.category_name_blank" -> appString(Res.string.msg_favorite_category_name_blank)
        "favorite.category_name_used" -> appString(Res.string.msg_favorite_category_name_used, arg(0))
        "favorite.collection_name_used" -> appString(Res.string.msg_favorite_collection_name_used, arg(0))
        "favorite.sync.preparing" -> appString(Res.string.msg_favorite_sync_preparing)
        "favorite.sync.canceled" -> appString(Res.string.msg_favorite_sync_canceled)
        "favorite.sync.item_missing" -> appString(Res.string.msg_favorite_sync_item_missing)
        "favorite.sync.unsupported_type" -> appString(Res.string.msg_favorite_sync_unsupported_type)
        "favorite.sync.already_remote" -> appString(Res.string.msg_favorite_sync_already_remote)
        "favorite.sync.not_logged_in" -> appString(Res.string.msg_favorite_sync_not_logged_in)
        "favorite.sync.maintenance" -> appString(Res.string.msg_favorite_sync_maintenance)
        "favorite.sync.uploaded" -> appString(Res.string.msg_favorite_sync_uploaded)
        "favorite.sync.uploaded_no_order" -> appString(Res.string.msg_favorite_sync_uploaded_no_order)
        "favorite.sync.uploaded_no_order_reason" -> appString(Res.string.msg_favorite_sync_uploaded_no_order_reason, arg(0))
        "favorite.sync.target_missing" -> appString(Res.string.msg_favorite_sync_target_missing)
        "favorite.sync.start" -> appString(Res.string.msg_favorite_sync_start)
        "favorite.sync.fetching_pages" -> appString(Res.string.msg_favorite_sync_fetching_pages)
        "favorite.sync.remote_pages_changed" -> appString(Res.string.msg_favorite_sync_remote_pages_changed)
        "favorite.sync.missing_thread_id" -> appString(Res.string.msg_favorite_sync_missing_thread_id, arg(0))
        "favorite.sync.duplicate_thread" -> appString(Res.string.msg_favorite_sync_duplicate_thread, arg(0))
        "favorite.sync.page_progress" -> appString(Res.string.msg_favorite_sync_page_progress, argInt(0), argInt(1))
        "favorite.sync.fetched_count" -> appString(Res.string.msg_favorite_sync_fetched_count, argInt(0))
        "favorite.sync.not_logged_in_retry" -> appString(Res.string.msg_favorite_sync_not_logged_in_retry)
        "favorite.sync.maintenance_retry" -> appString(Res.string.msg_favorite_sync_maintenance_retry)
        "favorite.sync.import_remote_threads" -> appString(Res.string.msg_favorite_sync_import_remote_threads)
        "favorite.sync.item_progress" -> appString(Res.string.msg_favorite_sync_item_progress, argInt(0), argInt(1), arg(2), arg(3))
        "favorite.sync.duplicate_synced" -> appString(Res.string.msg_favorite_sync_duplicate_synced, argInt(0), arg(1))
        "favorite.sync.reconcile_failed" -> appString(Res.string.msg_favorite_sync_reconcile_failed, arg(0))
        "favorite.sync.delete_not_logged_in" -> appString(Res.string.msg_favorite_sync_delete_not_logged_in)
        "favorite.sync.delete_maintenance" -> appString(Res.string.msg_favorite_sync_delete_maintenance)
        "favorite.sync.delete_missing_id" -> appString(Res.string.msg_favorite_sync_delete_missing_id)
        "favorite.sync.delete_failed" -> appString(Res.string.msg_favorite_sync_delete_failed)
        "favorite.sync.formhash_failed" -> appString(Res.string.msg_favorite_sync_formhash_failed)
        "favorite.sync.remote_not_logged_in" -> appString(Res.string.msg_favorite_sync_remote_not_logged_in)
        "favorite.sync.remote_maintenance" -> appString(Res.string.msg_favorite_sync_remote_maintenance)
        "favorite.sync.untitled_thread" -> appString(Res.string.msg_favorite_sync_untitled_thread)
        "favorite.sync.import_failed" -> appString(Res.string.msg_favorite_sync_import_failed, arg(0), arg(1))
        "favorite.sync.upload_failed" -> appString(Res.string.msg_favorite_sync_upload_failed, arg(0), arg(1))
        "favorite.update.preparing" -> appString(Res.string.msg_favorite_update_preparing)
        "favorite.update.continue" -> appString(Res.string.msg_favorite_update_continue)
        "favorite.update.interrupted" -> appString(Res.string.msg_favorite_update_interrupted)
        "favorite.update.canceled" -> appString(Res.string.msg_favorite_update_canceled)
        "favorite.update.loaded_resume" -> appString(Res.string.msg_favorite_update_loaded_resume, argInt(0), argInt(1))
        "favorite.update.loaded" -> appString(Res.string.msg_favorite_update_loaded, argInt(0))
        "favorite.update.item_progress" -> appString(Res.string.msg_favorite_update_item_progress, argInt(0), argInt(1), arg(2), arg(3))
        "favorite.update.completed" -> appString(Res.string.msg_favorite_update_completed)
        "favorite.update.not_logged_in" -> appString(Res.string.msg_favorite_update_not_logged_in, arg(0))
        "favorite.update.maintenance" -> appString(Res.string.msg_favorite_update_maintenance, arg(0))
        "favorite.update.multiple_new" -> appString(Res.string.msg_favorite_update_multiple_new)
        "favorite.update.author_new_count" -> appString(Res.string.msg_favorite_update_author_new_count, argInt(0))
        "favorite.update.reply_new_count" -> appString(Res.string.msg_favorite_update_reply_new_count, argInt(0))
        "favorite.update.author_new" -> appString(Res.string.msg_favorite_update_author_new)
        "favorite.update.thread_new" -> appString(Res.string.msg_favorite_update_thread_new)
        "favorite.update.tag_new" -> appString(Res.string.msg_favorite_update_tag_new)
        "favorite.update.author_edited" -> appString(Res.string.msg_favorite_update_author_edited)
        "favorite.update.thread_edited" -> appString(Res.string.msg_favorite_update_thread_edited)
        "favorite.update.tag_edited" -> appString(Res.string.msg_favorite_update_tag_edited)
        "favorite.update.tag_load_failed" -> appString(Res.string.msg_favorite_update_tag_load_failed)
        "favorite.update.tag_new_count" -> appString(Res.string.msg_favorite_update_tag_new_count, argInt(0))
        "favorite.update.tag_page_ambiguous" -> appString(Res.string.msg_favorite_update_tag_page_ambiguous)
        "inapp.progress.parse_link" -> appString(Res.string.msg_inapp_progress_parse_link)
        "inapp.progress.findpost" -> appString(Res.string.msg_inapp_progress_findpost)
        "inapp.progress.forum_type" -> appString(Res.string.msg_inapp_progress_forum_type)
        "inapp.progress.author" -> appString(Res.string.msg_inapp_progress_author)
        "inapp.progress.author_post" -> appString(Res.string.msg_inapp_progress_author_post)
        "inapp.progress.search_author_page" -> appString(Res.string.msg_inapp_progress_search_author_page, argInt(0), argInt(1))
        "inapp.progress.confirm_author_page" -> appString(Res.string.msg_inapp_progress_confirm_author_page, argInt(0), argInt(1))
        "inapp.progress.comment_parent" -> appString(Res.string.msg_inapp_progress_comment_parent)
        "inapp.progress.scan_comment_page" -> appString(Res.string.msg_inapp_progress_scan_comment_page, argInt(0))
        "inapp.progress.thread_home" -> appString(Res.string.msg_inapp_progress_thread_home)
        "inapp.notice.precise_failed" -> appString(Res.string.msg_inapp_notice_precise_failed)
        "sign.verified_no_button" -> appString(Res.string.msg_sign_verified_no_button)
        "sign.already_signed" -> appString(Res.string.msg_sign_already_signed)
        "sign.repair_completed" -> appString(Res.string.msg_sign_repair_completed, arg(0), argInt(1))
        "sign.cloudflare_required" -> appString(Res.string.msg_sign_cloudflare_required)
        "sign.action_completed" -> appString(Res.string.msg_sign_action_completed)
        else -> value
    }
}

fun YamiboResult<*>.localizedMessage(): String {
    return localizedAppMessage(message())
}
