package me.thenano.yamibo.yamibo_app.forum.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object YamiboIcons {
    val Search: ImageVector
        get() =
            ImageVector.Builder(
                name = "Search",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(11.742f, 10.344f)
                        arcTo(6.5f, 6.5f, 0f, true, false, 10.345f, 11.742f)
                        horizontalLineToRelative(-0.001f)
                        curveToRelative(0.03f, 0.04f, 0.062f, 0.078f, 0.098f, 0.115f)
                        lineToRelative(3.85f, 3.85f)
                        arcToRelative(1f, 1f, 0f, false, false, 1.415f, -1.414f)
                        lineToRelative(-3.85f, -3.85f)
                        arcToRelative(1.007f, 1.007f, 0f, false, false, -0.115f, -0.1f)
                        close()
                        moveTo(12f, 6.5f)
                        arcToRelative(5.5f, 5.5f, 0f, true, true, -11f, 0f)
                        arcToRelative(5.5f, 5.5f, 0f, false, true, 11f, 0f)
                        close()
                    }
                }
                .build()

    val Views: ImageVector
        get() =
            ImageVector.Builder(
                name = "Views",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 64f,
                viewportHeight = 64f
            )
                .apply {
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineJoin = StrokeJoin.Miter
                    ) {
                        moveTo(1f, 32f)
                        curveToRelative(0f, 0f, 11f, 15f, 31f, 15f)
                        reflectiveCurveToRelative(31f, -15f, 31f, -15f)
                        reflectiveCurveTo(52f, 17f, 32f, 17f)
                        reflectiveCurveTo(1f, 32f, 1f, 32f)
                        close()
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineJoin = StrokeJoin.Miter
                    ) {
                        moveTo(32f + 7f, 32f)
                        arcToRelative(7f, 7f, 0f, true, true, -14f, 0f)
                        arcToRelative(7f, 7f, 0f, true, true, 14f, 0f)
                        close()
                    }
                }
                .build()

    val Comment: ImageVector
        get() =
            ImageVector.Builder(
                name = "Comment",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            )
                .apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(21.99f, 4f)
                        curveToRelative(0f, -1.1f, -0.89f, -2f, -1.99f, -2f)
                        horizontalLineTo(4f)
                        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                        verticalLineToRelative(12f)
                        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                        horizontalLineToRelative(14f)
                        lineToRelative(4f, 4f)
                        lineToRelative(-0.01f, -18f)
                        close()
                    }
                }
                .build()
}
