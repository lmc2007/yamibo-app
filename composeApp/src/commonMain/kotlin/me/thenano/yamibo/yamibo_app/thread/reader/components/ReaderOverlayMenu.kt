package me.thenano.yamibo.yamibo_app.thread.reader.components

import YamiboIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.novel.components.ThreadTopBar

/** TopBar (CataLog), BottomBar : (Reply, Share, Favorite) , Float Circle Button (Refresh & Settings) */
@Composable
internal fun ReaderOverlayMenu(
    visible: Boolean,
    title: String,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCatalog: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onReply: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YamiboTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        // Animated top bar
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
        ) {
            Surface(
                color = colors.brownDeep,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ThreadTopBar(
                    title = title,
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = onCatalog) {
                            Text("☰", color = Color.White, fontSize = 22.sp)
                        }
                    },
                    windowInsets = WindowInsets.statusBars
                )
            }
        }

        /**
         * Float Circle Button (Refresh & Settings)
         */
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 110.dp, end = 16.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Refresh
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(56.dp)
                        .background(colors.brownPrimary.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(imageVector = YamiboIcons.Reload, contentDescription = "重新整理", tint = colors.brownPrimary, modifier = Modifier.size(24.dp))
                }
                // Settings
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .size(56.dp)
                        .background(colors.brownPrimary.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(imageVector = YamiboIcons.Setting, contentDescription = "設定", tint = colors.brownPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Bottom action bar
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            Surface(
                color = colors.brownDeep,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Center: Reply (Pill shape)
                    Surface(
                        onClick = onReply,
                        color = colors.creamSurface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                        ) {
                            Text("發表回復", color = colors.textDark.copy(alpha = 0.7f), fontSize = 15.sp)
                        }
                    }
                    // Favorite & Share (IconButtons)
                    IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = YamiboIcons.StarOutline, contentDescription = "收藏", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = YamiboIcons.Share, contentDescription = "分享", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 72.dp)
        )
    }
}
