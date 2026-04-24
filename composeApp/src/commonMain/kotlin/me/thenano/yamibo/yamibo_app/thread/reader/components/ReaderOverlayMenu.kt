package me.thenano.yamibo.yamibo_app.thread.reader.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.thread.reader.debug.DebugRecomposeProbe
import me.thenano.yamibo.yamibo_app.thread.reader.components.overlay.ReaderBottomBar
import me.thenano.yamibo.yamibo_app.thread.reader.components.overlay.ReaderFloatButtons
import me.thenano.yamibo.yamibo_app.thread.reader.components.overlay.ReaderOverlayTopBar

/**
 *  TopBar (CataLog)
 *
 *  BottomBar : (Reply, Share, Favorite)
 *
 *  Float Circle Button (Refresh & Settings, optionally Manga Reader)
 */
@Composable
internal fun ReaderOverlayMenu(
    visible: Boolean,
    title: String,
    snackbarHostState: SnackbarHostState,
    isFavorited: Boolean,
    onBack: () -> Unit,
    onCatalog: () -> Unit,
    onFavorite: () -> Unit,
    onFavoriteLongPress: (() -> Unit)? = null,
    onShare: () -> Unit,
    onReply: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    showMangaReader: Boolean = false,
    onMangaReader: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    DebugRecomposeProbe("ReaderOverlayMenu", "visible=$visible")
    Box(modifier = modifier.fillMaxSize()) {
        // Animated top bar
        ReaderOverlayTopBar(
            visible = visible,
            title = title,
            onBack = onBack,
            onCatalog = onCatalog,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Float Circle Buttons (Refresh & Settings, optionally Manga Reader)
        ReaderFloatButtons(
            visible = visible,
            onRefresh = onRefresh,
            onSettings = onSettings,
            showMangaReader = showMangaReader,
            onMangaReader = onMangaReader,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 110.dp, end = 16.dp)
        )

        // Bottom action bar (Comment, Favorite, Share)
        ReaderBottomBar(
            visible = visible,
            isFavorited = isFavorited,
            onReply = onReply,
            onFavorite = onFavorite,
            onFavoriteLongPress = onFavoriteLongPress,
            onShare = onShare,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        YamiboSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 72.dp)
        )
    }
}
