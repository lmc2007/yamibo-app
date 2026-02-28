package me.thenano.yamibo.yamibo_app.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BottomNavItem(val title: String, val icon: ImageVector)

@Composable
fun HomePageBottomBar(
    tabs: List<BottomNavItem>,
    currentTab: BottomNavItem,
    onTabSelected: (BottomNavItem) -> Unit
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFFFFE6B7))
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = tab == currentTab
            val color by
            animateColorAsState(
                targetValue = if (selected) Color(0xFF6E2B19) else Color(0xFFD29D7C),
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )

            Column(
                modifier =
                    Modifier.weight(1f).clickable(
                        interactionSource =
                            remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Text(text = tab.title, color = color, fontSize = 12.sp)
            }
        }
    }
}

object YamiboIcons {
    val Home =
        ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        )
            .apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(10.0f, 20.0f)
                    verticalLineTo(14.0f)
                    horizontalLineTo(14.0f)
                    verticalLineTo(20.0f)
                    horizontalLineTo(19.0f)
                    verticalLineTo(12.0f)
                    horizontalLineTo(22.0f)
                    lineTo(12.0f, 3.0f)
                    lineTo(2.0f, 12.0f)
                    horizontalLineTo(5.0f)
                    verticalLineTo(20.0f)
                    horizontalLineTo(10.0f)
                    close()
                }
            }
            .build()

    val Message =
        ImageVector.Builder(
            name = "Message",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        )
            .apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(20.0f, 2.0f)
                    horizontalLineTo(4.0f)
                    curveTo(2.9f, 2.0f, 2.0f, 2.9f, 2.0f, 4.0f)
                    verticalLineTo(22.0f)
                    lineTo(6.0f, 18.0f)
                    horizontalLineTo(20.0f)
                    curveTo(21.1f, 18.0f, 22.0f, 17.1f, 22.0f, 16.0f)
                    verticalLineTo(4.0f)
                    curveTo(22.0f, 2.9f, 21.1f, 2.0f, 20.0f, 2.0f)
                    close()
                    moveTo(20.0f, 16.0f)
                    horizontalLineTo(6.0f)
                    lineTo(4.0f, 18.0f)
                    verticalLineTo(4.0f)
                    horizontalLineTo(20.0f)
                    verticalLineTo(16.0f)
                    close()
                }
            }
            .build()

    val Profile =
        ImageVector.Builder(
            name = "Profile",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        )
            .apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12.0f, 12.0f)
                    curveTo(14.21f, 12.0f, 16.0f, 10.21f, 16.0f, 8.0f)
                    curveTo(16.0f, 5.79f, 14.21f, 4.0f, 12.0f, 4.0f)
                    curveTo(9.79f, 4.0f, 8.0f, 5.79f, 8.0f, 8.0f)
                    curveTo(8.0f, 10.21f, 9.79f, 12.0f, 12.0f, 12.0f)
                    close()
                    moveTo(12.0f, 14.0f)
                    curveTo(9.33f, 14.0f, 4.0f, 15.34f, 4.0f, 18.0f)
                    verticalLineTo(20.0f)
                    horizontalLineTo(20.0f)
                    verticalLineTo(18.0f)
                    curveTo(20.0f, 15.34f, 14.67f, 14.0f, 12.0f, 14.0f)
                    close()
                }
            }
            .build()
}
