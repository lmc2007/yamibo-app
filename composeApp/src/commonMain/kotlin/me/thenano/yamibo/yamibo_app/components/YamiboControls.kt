package me.thenano.yamibo.yamibo_app.components

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextAlign
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/**
 * Reusable low-emphasis filter/sort chip.
 *
 * Use for option triggers such as Favorite sort/layout, Forum filter/order,
 * UserSpace blog filters, reader settings chips, and other compact controls
 * that open a dialog or immediately select a mode. For primary actions, use
 * [YamiboPrimaryButton] instead.
 *
 * @param text Label rendered inside the chip.
 * @param onClick Called when the chip is tapped.
 * @param selected Whether the chip should use the selected/brown-filled style.
 */
@Composable
fun YamiboActionChip(
    text: String,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.brownDeep else colors.brownPrimary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.12f)),
    ) {
        Text(
            text,
            color = if (selected) Color.White else colors.brownDeep,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

/**
 * Small inline action button used inside cards and list rows.
 *
 * Use for secondary object actions such as appString(Res.string.ui_send_message), appString(Res.string.common_delete), appString(Res.string.ui_shield),
 * appString(Res.string.main_favorite), appString(Res.string.ui_share), or appString(Res.string.ui_invite). It is intentionally smaller than full-width
 * submit buttons and should not be used as the main page CTA.
 *
 * @param text Button label.
 * @param onClick Action invoked on tap.
 */
@Composable
fun YamiboSmallActionButton(text: String, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(onClick = onClick, shape = RoundedCornerShape(5.dp), color = colors.orangeAccent) {
        Text(text, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = Color.White, fontSize = 12.sp)
    }
}

/**
 * Primary full-width or inline submit button.
 *
 * Use for form submission actions such as posting a blog comment, sending a
 * private message, or confirming a page-level action. The caller controls the
 * outer size through [modifier]; pass `Modifier.fillMaxWidth()` for full-width
 * form buttons.
 *
 * @param text Text shown when not busy.
 * @param busyText Text shown while [busy] is true.
 * @param enabled Whether the button is interactive.
 * @param busy Whether the button is currently processing.
 * @param onClick Action invoked when tapped and enabled.
 * @param fillContentWidth Set true when [modifier] is full width and the text
 * should be visually centered across the entire button.
 */
@Composable
fun YamiboPrimaryButton(
    text: String,
    busyText: String,
    enabled: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillContentWidth: Boolean = false,
) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (enabled && !busy) colors.brownDeep else colors.brownLight.copy(alpha = 0.5f),
    ) {
        Text(
            text = if (busy) busyText else text,
            modifier = (if (fillContentWidth) Modifier.fillMaxWidth() else Modifier)
                .padding(horizontal = 18.dp, vertical = 13.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Compact icon + value badge for counts.
 *
 * Use for view/reply/read counts in thread cards, blog readers, and other
 * metadata rows. This keeps count presentation icon-based instead of text-only.
 *
 * @param icon Yamibo icon for the metric.
 * @param value Preformatted metric value.
 */
@Composable
fun YamiboStatBadge(icon: ImageVector, value: String) {
    val colors = YamiboTheme.colors
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(0.8.dp, colors.brownPrimary.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = colors.brownPrimary.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            Text(value, color = colors.brownPrimary.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

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
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp)
    }
}

