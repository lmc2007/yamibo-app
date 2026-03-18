import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object YamiboIcons {
    val Home =
        ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
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
        }.build()

    val Message =
        ImageVector.Builder(
            name = "Message",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
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
        }.build()

    val Profile =
        ImageVector.Builder(
            name = "Profile",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
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
        }.build()

    val Search: ImageVector =
        ImageVector.Builder(
            name = "Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1f
            ) {
                moveTo(11.742f, 10.344f)
                arcTo(6.5f, 6.5f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 10.345f, y1 = 11.742f)
                horizontalLineToRelative(-0.001f)
                curveToRelative(0.03f, 0.04f, 0.062f, 0.078f, 0.098f, 0.115f)
                lineToRelative(3.85f, 3.85f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 1.415f, dy1 = -1.414f)
                lineToRelative(-3.85f, -3.85f)
                arcToRelative(1.007f, 1.007f, 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -0.115f,
                    dy1 = -0.1f
                )
                close()
                moveTo(12f, 6.5f)
                arcToRelative(5.5f, 5.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -11f, dy1 = 0f)
                arcToRelative(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 11f, dy1 = 0f)
                close()
            }
        }.build()

    val Views: ImageVector =
        ImageVector.Builder(
            name = "Views",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Outer eye shape
                moveTo(991f, 515f)
                quadToRelative(-11f, 19f, -25f, 39f)
                quadToRelative(-20f, 28f, -42f, 55f)
                quadToRelative(-27f, 33f, -57f, 63f)
                quadToRelative(-35f, 35f, -72f, 63f)
                quadToRelative(-42f, 32f, -85f, 55f)
                quadToRelative(-49f, 26f, -99f, 39f)
                quadToRelative(-55f, 15f, -111f, 15f)
                reflectiveQuadToRelative(-111f, -15f)
                quadToRelative(-50f, -13f, -99f, -39f)
                quadToRelative(-43f, -23f, -85f, -55f)
                quadToRelative(-37f, -28f, -72f, -63f)
                quadToRelative(-30f, -30f, -57f, -63f)
                quadToRelative(-22f, -27f, -42f, -55f)
                quadToRelative(-14f, -20f, -25f, -39f)
                lineToRelative(-9f, -15f)
                lineToRelative(9f, -15f)
                quadToRelative(11f, -19f, 25f, -39f)
                quadToRelative(20f, -28f, 42f, -55f)
                quadToRelative(27f, -33f, 57f, -63f)
                quadToRelative(35f, -35f, 71f, -63f)
                quadToRelative(42f, -32f, 86f, -55f)
                quadToRelative(49f, -26f, 99f, -39f)
                quadToRelative(55f, -15f, 111f, -15f)
                reflectiveQuadToRelative(111f, 15f)
                quadToRelative(50f, 13f, 99f, 39f)
                quadToRelative(44f, 23f, 86f, 55f)
                quadToRelative(36f, 28f, 71f, 63f)
                quadToRelative(30f, 30f, 57f, 63f)
                quadToRelative(22f, 27f, 42f, 55f)
                quadToRelative(14f, 20f, 25f, 39f)
                quadToRelative(5f, 8f, 9f, 15f)
                close()
                // Inner detail - top arc
                moveTo(181f, 631f)
                verticalLineToRelative(0f)
                quadToRelative(70f, 69f, 144f, 108f)
                quadToRelative(90f, 48f, 181f, 48f)
                quadToRelative(95f, 0f, 184f, -48f)
                quadToRelative(70f, -37f, 141f, -108f)
                quadToRelative(66f, -66f, 94f, -112f)
                quadToRelative(3f, -3f, 6f, -9.5f)
                reflectiveQuadToRelative(7f, -9.5f)
                quadToRelative(-52f, -71f, -107f, -125f)
                quadToRelative(-70f, -70f, -141f, -108f)
                quadToRelative(-90f, -48f, -184f, -48f)
                reflectiveQuadToRelative(-183f, 48f)
                quadToRelative(-70f, 37f, -142f, 108f)
                quadToRelative(-29f, 25f, -57f, 60f)
                quadToRelative(-20f, 23f, -49f, 65f)
                quadToRelative(57f, 81f, 106f, 131f)
                close()
                // Inner pupil ring
                moveTo(579f, 365f)
                quadToRelative(36f, 21f, 56.5f, 56.5f)
                reflectiveQuadTo(656f, 500f)
                reflectiveQuadToRelative(-21f, 78.5f)
                reflectiveQuadToRelative(-56.5f, 56.5f)
                reflectiveQuadToRelative(-78.5f, 21f)
                reflectiveQuadToRelative(-78.5f, -20.5f)
                reflectiveQuadTo(365f, 579f)
                reflectiveQuadToRelative(-21f, -79f)
                reflectiveQuadToRelative(21f, -79f)
                reflectiveQuadToRelative(56.5f, -56.5f)
                reflectiveQuadTo(500f, 344f)
                reflectiveQuadToRelative(79f, 21f)
                close()
                // Outer iris ring
                moveTo(311f, 608.5f)
                quadToRelative(30f, 50.5f, 80.5f, 80.5f)
                reflectiveQuadToRelative(109f, 30f)
                reflectiveQuadToRelative(109f, -29f)
                reflectiveQuadToRelative(79.5f, -78f)
                quadToRelative(30f, -51f, 30f, -112f)
                reflectiveQuadToRelative(-30f, -112f)
                quadToRelative(-29f, -49f, -79.5f, -78f)
                reflectiveQuadTo(500f, 281f)
                reflectiveQuadTo(390.5f, 310f)
                reflectiveQuadTo(311f, 388f)
                quadToRelative(-30f, 51f, -30f, 112f)
                quadToRelative(0f, 58f, 30f, 108.5f)
                close()
            }
        }.build()

    val Comment: ImageVector =
        ImageVector.Builder(
            name = "Comment",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2f, 5.5f)
                curveTo(2f, 4.1f, 3.1f, 3f, 4.5f, 3f)
                horizontalLineToRelative(15f)
                curveTo(20.9f, 3f, 22f, 4.1f, 22f, 5.5f)
                verticalLineToRelative(10f)
                curveToRelative(0f, 1.4f, -1.1f, 2.5f, -2.5f, 2.5f)
                horizontalLineToRelative(-3.1f)
                curveToRelative(-0.4f, 0f, -0.8f, 0.2f, -1f, 0.5f)
                lineTo(13f, 21.6f)
                curveToRelative(-0.4f, 0.6f, -1.2f, 0.7f, -1.8f, 0.3f)
                lineToRelative(-0.3f, -0.3f)
                lineToRelative(-2.4f, -3.2f)
                curveToRelative(-0.1f, -0.2f, -0.5f, -0.4f, -0.9f, -0.4f)
                horizontalLineTo(4.5f)
                curveTo(3.1f, 18f, 2f, 16.9f, 2f, 15.5f)
                verticalLineToRelative(-10f)
                close()
                moveTo(8.2f, 10.5f)
                curveToRelative(0f, -0.7f, -0.6f, -1.3f, -1.3f, -1.3f)
                reflectiveCurveToRelative(-1.3f, 0.6f, -1.3f, 1.3f)
                reflectiveCurveToRelative(0.6f, 1.3f, 1.3f, 1.3f)
                reflectiveCurveToRelative(1.3f, -0.6f, 1.3f, -1.3f)
                close()
                moveTo(13.2f, 10.5f)
                curveToRelative(0f, -0.7f, -0.6f, -1.3f, -1.3f, -1.3f)
                reflectiveCurveToRelative(-1.3f, 0.6f, -1.3f, 1.3f)
                reflectiveCurveToRelative(0.6f, 1.3f, 1.3f, 1.3f)
                reflectiveCurveToRelative(1.3f, -0.6f, 1.3f, -1.3f)
                close()
                moveTo(17f, 11.7f)
                curveToRelative(0.7f, 0f, 1.3f, -0.6f, 1.3f, -1.3f)
                reflectiveCurveToRelative(-0.6f, -1.2f, -1.3f, -1.2f)
                reflectiveCurveToRelative(-1.3f, 0.6f, -1.3f, 1.3f)
                reflectiveCurveToRelative(0.6f, 1.2f, 1.3f, 1.2f)
                close()
            }
        }.build()

    val PersonFill: ImageVector =
        ImageVector.Builder(
            name = "PersonFill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(813f, 875f)
                lineToRelative(10f, -1f)
                quadToRelative(11f, -2f, 21f, -7f)
                quadToRelative(14f, -7f, 21f, -19f)
                quadToRelative(10f, -15f, 10f, -36f)
                quadToRelative(0f, -29f, -15f, -65f)
                quadToRelative(-19f, -46f, -55f, -83f)
                quadToRelative(-46f, -46f, -115f, -72f)
                quadToRelative(-81f, -30f, -190f, -30f)
                reflectiveQuadToRelative(-190f, 30f)
                quadToRelative(-69f, 26f, -114f, 71f)
                quadToRelative(-37f, 37f, -56f, 83f)
                quadToRelative(-15f, 36f, -15f, 66f)
                quadToRelative(0f, 22f, 10f, 36f)
                quadToRelative(8f, 12f, 22f, 19f)
                quadToRelative(9f, 5f, 21f, 7f)
                quadToRelative(5f, 1f, 10f, 1f)
                close()
                moveTo(405f, 475f)
                quadToRelative(-43f, -25f, -67.5f, -68f)
                reflectiveQuadTo(313f, 312f)
                reflectiveQuadToRelative(24.5f, -95f)
                reflectiveQuadToRelative(67.5f, -67.5f)
                reflectiveQuadToRelative(95f, -24.5f)
                reflectiveQuadToRelative(95f, 24.5f)
                reflectiveQuadToRelative(68f, 67f)
                reflectiveQuadToRelative(25f, 95.5f)
                reflectiveQuadToRelative(-25f, 96f)
                reflectiveQuadToRelative(-68f, 67.5f)
                reflectiveQuadToRelative(-95f, 24.5f)
                reflectiveQuadToRelative(-95f, -25f)
                close()
            }
        }.build()

    val StarOutline: ImageVector =
        ImageVector.Builder(
            name = "StarOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(231f, 631f)
                lineTo(13f, 419f)
                quadToRelative(-13f, -9f, -14f, -21f)
                reflectiveQuadToRelative(8f, -22.5f)
                reflectiveQuadToRelative(24f, -13.5f)
                lineToRelative(307f, -43f)
                lineToRelative(131f, -269f)
                lineToRelative(19f, -19f)
                quadToRelative(9f, -5f, 21.5f, 0f)
                reflectiveQuadTo(531f, 50f)
                lineToRelative(132f, 275f)
                lineToRelative(306f, 44f)
                quadToRelative(15f, 0f, 22.5f, 10f)
                reflectiveQuadToRelative(6.5f, 23.5f)
                reflectiveQuadToRelative(-10f, 22.5f)
                lineTo(769f, 637f)
                lineToRelative(50f, 294f)
                quadToRelative(3f, 15f, -3f, 26f)
                reflectiveQuadToRelative(-17.5f, 14.5f)
                reflectiveQuadTo(775f, 969f)
                lineTo(500f, 825f)
                lineTo(225f, 963f)
                quadToRelative(-9f, 6f, -21f, 1.5f)
                reflectiveQuadToRelative(-19f, -16f)
                reflectiveQuadToRelative(-4f, -23.5f)
                close()
                moveTo(500.5f, 752.5f)
                quadTo(508f, 754f, 513f, 762f)
                lineToRelative(231f, 119f)
                lineToRelative(-44f, -250f)
                quadToRelative(0f, -9f, 4f, -17.5f)
                reflectiveQuadToRelative(9f, -13.5f)
                lineToRelative(181f, -175f)
                lineToRelative(-256f, -38f)
                quadToRelative(-9f, 0f, -14.5f, -4f)
                reflectiveQuadTo(613f, 369f)
                lineTo(500f, 137f)
                lineTo(388f, 362f)
                quadToRelative(-6f, 10f, -11f, 14.5f)
                reflectiveQuadToRelative(-14f, 4.5f)
                lineToRelative(-257f, 38f)
                lineToRelative(182f, 175f)
                quadToRelative(8f, 4f, 11f, 13f)
                quadToRelative(1f, 5f, 1f, 18f)
                lineToRelative(-44f, 250f)
                lineToRelative(232f, -119f)
                quadToRelative(5f, -5f, 12.5f, -3.5f)
                close()
            }
        }.build()

    val StarFilled: ImageVector =
        ImageVector.Builder(
            name = "StarFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(500f, 825f)
                lineTo(781f, 963f)
                quadToRelative(9f, 6f, 21f, 1.5f)
                reflectiveQuadToRelative(19f, -16f)
                reflectiveQuadToRelative(4f, -23.5f)
                lineToRelative(-50f, -294f)
                lineToRelative(219f, -212f)
                quadToRelative(9f, -9f, 9f, -21f)
                reflectiveQuadToRelative(-8f, -22.5f)
                reflectiveQuadToRelative(-20f, -13.5f)
                lineToRelative(-306f, -43f)
                lineTo(531f, 50f)
                quadToRelative(-6f, -12f, -17f, -16.5f)
                reflectiveQuadToRelative(-22f, 0f)
                reflectiveQuadTo(475f, 50f)
                lineTo(338f, 319f)
                lineTo(31f, 362f)
                quadToRelative(-15f, 3f, -24f, 13.5f)
                reflectiveQuadTo(-1f, 398f)
                reflectiveQuadToRelative(14f, 21f)
                lineToRelative(218f, 212f)
                lineToRelative(-50f, 294f)
                quadToRelative(-3f, 15f, 3f, 26f)
                reflectiveQuadToRelative(17.5f, 14.5f)
                reflectiveQuadToRelative(23.5f, -2.5f)
                close()
            }
        }.build()

    val Share: ImageVector =
        ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 25f,
            viewportHeight = 25f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Arrow diagonal line
                moveTo(18.9697f, 4.96967f)
                lineTo(11.9697f, 11.9697f)
                lineTo(13.0303f, 13.0303f)
                lineTo(20.0303f, 6.03033f)
                close()
                // Top-right arrow vertical
                moveTo(18.75f, 5.5f)
                verticalLineTo(9.641f)
                horizontalLineTo(20.25f)
                verticalLineTo(5.5f)
                close()
                // Top-right arrow horizontal
                moveTo(19.5f, 4.75f)
                horizontalLineTo(15.412f)
                verticalLineTo(6.25f)
                horizontalLineTo(19.5f)
                close()
                // Rounded rect path
                moveTo(12.5f, 4.75f)
                horizontalLineTo(9.5f)
                curveTo(6.87665f, 4.75f, 4.75f, 6.87665f, 4.75f, 9.5f)
                verticalLineTo(15.5f)
                curveTo(4.75f, 18.1234f, 6.87665f, 20.25f, 9.5f, 20.25f)
                horizontalLineTo(15.5f)
                curveTo(18.1234f, 20.25f, 20.25f, 18.1234f, 20.25f, 15.5f)
                verticalLineTo(12.5f)
                horizontalLineTo(18.75f)
                verticalLineTo(15.5f)
                curveTo(18.75f, 17.2949f, 17.2949f, 18.75f, 15.5f, 18.75f)
                horizontalLineTo(9.5f)
                curveTo(7.70507f, 18.75f, 6.25f, 17.2949f, 6.25f, 15.5f)
                verticalLineTo(9.5f)
                curveTo(6.25f, 7.70507f, 7.70507f, 6.25f, 9.5f, 6.25f)
                horizontalLineTo(12.5f)
                close()
            }
        }.build()

    val Explore =
        ImageVector.Builder(
            name = "Explore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(14.19f, 14.19f)
                lineTo(6f, 18f)
                lineToRelative(3.81f, -8.19f)
                lineTo(18f, 6f)
                lineToRelative(-3.81f, 8.19f)
                close()
                moveTo(12f, 10.9f)
                curveToRelative(-0.61f, 0f, -1.1f, 0.49f, -1.1f, 1.1f)
                reflectiveCurveToRelative(0.49f, 1.1f, 1.1f, 1.1f)
                curveToRelative(0.61f, 0f, 1.1f, -0.49f, 1.1f, -1.1f)
                reflectiveCurveToRelative(-0.49f, -1.1f, -1.1f, -1.1f)
                close()
            }
        }.build()

    val ThreeDots =
        ImageVector.Builder(
            name = "ThreeDots",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9.5f, 13f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -3f, dy1 = 0f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = 0f)
                close()
                moveTo(9.5f, 8f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -3f, dy1 = 0f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = 0f)
                close()
                moveTo(9.5f, 3f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -3f, dy1 = 0f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = 0f)
                close()
            }
        }.build()

    val Heart =
        ImageVector.Builder(
            name = "Heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(923f, 283f)
                curveToRelative(0f, -132f, -107f, -239f, -239f, -239f)
                curveToRelative(-80f, 0f, -151f, 39f, -195f, 99f)
                curveToRelative(-44f, -60f, -115f, -99f, -195f, -99f)
                curveToRelative(-132f, 0f, -239f, 107f, -239f, 239f)
                curveToRelative(0f, 247f, 434f, 493f, 434f, 493f)
                reflectiveCurveToRelative(434f, -246f, 434f, -493f)
                close()
            }
        }.build()

    val History =
        ImageVector.Builder(
            name = "History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 512f,
            viewportHeight = 512f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Clock circle with gap (counter-clockwise arrow)
                moveTo(271.31f, 15.63f)
                curveToRelative(27f, 0.06f, 49.61f, 3.4f, 72.69f, 10.38f)
                curveToRelative(1.25f, 0.38f, 1.25f, 0.38f, 2.53f, 0.76f)
                curveToRelative(32.61f, 10.04f, 62.55f, 28.03f, 87.37f, 51.24f)
                curveToRelative(1.39f, 1.21f, 2.79f, 2.42f, 4.19f, 3.63f)
                curveToRelative(6.49f, 5.69f, 12.09f, 11.76f, 17.5f, 18.5f)
                curveToRelative(1.49f, 1.85f, 3f, 3.69f, 4.54f, 5.5f)
                curveToRelative(10.71f, 12.69f, 19.07f, 26.71f, 26.75f, 41.38f)
                curveToRelative(0.35f, 0.65f, 0.7f, 1.3f, 1.05f, 1.97f)
                curveToRelative(11.17f, 21.15f, 18.25f, 45.48f, 21.94f, 69.03f)
                curveToRelative(0.18f, 1.16f, 0.37f, 2.31f, 0.55f, 3.5f)
                curveToRelative(1.6f, 11.27f, 1.85f, 22.44f, 1.82f, 33.81f)
                curveToRelative(0f, 1.45f, 0f, 1.45f, -0.01f, 2.93f)
                curveToRelative(-0.06f, 24.07f, -3.4f, 46.68f, -10.38f, 69.75f)
                curveToRelative(-0.25f, 0.84f, -0.5f, 1.67f, -0.76f, 2.53f)
                curveToRelative(-10.04f, 32.62f, -28.03f, 62.55f, -51.24f, 87.38f)
                curveToRelative(-1.21f, 1.39f, -2.42f, 2.79f, -3.63f, 4.19f)
                curveToRelative(-5.69f, 6.49f, -11.77f, 12.09f, -18.5f, 17.5f)
                curveToRelative(-1.79f, 1.49f, -3.57f, 2.96f, -5.31f, 4.47f)
                curveToRelative(-37.16f, 31.85f, -89.81f, 51.71f, -138.84f, 52.12f)
                curveToRelative(-0.96f, 0.01f, -1.92f, 0.02f, -2.9f, 0.03f)
                curveToRelative(-57.6f, 0.47f, -111.61f, -12.88f, -156.89f, -50.27f)
                curveToRelative(-0.86f, -0.7f, -0.86f, -0.7f, -1.73f, -1.42f)
                curveToRelative(-15.54f, -12.86f, -33.12f, -28.27f, -42.27f, -46.58f)
                curveToRelative(-0.5f, -5.53f, -0.74f, -10.41f, 1.63f, -15.5f)
                curveToRelative(4.57f, -4.81f, 7.68f, -6.75f, 14.37f, -7f)
                curveToRelative(5.23f, -0.05f, 7.94f, 1.21f, 12f, 4.5f)
                curveToRelative(2.19f, 2.37f, 4.16f, 4.88f, 6.13f, 7.44f)
                curveToRelative(32f, 40.3f, 75.96f, 65.54f, 126.88f, 73.53f)
                curveToRelative(1.07f, 0.18f, 2.14f, 0.35f, 3.25f, 0.53f)
                curveToRelative(57.51f, 6.9f, 113.29f, -8.65f, 157.43f, -42.28f)
                curveToRelative(3.54f, -2.81f, 6.84f, -5.86f, 10.13f, -8.95f)
                curveToRelative(2.1f, -1.97f, 4.24f, -3.82f, 6.45f, -5.67f)
                curveToRelative(38.32f, -33.67f, 58.96f, -86.42f, 62.6f, -136.38f)
                curveToRelative(3.37f, -57.63f, -17.79f, -111.28f, -55.8f, -154.04f)
                curveToRelative(-36.15f, -39.91f, -88.34f, -61.65f, -141.53f, -64.96f)
                curveToRelative(-52.98f, -2.55f, -106.3f, 16.12f, -145.78f, 51.56f)
                curveToRelative(-0.77f, 0.72f, -1.54f, 1.44f, -2.34f, 2.18f)
                curveToRelative(-2.03f, 1.88f, -4.11f, 3.66f, -6.22f, 5.44f)
                curveToRelative(-12.93f, 11.32f, -23.12f, 25.9f, -32f, 40.56f)
                curveToRelative(-0.41f, 0.68f, -0.81f, 1.35f, -1.23f, 2.05f)
                curveToRelative(-12.21f, 20.53f, -21.95f, 44.07f, -24.76f, 67.95f)
                curveToRelative(0.55f, -0.61f, 1.09f, -1.23f, 1.65f, -1.86f)
                curveToRelative(4.86f, -5.21f, 9.03f, -9.37f, 16.36f, -10.14f)
                curveToRelative(6.42f, 0.29f, 10.56f, 2.45f, 15f, 7f)
                curveToRelative(2.08f, 3.28f, 2.33f, 6.02f, 2.38f, 9.88f)
                curveToRelative(0.03f, 1.01f, 0.06f, 2.02f, 0.09f, 3.05f)
                curveToRelative(-1.18f, 7.88f, -8.53f, 13.43f, -13.9f, 18.78f)
                curveToRelative(-0.92f, 0.92f, -1.84f, 1.84f, -2.78f, 2.78f)
                curveToRelative(-1.93f, 1.94f, -3.87f, 3.87f, -5.81f, 5.8f)
                curveToRelative(-2.48f, 2.47f, -4.95f, 4.95f, -7.42f, 7.43f)
                curveToRelative(-2.37f, 2.37f, -4.74f, 4.74f, -7.11f, 7.11f)
                curveToRelative(-0.89f, 0.89f, -1.77f, 1.78f, -2.68f, 2.7f)
                curveToRelative(-0.83f, 0.82f, -1.66f, 1.64f, -2.52f, 2.49f)
                curveToRelative(-0.73f, 0.72f, -1.45f, 1.45f, -2.2f, 2.19f)
                curveToRelative(-4.77f, 4.18f, -8.97f, 4.24f, -15.15f, 4.08f)
                curveToRelative(-4.16f, -0.42f, -5.89f, -1.35f, -8.9f, -4.29f)
                curveToRelative(-7.34f, -9.26f, -13.61f, -19.37f, -20.08f, -29.24f)
                curveToRelative(-1.87f, -2.85f, -3.75f, -5.7f, -5.62f, -8.55f)
                curveToRelative(-1.19f, -1.81f, -2.38f, -3.63f, -3.57f, -5.44f)
                curveToRelative(-0.84f, -1.28f, -0.84f, -1.28f, -1.7f, -2.58f)
                curveToRelative(-0.51f, -0.79f, -1.03f, -1.57f, -1.56f, -2.38f)
                curveToRelative(-0.68f, -1.04f, -0.68f, -1.04f, -1.38f, -2.09f)
                curveToRelative(-3.08f, -4.87f, -2.61f, -10.11f, -2.12f, -15.72f)
                curveToRelative(2.28f, -4.91f, 5.24f, -7.48f, 10f, -10f)
                curveToRelative(5.85f, -1.66f, 10.71f, -0.85f, 16f, 2f)
                curveToRelative(2.32f, 2.13f, 4.12f, 4.49f, 6f, 7f)
                curveToRelative(0.99f, 1.01f, 1.99f, 2.01f, 3.01f, 3.01f)
                curveToRelative(0.13f, -0.63f, 0.27f, -1.25f, 0.41f, -1.9f)
                curveToRelative(8.48f, -38.83f, 23.82f, -75.49f, 49.59f, -106.1f)
                curveToRelative(0.48f, -0.58f, 0.96f, -1.15f, 1.45f, -1.75f)
                curveToRelative(10.17f, -12.3f, 20.95f, -23.41f, 33.55f, -33.25f)
                curveToRelative(0.98f, -0.81f, 1.97f, -1.62f, 2.98f, -2.45f)
                curveToRelative(28.98f, -23.05f, 63.03f, -38.7f, 99.21f, -46.36f)
                curveToRelative(0.94f, -0.2f, 1.87f, -0.4f, 2.84f, -0.6f)
                curveToRelative(15.47f, -3.12f, 30.55f, -3.96f, 46.29f, -3.93f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                // Clock hands
                moveTo(270f, 104f)
                curveToRelative(4.64f, 2.15f, 8.09f, 5.2f, 10f, 10f)
                curveToRelative(0.58f, 4.57f, 0.5f, 9.11f, 0.45f, 13.72f)
                curveToRelative(0f, 1.4f, 0f, 2.8f, 0f, 4.2f)
                curveToRelative(0f, 3.79f, -0.02f, 7.58f, -0.05f, 11.37f)
                curveToRelative(-0.03f, 3.96f, -0.03f, 7.93f, -0.03f, 11.89f)
                curveToRelative(-0.01f, 7.5f, -0.04f, 15f, -0.08f, 22.5f)
                curveToRelative(-0.05f, 8.54f, -0.07f, 17.08f, -0.09f, 25.63f)
                curveToRelative(-0.04f, 17.57f, -0.11f, 35.13f, -0.2f, 52.7f)
                lineTo(280.93f, 256.55f)
                curveToRelative(2.19f, 1.12f, 4.06f, 2.39f, 6.03f, 3.85f)
                curveToRelative(0.75f, 0.55f, 1.5f, 1.11f, 2.28f, 1.68f)
                curveToRelative(1.2f, 0.9f, 1.2f, 0.9f, 2.43f, 1.81f)
                curveToRelative(0.84f, 0.62f, 1.68f, 1.25f, 2.55f, 1.89f)
                curveToRelative(6.43f, 4.77f, 12.75f, 9.67f, 19.05f, 14.61f)
                curveToRelative(5.35f, 4.19f, 10.79f, 8.24f, 16.27f, 12.25f)
                curveToRelative(7.17f, 5.24f, 14.21f, 10.6f, 21.19f, 16.08f)
                curveToRelative(4.32f, 3.39f, 8.69f, 6.68f, 13.12f, 9.92f)
                curveToRelative(10.1f, 7.62f, 10.1f, 7.62f, 11.19f, 14.94f)
                curveToRelative(0.31f, 6.07f, -1.49f, 10.01f, -5.06f, 14.88f)
                curveToRelative(-4.02f, 2.99f, -7.91f, 4.24f, -12.94f, 4.18f)
                curveToRelative(-7.03f, -1.44f, -11.93f, -4.8f, -17.38f, -9.31f)
                curveToRelative(-1.58f, -1.27f, -3.17f, -2.54f, -4.75f, -3.81f)
                curveToRelative(-0.81f, -0.65f, -1.61f, -1.3f, -2.44f, -1.96f)
                curveToRelative(-3.84f, -3.04f, -7.78f, -5.92f, -11.74f, -8.79f)
                curveToRelative(-6.79f, -4.96f, -13.51f, -10.01f, -20.19f, -15.13f)
                curveToRelative(-6.62f, -5.07f, -13.27f, -10.07f, -20f, -15f)
                curveToRelative(-2.33f, -1.72f, -4.67f, -3.43f, -7f, -5.15f)
                curveToRelative(-1.77f, -1.31f, -3.55f, -2.62f, -5.33f, -3.93f)
                curveToRelative(-2.9f, -2.13f, -5.79f, -4.28f, -8.69f, -6.43f)
                curveToRelative(-0.9f, -0.66f, -1.81f, -1.32f, -2.74f, -2f)
                curveToRelative(-0.84f, -0.63f, -1.68f, -1.26f, -2.55f, -1.91f)
                curveToRelative(-0.74f, -0.55f, -1.49f, -1.1f, -2.25f, -1.67f)
                curveToRelative(-3.26f, -3.19f, -4.8f, -6.56f, -5.35f, -11.08f)
                curveToRelative(0f, -1.14f, 0f, -2.28f, 0f, -3.45f)
                curveToRelative(-0.01f, -1.31f, -0.02f, -2.62f, -0.03f, -3.97f)
                curveToRelative(0f, -1.44f, 0.01f, -2.88f, 0.01f, -4.33f)
                curveToRelative(-0.01f, -1.51f, -0.02f, -3.03f, -0.03f, -4.55f)
                curveToRelative(-0.02f, -4.14f, -0.02f, -8.28f, -0.01f, -12.42f)
                curveToRelative(0f, -3.46f, -0.01f, -6.92f, -0.01f, -10.38f)
                curveToRelative(-0.01f, -8.16f, -0.01f, -16.32f, 0f, -24.49f)
                curveToRelative(0.01f, -8.41f, 0f, -16.83f, -0.03f, -25.24f)
                curveToRelative(-0.02f, -7.23f, -0.03f, -14.46f, -0.02f, -21.69f)
                curveToRelative(0f, -4.32f, 0f, -8.63f, -0.02f, -12.95f)
                curveToRelative(-0.02f, -4.06f, -0.01f, -8.12f, 0.01f, -12.17f)
                curveToRelative(0f, -1.49f, 0f, -2.97f, -0.01f, -4.46f)
                curveToRelative(-0.01f, -2.03f, 0f, -4.07f, 0.01f, -6.1f)
                curveToRelative(0f, -1.71f, 0f, -1.71f, 0f, -3.45f)
                curveToRelative(0.79f, -5.01f, 3.39f, -8.1f, 7f, -11.53f)
                curveToRelative(5.17f, -2.4f, 9.9f, -2.38f, 15.5f, -1.63f)
                close()
            }
        }.build()

    val Trashcan: ImageVector =
        ImageVector.Builder(
            name = "Trashcan",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 512f,
            viewportHeight = 512f
        ).apply {
            group(
                translationX = 64f,
                translationY = 42.666668f
            ) {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(256f, 42.666668f)
                    lineTo(128f, 42.666668f)
                    lineTo(128f, 0f)
                    lineTo(256f, 0f)
                    lineTo(256f, 42.666668f)
                    close()
                    moveTo(170.66667f, 170.66667f)
                    lineTo(128f, 170.66667f)
                    lineTo(128f, 341.33334f)
                    lineTo(170.66667f, 341.33334f)
                    lineTo(170.66667f, 170.66667f)
                    close()
                    moveTo(256f, 170.66667f)
                    lineTo(213.33333f, 170.66667f)
                    lineTo(213.33333f, 341.33334f)
                    lineTo(256f, 341.33334f)
                    lineTo(256f, 170.66667f)
                    close()
                    moveTo(384f, 85.333336f)
                    lineTo(384f, 128f)
                    lineTo(341.33334f, 128f)
                    lineTo(341.33334f, 426.66666f)
                    lineTo(42.666668f, 426.66666f)
                    lineTo(42.666668f, 128f)
                    lineTo(0f, 128f)
                    lineTo(0f, 85.333336f)
                    lineTo(384f, 85.333336f)
                    close()
                    moveTo(298.66666f, 128f)
                    lineTo(85.333336f, 128f)
                    lineTo(85.333336f, 384f)
                    lineTo(298.66666f, 384f)
                    lineTo(298.66666f, 128f)
                    close()
                }
            }
        }.build()

    val Reload =
        ImageVector.Builder(
            name = "Reload",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.0f, 3.0f)
                verticalLineTo(8.0f)
                moveTo(3.0f, 8.0f)
                horizontalLineTo(8.0f)
                moveTo(3.0f, 8.0f)
                lineTo(6.0f, 5.2917f)
                curveTo(7.5923f, 3.8666f, 9.6949f, 3.0f, 12.0f, 3.0f)
                curveTo(16.9706f, 3.0f, 21.0f, 7.0294f, 21.0f, 12.0f)
                curveTo(21.0f, 16.9706f, 16.9706f, 21.0f, 12.0f, 21.0f)
                curveTo(7.7168f, 21.0f, 4.1325f, 18.008f, 3.223f, 14.0f)
            }
        }.build()

    val Reply =
        ImageVector.Builder(
            name = "Reply",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.0f, 17.0f)
                verticalLineTo(15.8f)
                curveTo(20.0f, 14.1198f, 20.0f, 13.2798f, 19.673f, 12.638f)
                curveTo(19.3854f, 12.0735f, 18.9265f, 11.6146f, 18.362f, 11.327f)
                curveTo(17.7202f, 11.0f, 16.8802f, 11.0f, 15.2f, 11.0f)
                horizontalLineTo(4.0f)
                moveTo(4.0f, 11.0f)
                lineTo(8.0f, 7.0f)
                moveTo(4.0f, 11.0f)
                lineTo(8.0f, 15.0f)
            }
        }.build()

    val Setting =
        ImageVector.Builder(
            name = "Setting",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.0175f, 19.0f)
                curveTo(10.6601f, 19.0f, 10.3552f, 18.7347f, 10.297f, 18.373f)
                curveTo(10.2434f, 18.0804f, 10.038f, 17.8413f, 9.7617f, 17.75f)
                curveTo(9.5366f, 17.6707f, 9.3165f, 17.5772f, 9.1026f, 17.47f)
                curveTo(8.8482f, 17.3365f, 8.5429f, 17.3565f, 8.307f, 17.522f)
                curveTo(8.0216f, 17.7325f, 7.6294f, 17.6999f, 7.3808f, 17.445f)
                lineTo(6.4136f, 16.453f)
                curveTo(6.1533f, 16.186f, 6.1194f, 15.7651f, 6.3336f, 15.458f)
                curveTo(6.4988f, 15.2105f, 6.5226f, 14.8914f, 6.396f, 14.621f)
                curveTo(6.3126f, 14.4332f, 6.2391f, 14.2409f, 6.1757f, 14.045f)
                curveTo(6.0849f, 13.7363f, 5.8342f, 13.5051f, 5.5253f, 13.445f)
                curveTo(5.1529f, 13.384f, 4.8779f, 13.0559f, 4.875f, 12.669f)
                verticalLineTo(11.428f)
                curveTo(4.873f, 10.9821f, 5.1871f, 10.6007f, 5.616f, 10.528f)
                curveTo(5.9414f, 10.4645f, 6.2132f, 10.2359f, 6.3375f, 9.921f)
                curveTo(6.3746f, 9.8323f, 6.4136f, 9.7443f, 6.4545f, 9.657f)
                curveTo(6.6199f, 9.3304f, 6.5971f, 8.9371f, 6.395f, 8.633f)
                curveTo(6.1424f, 8.2729f, 6.1812f, 7.7781f, 6.4867f, 7.464f)
                lineTo(7.1975f, 6.735f)
                curveTo(7.548f, 6.3753f, 8.1009f, 6.3288f, 8.504f, 6.625f)
                lineTo(8.5264f, 6.641f)
                curveTo(8.8274f, 6.8488f, 9.2103f, 6.8864f, 9.5443f, 6.741f)
                curveTo(9.9016f, 6.6091f, 10.1649f, 6.2942f, 10.2375f, 5.912f)
                lineTo(10.2473f, 5.878f)
                curveTo(10.3275f, 5.372f, 10.7536f, 5.0002f, 11.2535f, 5.0f)
                horizontalLineTo(12.1115f)
                curveTo(12.6248f, 4.9998f, 13.0629f, 5.3806f, 13.1469f, 5.9f)
                lineTo(13.1625f, 5.97f)
                curveTo(13.2314f, 6.3362f, 13.4811f, 6.6392f, 13.8216f, 6.77f)
                curveTo(14.1498f, 6.9145f, 14.5272f, 6.8767f, 14.822f, 6.67f)
                lineTo(14.8707f, 6.634f)
                curveTo(15.2842f, 6.3283f, 15.8528f, 6.3754f, 16.2133f, 6.745f)
                lineTo(16.8675f, 7.417f)
                curveTo(17.1954f, 7.7552f, 17.2366f, 8.2869f, 16.965f, 8.674f)
                curveTo(16.7522f, 8.9975f, 16.7251f, 9.4133f, 16.8938f, 9.763f)
                lineTo(16.9358f, 9.863f)
                curveTo(17.0724f, 10.2045f, 17.3681f, 10.452f, 17.7216f, 10.521f)
                curveTo(18.1837f, 10.5983f, 18.5235f, 11.0069f, 18.525f, 11.487f)
                verticalLineTo(12.6f)
                curveTo(18.5249f, 13.0234f, 18.2263f, 13.3846f, 17.8191f, 13.454f)
                curveTo(17.4842f, 13.5199f, 17.2114f, 13.7686f, 17.1083f, 14.102f)
                curveTo(17.0628f, 14.2353f, 17.0121f, 14.3687f, 16.9562f, 14.502f)
                curveTo(16.8261f, 14.795f, 16.855f, 15.1364f, 17.0323f, 15.402f)
                curveTo(17.2662f, 15.7358f, 17.2299f, 16.1943f, 16.9465f, 16.485f)
                lineTo(16.0388f, 17.417f)
                curveTo(15.7792f, 17.6832f, 15.3698f, 17.7175f, 15.0716f, 17.498f)
                curveTo(14.8226f, 17.3235f, 14.5001f, 17.3043f, 14.2331f, 17.448f)
                curveTo(14.0428f, 17.5447f, 13.8475f, 17.6305f, 13.6481f, 17.705f)
                curveTo(13.3692f, 17.8037f, 13.1636f, 18.0485f, 13.1099f, 18.346f)
                curveTo(13.053f, 18.7203f, 12.7401f, 18.9972f, 12.3708f, 19.0f)
                horizontalLineTo(11.0175f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.9747f, 12.0f)
                curveTo(13.9747f, 13.2885f, 12.9563f, 14.333f, 11.7f, 14.333f)
                curveTo(10.4437f, 14.333f, 9.42533f, 13.2885f, 9.42533f, 12.0f)
                curveTo(9.42533f, 10.7115f, 10.4437f, 9.66699f, 11.7f, 9.66699f)
                curveTo(12.9563f, 9.66699f, 13.9747f, 10.7115f, 13.9747f, 12.0f)
                close()
            }
        }.build()

    val Book =
        ImageVector.Builder(
            name = "Book",
            defaultWidth = 16.0.dp,
            defaultHeight = 16.0.dp,
            viewportWidth = 16.0f,
            viewportHeight = 16.0f
        ).apply {
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
                moveTo(15.0f, 4.7f)
                verticalLineToRelative(-0.7f)
                curveToRelative(-1.159f, -1.163f, -2.734f, -1.91f, -4.484f, -1.999f)
                curveToRelative(-0.112f, -0.012f, -0.222f, -0.018f, -0.334f, -0.018f)
                curveToRelative(-0.874f, 0.0f, -1.657f, 0.394f, -2.179f, 1.013f)
                curveToRelative(-0.556f, -0.617f, -1.357f, -1.007f, -2.249f, -1.007f)
                curveToRelative(-0.09f, 0.0f, -0.178f, 0.004f, -0.266f, 0.012f)
                curveToRelative(-1.754f, 0.089f, -3.33f, 0.836f, -4.488f, 1.999f)
                lineToRelative(0.0f, 0.7f)
                lineToRelative(-1.0f, 0.3f)
                verticalLineToRelative(10.0f)
                lineToRelative(6.7f, -1.4f)
                lineToRelative(0.3f, 0.4f)
                horizontalLineToRelative(2.0f)
                lineToRelative(0.3f, -0.4f)
                lineToRelative(6.7f, 1.4f)
                verticalLineToRelative(-10.0f)
                close()
                moveTo(5.48f, 11.31f)
                curveToRelative(-1.275f, 0.037f, -2.467f, 0.358f, -3.526f, 0.902f)
                lineToRelative(0.046f, -7.792f)
                curveToRelative(0.885f, -0.835f, 2.066f, -1.365f, 3.369f, -1.42f)
                curveToRelative(0.806f, 0.054f, 1.534f, 0.303f, 2.159f, 0.701f)
                lineToRelative(-0.019f, 7.869f)
                curveToRelative(-0.555f, -0.166f, -1.193f, -0.262f, -1.854f, -0.262f)
                curveToRelative(-0.062f, 0.0f, -0.124f, 0.001f, -0.185f, 0.003f)
                close()
                moveTo(14.0f, 12.19f)
                curveToRelative(-1.013f, -0.522f, -2.205f, -0.843f, -3.468f, -0.88f)
                curveToRelative(-0.056f, -0.001f, -0.108f, -0.002f, -0.161f, -0.002f)
                curveToRelative(-0.66f, 0.0f, -1.297f, 0.096f, -1.899f, 0.274f)
                lineToRelative(0.047f, -7.902f)
                curveToRelative(0.601f, -0.381f, 1.322f, -0.627f, 2.096f, -0.679f)
                curveToRelative(1.324f, 0.055f, 2.501f, 0.586f, 3.386f, 1.422f)
                lineToRelative(-0.003f, 7.768f)
                close()
            }
        }.build()

    val Copy: ImageVector =
        ImageVector.Builder(
            name = "Copy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.0f, 11.0f)
                curveTo(6.0f, 8.172f, 6.0f, 6.757f, 6.879f, 5.879f)
                curveTo(7.757f, 5.0f, 9.172f, 5.0f, 12.0f, 5.0f)
                horizontalLineTo(15.0f)
                curveTo(17.828f, 5.0f, 19.243f, 5.0f, 20.121f, 5.879f)
                curveTo(21.0f, 6.757f, 21.0f, 8.172f, 21.0f, 11.0f)
                verticalLineTo(16.0f)
                curveTo(21.0f, 18.828f, 21.0f, 20.243f, 20.121f, 21.121f)
                curveTo(19.243f, 22.0f, 17.828f, 22.0f, 15.0f, 22.0f)
                horizontalLineTo(12.0f)
                curveTo(9.172f, 22.0f, 7.757f, 22.0f, 6.879f, 21.121f)
                curveTo(6.0f, 20.243f, 6.0f, 18.828f, 6.0f, 16.0f)
                verticalLineTo(11.0f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.0f, 19.0f)
                curveTo(4.343f, 19.0f, 3.0f, 17.657f, 3.0f, 16.0f)
                verticalLineTo(10.0f)
                curveTo(3.0f, 6.229f, 3.0f, 4.343f, 4.172f, 3.172f)
                curveTo(5.343f, 2.0f, 7.229f, 2.0f, 11.0f, 2.0f)
                horizontalLineTo(15.0f)
                curveTo(16.657f, 2.0f, 18.0f, 3.343f, 18.0f, 5.0f)
            }
        }.build()

    val Save: ImageVector =
        ImageVector.Builder(
            name = "Save",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.0f, 21.0f)
                lineTo(7.0f, 21.0f)
                moveTo(17.0f, 21.0f)
                lineTo(17.803f, 21.0f)
                curveTo(18.921f, 21.0f, 19.48f, 21.0f, 19.907f, 20.782f)
                curveTo(20.284f, 20.591f, 20.591f, 20.284f, 20.782f, 19.908f)
                curveTo(21.0f, 19.481f, 21.0f, 18.921f, 21.0f, 17.803f)
                verticalLineTo(9.22f)
                curveTo(21.0f, 8.771f, 21.0f, 8.545f, 20.952f, 8.331f)
                curveTo(20.91f, 8.14f, 20.839f, 7.957f, 20.743f, 7.786f)
                curveTo(20.637f, 7.597f, 20.487f, 7.431f, 20.193f, 7.104f)
                lineTo(17.438f, 4.042f)
                curveTo(17.097f, 3.664f, 16.924f, 3.472f, 16.717f, 3.334f)
                curveTo(16.53f, 3.21f, 16.324f, 3.119f, 16.107f, 3.063f)
                curveTo(15.863f, 3.0f, 15.6f, 3.0f, 15.075f, 3.0f)
                horizontalLineTo(6.2f)
                curveTo(5.08f, 3.0f, 4.52f, 3.0f, 4.092f, 3.218f)
                curveTo(3.715f, 3.41f, 3.41f, 3.715f, 3.218f, 4.092f)
                curveTo(3.0f, 4.52f, 3.0f, 5.08f, 3.0f, 6.2f)
                verticalLineTo(17.8f)
                curveTo(3.0f, 18.92f, 3.0f, 19.48f, 3.218f, 19.907f)
                curveTo(3.41f, 20.284f, 3.715f, 20.591f, 4.092f, 20.782f)
                curveTo(4.519f, 21.0f, 5.079f, 21.0f, 6.197f, 21.0f)
                horizontalLineTo(7.0f)
                moveTo(17.0f, 21.0f)
                verticalLineTo(17.197f)
                curveTo(17.0f, 16.079f, 17.0f, 15.519f, 16.782f, 15.092f)
                curveTo(16.591f, 14.716f, 16.284f, 14.41f, 15.907f, 14.218f)
                curveTo(15.48f, 14.0f, 14.92f, 14.0f, 13.8f, 14.0f)
                horizontalLineTo(10.2f)
                curveTo(9.08f, 14.0f, 8.52f, 14.0f, 8.092f, 14.218f)
                curveTo(7.715f, 14.41f, 7.41f, 14.716f, 7.218f, 15.092f)
                curveTo(7.0f, 15.52f, 7.0f, 16.08f, 7.0f, 17.2f)
                verticalLineTo(21.0f)
                moveTo(15.0f, 7.0f)
                horizontalLineTo(9.0f)
            }
        }.build()
}
