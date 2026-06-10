package me.thenano.yamibo.yamibo_app.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/**
 * Standard cream top bar for MainScreen tabs.
 *
 * Use this for top-level tabs such as Reading History, Messages, and Favorites
 * where the header sits directly below the system status bar instead of using
 * the nested brown [YamiboTopBar]. It fixes the content row to 60dp so titles,
 * chips, avatar buttons, and icon buttons keep the same vertical center while
 * switching tabs.
 *
 * @param title Main tab title.
 * @param modifier Optional root modifier.
 * @param actions Right-side actions. Keep items 36dp touch targets or compact
 * chips/buttons so they align to the standard center line.
 */
@Composable
fun YamiboMainTabTopBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = YamiboTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp),
        color = colors.creamBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textOnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}
