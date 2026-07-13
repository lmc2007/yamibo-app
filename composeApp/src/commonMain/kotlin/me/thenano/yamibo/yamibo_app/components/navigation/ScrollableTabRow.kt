package me.thenano.yamibo.yamibo_app.components.navigation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

@Composable
internal fun YamiboScrollableTabRow(
    selectedIndex: Int,
    content: @Composable () -> Unit,
) {
    val colors = YamiboTheme.colors
    @Suppress("DEPRECATION")
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = colors.creamSurface,
        contentColor = colors.textStrong,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = colors.textStrong,
                    height = 2.dp,
                )
            }
        },
        divider = {
            HorizontalDivider(color = colors.brownLight.copy(alpha = 0.45f))
        },
    ) {
        content()
    }
}
