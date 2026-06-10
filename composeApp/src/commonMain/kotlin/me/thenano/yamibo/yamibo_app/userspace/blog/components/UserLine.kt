package me.thenano.yamibo.yamibo_app.userspace.blog.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import io.github.littlesurvival.dto.model.User
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.components.user.UserAvatar

@Composable
internal fun UserLine(user: User, time: String?, onUserClick: (User) -> Unit) {
    val colors = YamiboTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable { onUserClick(user) }) {
            UserAvatar(user.avatarUrl, size = 34)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.clickable { onUserClick(user) }) {
            Text(user.name, color = colors.textStrong, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            time?.let { Text(it, color = colors.brownLight, fontSize = 11.sp) }
        }
    }
}
