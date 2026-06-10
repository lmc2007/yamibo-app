package me.thenano.yamibo.yamibo_app.components.controls

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/**
 * Small inline action button used inside cards and list rows.
 *
 * Use for secondary object actions such as send message, delete, shield,
 * favorite, share, or invite. It is intentionally smaller than full-width
 * submit buttons and should not be used as the main page CTA.
 *
 * @param text Button label.
 * @param onClick Action invoked on tap.
 */
@Composable
fun YamiboSmallActionButton(text: String, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(onClick = onClick, shape = RoundedCornerShape(5.dp), color = colors.orangeAccent) {
        Text(text, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = colors.textOnAccent, fontSize = 12.sp)
    }
}
