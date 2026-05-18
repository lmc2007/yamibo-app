package me.thenano.yamibo.yamibo_app.message

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.page.NoticeItem
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlRenderer
import me.thenano.yamibo.yamibo_app.components.UserAvatar
import me.thenano.yamibo.yamibo_app.userspace.components.SmallActionButton

@Composable
internal fun NoticeCard(item: NoticeItem, onAction: () -> Unit) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAction,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        UserAvatar(item.avatarUrl, size = 42)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.timeInfo.text, color = colors.brownLight, fontSize = 12.sp)
            Spacer(Modifier.height(3.dp))
            HtmlRenderer(html = item.contentHtml, modifier = Modifier.fillMaxWidth())
            item.quote?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = colors.creamBackground,
                ) {
                    Text(it, modifier = Modifier.padding(10.dp), color = colors.textDark, fontSize = 13.sp)
                }
            }
        }
        SmallActionButton(appString(Res.string.ui_shield), onAction)
    }
    HorizontalDivider(color = colors.brownLight.copy(alpha = 0.35f))
}

