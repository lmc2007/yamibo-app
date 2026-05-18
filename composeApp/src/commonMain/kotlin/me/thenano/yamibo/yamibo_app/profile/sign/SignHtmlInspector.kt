package me.thenano.yamibo.yamibo_app.profile.sign

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

internal fun isCloudflareChallengeHtml(html: String): Boolean {
    val body = html.lowercase()
    return body.contains("<title>just a moment") ||
        body.contains("cf-chl") ||
        body.contains("challenge-platform") ||
        body.contains("verify you are human") ||
        body.contains("cloudflare") && body.contains("challenge")
}

internal fun isSignPageHtml(html: String): Boolean {
    return html.contains(appString(Res.string.ui_click_check_in)) ||
        html.contains(appString(Res.string.ui_check_in_announcement)) ||
        html.contains("repairday") ||
        html.contains(appString(Res.string.ui_my_check_in_status))
}

internal fun isSignResultPageHtml(html: String): Boolean {
    return html.contains(appString(Res.string.ui_prompt_message)) && (
        html.contains(appString(Res.string.ui_check_in_successfully)) ||
            html.contains(appString(Res.string.ui_already_checked_in)) ||
            html.contains(appString(Res.string.ui_re_signing_2)) ||
            html.contains(appString(Res.string.ui_return_signature_details))
        )
}

internal fun isMaintenancePageHtml(html: String): Boolean {
    return html.contains(appString(Res.string.ui_title_yamibo_daily_maintenance_title)) ||
        html.contains("""<img class="pic" src="/images/backup01.jpg" alt=appString(Res.string.ui_daily_maintenance)>""")
}

