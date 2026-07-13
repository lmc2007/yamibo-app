package me.thenano.yamibo.yamibo_app.profile.sign

import me.thenano.yamibo.yamibo_app.i18n.i18n

internal fun isCloudflareChallengeHtml(html: String): Boolean {
    val body = html.lowercase()
    return body.contains("<title>just a moment") ||
        body.contains("cf-chl") ||
        body.contains("challenge-platform") ||
        body.contains("verify you are human") ||
        body.contains("cloudflare") && body.contains("challenge")
}

internal fun isSignResultPageHtml(html: String): Boolean {
    return html.contains(i18n("提示信息")) && (
        html.contains(i18n("打卡成功")) ||
            html.contains(i18n("已经打过卡")) ||
            html.contains(i18n("补签")) ||
            html.contains(i18n("返回签详情"))
        )
}

internal fun isMaintenancePageHtml(html: String): Boolean {
    return html.contains(i18n("<title>百合会每日维护</title>")) ||
        html.contains("""<img class="pic" src="/images/backup01.jpg" alt=i18n("每日维护")>""")
}

