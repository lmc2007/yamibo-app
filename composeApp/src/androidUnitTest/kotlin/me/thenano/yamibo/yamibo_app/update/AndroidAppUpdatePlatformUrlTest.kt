package me.thenano.yamibo.yamibo_app.update

import kotlin.test.Test
import kotlin.test.assertEquals
import me.thenano.yamibo.yamibo_app.repository.settings.AppUpdateDownloadMode
import me.thenano.yamibo.yamibo_app.repository.settings.AppUpdateDownloadProxy

class AndroidAppUpdatePlatformUrlTest {

    private val releaseUrl = "https://github.com/lmc2007/yamibo-app/releases/download/8/yamibo-stable-v0.1.6.apk"
    private val objectUrl = "https://objects.githubusercontent.com/github-production-release/asset/123.apk"

    @Test
    fun proxyModeDefaultsToGhProxyComThenDirect() {
        assertEquals(
            listOf("https://gh-proxy.com/$releaseUrl", releaseUrl),
            resolveApkDownloadUrls(releaseUrl),
        )
        assertEquals(
            listOf("https://gh-proxy.com/$objectUrl", objectUrl),
            resolveApkDownloadUrls(objectUrl),
        )
    }

    @Test
    fun proxyModeUsesGhproxyNet() {
        assertEquals(
            listOf("https://ghproxy.net/$releaseUrl", releaseUrl),
            resolveApkDownloadUrls(
                releaseUrl,
                AppUpdateDownloadMode.PROXY,
                AppUpdateDownloadProxy.GHPROXY_NET,
            ),
        )
    }

    @Test
    fun proxyModeUsesGhDpikTop() {
        assertEquals(
            listOf("https://gh.dpik.top/$releaseUrl", releaseUrl),
            resolveApkDownloadUrls(
                releaseUrl,
                AppUpdateDownloadMode.PROXY,
                AppUpdateDownloadProxy.GH_DPIK_TOP,
            ),
        )
    }

    @Test
    fun directModeStaysDirectOnly() {
        assertEquals(
            listOf(releaseUrl),
            resolveApkDownloadUrls(releaseUrl, AppUpdateDownloadMode.DIRECT),
        )
        assertEquals(
            listOf(objectUrl),
            resolveApkDownloadUrls(objectUrl, AppUpdateDownloadMode.DIRECT),
        )
    }

    @Test
    fun giteeAssetStaysDirectOnly() {
        val url = "https://gitee.com/LittleSurvival/ymb-apk-release/releases/download/8/yamibo.apk"

        assertEquals(listOf(url), resolveApkDownloadUrls(url))
        assertEquals(listOf(url), resolveApkDownloadUrls(url, AppUpdateDownloadMode.DIRECT))
        assertEquals(listOf(url), resolveApkDownloadUrls(url, AppUpdateDownloadMode.PROXY))
    }

    @Test
    fun giteaAssetStaysDirectOnly() {
        val url = "https://gitea.com/LittleSurvival/ymb-apk-release/releases/download/8/yamibo.apk"

        assertEquals(listOf(url), resolveApkDownloadUrls(url))
        assertEquals(listOf(url), resolveApkDownloadUrls(url, AppUpdateDownloadMode.PROXY))
    }

    @Test
    fun blankUrlHasNoCandidates() {
        assertEquals(emptyList(), resolveApkDownloadUrls("  "))
    }

    @Test
    fun nonHttpUrlIsUsedAsIs() {
        val url = "content://updates/yamibo.apk"

        assertEquals(listOf(url), resolveApkDownloadUrls(url))
    }
}
