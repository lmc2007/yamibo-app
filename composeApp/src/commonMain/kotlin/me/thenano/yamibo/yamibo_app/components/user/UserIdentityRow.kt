package me.thenano.yamibo.yamibo_app.components.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.model.User
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

/**
 * Shared avatar + user name + optional time row.
 *
 * Use anywhere a Yamibo user identity appears and can navigate to UserSpace:
 * thread cards, blog cards, blog comments, private-message rows, notice rows,
 * and reader author blocks. Keeping this common prevents small differences in
 * avatar size, name color, and click target behavior.
 *
 * @param user DTO containing avatar/name/uid.
 * @param time Optional timestamp shown below the name.
 * @param avatarSize Avatar size in dp.
 * @param onUserClick Called when either avatar or name area is tapped.
 */
@Composable
fun UserIdentityRow(
    user: User,
    time: String? = null,
    avatarSize: Int = 34,
    onUserClick: (User) -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(
            url = user.avatarUrl,
            size = avatarSize,
            modifier = Modifier.clickable { onUserClick(user) },
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.clickable { onUserClick(user) }) {
            Text(user.name, color = colors.textStrong, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            time?.let { Text(it, color = colors.brownLight, fontSize = 11.sp) }
        }
    }
}
