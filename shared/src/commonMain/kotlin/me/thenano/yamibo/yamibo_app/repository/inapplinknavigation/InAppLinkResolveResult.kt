package me.thenano.yamibo.yamibo_app.repository.inapplinknavigation

sealed interface InAppLinkResolveResult {
    data class Resolved(val target: InAppLinkTarget) : InAppLinkResolveResult
    data class Failed(val target: InAppLinkTarget, val reason: String? = null) : InAppLinkResolveResult
}