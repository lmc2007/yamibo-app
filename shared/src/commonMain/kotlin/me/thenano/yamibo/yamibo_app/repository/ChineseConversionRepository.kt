package me.thenano.yamibo.yamibo_app.repository

import kotlinx.coroutines.flow.StateFlow
import me.thenano.yamibo.yamibo_app.repository.chineseconversion.ChineseConversionMode

/**
 * Simplified/traditional Chinese conversion entry point for app features that need text
 * normalization or display conversion.
 *
 * Usage:
 * - Inject the platform implementation through `LocalChineseConversionRepository`.
 * - Call [setConversionMode] once from settings state.
 * - Call [convert] from a coroutine. The repository applies the currently selected mode.
 * - Check [isModeAvailable] before exposing the setting on platforms whose backend may be absent.
 *
 * Current implementation policy:
 * - Android uses the bundled OpenCC-style JVM dictionary library.
 * - iOS returns the original text for now, keeping the common API ready until a native OpenCC
 *   backend is added.
 */
interface ChineseConversionRepository {
    val currentMode: StateFlow<ChineseConversionMode?>
    fun setConversionMode(mode: ChineseConversionMode?)
    suspend fun convert(text: String): String
    fun isModeAvailable(mode: ChineseConversionMode): Boolean
}
