package me.thenano.yamibo.yamibo_app.components.controls

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

/**
 * Small numeric/text badge for unread counts or labels.
 *
 * Use beside titles in message rows or compact metadata rows where a filled
 * orange pill is expected.
 */
@Composable
fun YamiboSmallBadge(text: String) {
    val colors = YamiboTheme.colors
    Surface(shape = RoundedCornerShape(10.dp), color = colors.orangeAccent) {
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = colors.textOnAccent, fontSize = 10.sp)
    }
}
