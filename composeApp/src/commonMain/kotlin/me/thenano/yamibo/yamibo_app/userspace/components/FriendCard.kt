package me.thenano.yamibo.yamibo_app.userspace.components

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.page.UserSpaceFriendItem
import me.thenano.yamibo.yamibo_app.components.UserAvatar
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
internal fun FriendCard(
    item: UserSpaceFriendItem,
    onUserClick: () -> Unit,
    onMessageClick: (() -> Unit)?,
    onDeleteClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onUserClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(item.user.avatarUrl, size = 42)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.user.name, color = colors.brownDeep, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            item.description?.let {
                Text(it, color = colors.brownPrimary.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
        if (onMessageClick != null) {
            SmallActionButton(appString(Res.string.ui_send_message), onMessageClick)
            Spacer(Modifier.width(6.dp))
        }
        if (item.deleteUrl != null) {
            SmallActionButton(appString(Res.string.common_delete), onDeleteClick)
        }
    }
    HorizontalDivider(color = colors.brownLight.copy(alpha = 0.35f))
}

