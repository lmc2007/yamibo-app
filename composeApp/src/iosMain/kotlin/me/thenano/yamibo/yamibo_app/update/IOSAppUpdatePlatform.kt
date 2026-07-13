package me.thenano.yamibo.yamibo_app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdateDownloadState
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdatePlatform
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdateRelease
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IOSAppUpdatePlatform : AppUpdatePlatform {
    override val currentVersionCode: Long =
        (NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String)?.toLongOrNull() ?: 0L

    override val currentVersionName: String =
        NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "0"

    override val platformKey: String = "ios"
    override val supportedAssetTypes: Set<String> = emptySet()

    override suspend fun downloadAndInstall(
        release: AppUpdateRelease,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): AppUpdateDownloadState = withContext(Dispatchers.Main) {
        openReleasePage(release.releaseUrl)
        AppUpdateDownloadState.Completed(release)
    }

    override fun cancelDownload() = Unit

    override fun openReleasePage(url: String) {
        NSURL.URLWithString(url)?.let { nsUrl ->
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
}
