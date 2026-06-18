package me.thenano.yamibo.yamibo_app.components.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

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
            color = colors.textOnDeepHigh,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}
