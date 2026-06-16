package me.thenano.yamibo.yamibo_app.components.navigation

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/**
 * Standard icon action for [YamiboMainTabTopBar].
 *
 * Use this for main-tab top-bar icons such as search, delete, refresh, or menu.
 * It keeps a 36dp touch target and a centered icon so tab switches do not cause
 * action controls to visually jump.
 *
 * @param icon Icon vector to draw.
 * @param contentDescription Accessibility label for the action.
 * @param onClick Action invoked when tapped.
 * @param iconSize Visual icon size in dp.
 * @param iconOffsetY Extra vertical optical adjustment for icons whose vector
 * bounds are visually off-center, such as search.
 */
@Composable
fun YamiboMainTabIconAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    iconSize: Int = 27,
    iconOffsetY: Int = 0,
    tint: Color = YamiboTheme.colors.brownDeep,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(iconSize.dp)
                .offset(y = iconOffsetY.dp),
        )
    }
}
