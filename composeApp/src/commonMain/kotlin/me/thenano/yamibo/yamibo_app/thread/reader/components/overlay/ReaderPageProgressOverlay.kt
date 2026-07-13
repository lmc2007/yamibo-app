package me.thenano.yamibo.yamibo_app.thread.reader.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import kotlin.math.roundToInt

data class ReaderPageProgress(
    val page: Int,
    val totalPages: Int,
    val fraction: Float,
)

@Composable
fun ReaderPageProgressSlideBar(
    progress: ReaderPageProgress?,
    modifier: Modifier = Modifier,
) {
    val colors = YamiboTheme.colors
    val value = progress?.fraction?.coerceIn(0f, 1f) ?: return

    BoxWithConstraints(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight(),
    ) {
        val thumbHeight = 24.dp
        val travel = (maxHeight - thumbHeight).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = travel * value)
                .width(3.dp)
                .height(thumbHeight)
                .background(colors.textDark.copy(alpha = 0.38f)),
        )
    }
}

@Composable
fun ReaderPageProgressHint(
    progress: ReaderPageProgress?,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = YamiboTheme.colors
    val value = progress?.fraction?.coerceIn(0f, 1f) ?: return
    if (!visible) return

    Text(
        text = "${progress.page}/${progress.totalPages} ${(value * 100f).roundToInt()}%",
        color = colors.textDark.copy(alpha = 0.46f),
        fontSize = 9.sp,
        lineHeight = 10.sp,
        modifier = modifier
            .background(colors.creamBackground.copy(alpha = 0.62f))
            .padding(start = 3.dp, top = 1.dp, bottom = 1.dp),
    )
}
