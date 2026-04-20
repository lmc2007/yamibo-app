package me.thenano.yamibo.yamibo_app.util

import coil3.PlatformContext
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

private fun UIViewController.topMostViewController(): UIViewController {
    var current = this
    while (current.presentedViewController != null) {
        current = current.presentedViewController!!
    }
    return current
}

actual fun shareText(context: PlatformContext, text: String, title: String?) {
    val activityViewController = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    rootViewController.topMostViewController()
        .presentViewController(activityViewController, animated = true, completion = null)
}
