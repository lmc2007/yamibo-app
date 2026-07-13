package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.repository.inapplinknavigation.InAppLinkContext
import me.thenano.yamibo.yamibo_app.repository.inapplinknavigation.InAppLinkResolveResult
import me.thenano.yamibo.yamibo_app.repository.inapplinknavigation.InAppLinkTarget

/**
 * Resolves Yamibo HTML links into pure app-navigation targets.
 *
 * This repository performs URL parsing and, only when necessary, lightweight API probes such as
 * `fetchFindPost` or thread page 1. It deliberately does not depend on Compose or a navigator:
 * UI layers should map [InAppLinkTarget] to the existing screen classes.
 */
interface InAppLinkNavigationRepository {
    suspend fun resolve(
        url: String,
        context: InAppLinkContext = InAppLinkContext(),
        onProgress: (String) -> Unit = {},
    ): InAppLinkResolveResult
}

