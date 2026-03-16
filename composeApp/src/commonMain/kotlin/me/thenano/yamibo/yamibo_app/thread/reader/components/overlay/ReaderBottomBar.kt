package me.thenano.yamibo.yamibo_app.thread.reader.components.overlay

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Bottom action bar (Reply pill, Favorite, Share) */
@Composable
fun ReaderBottomBar(
    visible: Boolean,
    onReply: () -> Unit,
    onFavorite: () -> Unit,
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
}