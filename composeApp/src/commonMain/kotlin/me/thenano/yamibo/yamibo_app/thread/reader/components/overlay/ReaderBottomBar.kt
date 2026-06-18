package me.thenano.yamibo.yamibo_app.thread.reader.components.overlay

import me.thenano.yamibo.yamibo_app.i18n.i18n

import YamiboIcons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.favorite.FavoriteActionButton
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

@Composable
fun ReaderBottomBar(
    visible: Boolean,
    isFavorited: Boolean,
    onReply: () -> Unit,
    onFavorite: () -> Unit,
    onFavoriteLongPress: (() -> Unit)? = null,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YamiboTheme.colors
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier.fillMaxWidth()
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
                Surface(
                    onClick = onReply,
                    color = colors.creamSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    ) {
                        Text(i18n("發送回覆"), color = colors.textDark.copy(alpha = 0.72f), fontSize = 15.sp)
                    }
                }

                FavoriteActionButton(
                    onClick = onFavorite,
                    onLongClick = onFavoriteLongPress,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White,
                    iconSize = 28,
                    filled = isFavorited,
                )

                Surface(
                    onClick = onShare,
                    color = Color.Transparent
                ) {
                    Icon(
                        imageVector = YamiboIcons.Share,
                        contentDescription = i18n("分享"),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

