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
    val Language: ImageVector by lazy { buildLanguage() }
    val Backup: ImageVector by lazy { buildBackup() }
    val Statistics: ImageVector by lazy { buildStatistics() }
    val Bookmark: ImageVector by lazy { buildBookmark() }
    val Storage: ImageVector by lazy { buildStorage() }
    val Home: ImageVector by lazy { buildHome() }
    val Message: ImageVector by lazy { buildMessage() }
    val Profile: ImageVector by lazy { buildProfile() }
    val EditOrSign: ImageVector by lazy { buildEditOrSign() }
    val Search: ImageVector by lazy { buildSearch() }
    val Plus: ImageVector by lazy { buildPlus() }
    val Views: ImageVector by lazy { buildViews() }
    val Comment: ImageVector by lazy { buildComment() }
    val PersonFill: ImageVector by lazy { buildPersonFill() }
    val StarOutline: ImageVector by lazy { buildStarOutline() }
    val StarFilled: ImageVector by lazy { buildStarFilled() }
    val Share: ImageVector by lazy { buildShare() }
    val Explore: ImageVector by lazy { buildExplore() }
    val ThreeDots: ImageVector by lazy { buildThreeDots() }
    val Heart: ImageVector by lazy { buildHeart() }
    val History: ImageVector by lazy { buildHistory() }
    val New: ImageVector by lazy { buildNew() }
    val FilterList: ImageVector by lazy { buildFilterList() }
    val CheckboxEnabled: ImageVector by lazy { buildCheckboxEnabled() }
    val CheckboxDisabled: ImageVector by lazy { buildCheckboxDisabled() }
    val Trashcan: ImageVector by lazy { buildTrashcan() }
    val Reload: ImageVector by lazy { buildReload() }
    val Sync: ImageVector by lazy { buildSync() }
    val Reply: ImageVector by lazy { buildReply() }
    val Setting: ImageVector by lazy { buildSetting() }
    val Book: ImageVector by lazy { buildBook() }
    val Copy: ImageVector by lazy { buildCopy() }
    val Save: ImageVector by lazy { buildSave() }
    val InfoCircle: ImageVector by lazy { buildInfoCircle() }
    val ChevronUp: ImageVector by lazy { buildChevronUp() }
    val Qq: ImageVector by lazy { buildQq() }

    private fun buildBackup(): ImageVector =
        ImageVector.Builder(
            name = "Backup",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 64f,
            viewportHeight = 64f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15f, 2f)
                horizontalLineTo(49f)
                curveTo(50.1f, 2f, 51f, 2.9f, 51f, 4f)
                verticalLineTo(56f)
                curveTo(51f, 57.1f, 50.1f, 58f, 49f, 58f)
                horizontalLineTo(47f)
                verticalLineTo(62f)
                horizontalLineTo(43f)
                verticalLineTo(58f)
                horizontalLineTo(21f)
                verticalLineTo(62f)
                horizontalLineTo(17f)
                verticalLineTo(58f)
                horizontalLineTo(15f)
                curveTo(13.9f, 58f, 13f, 57.1f, 13f, 56f)
                verticalLineTo(4f)
                curveTo(13f, 2.9f, 13.9f, 2f, 15f, 2f)
                close()
                moveTo(17f, 6f)
                verticalLineTo(54f)
                horizontalLineTo(47f)
                verticalLineTo(6f)
                close()
                moveTo(24f, 10f)
                horizontalLineTo(40f)
                curveTo(41.1f, 10f, 42f, 10.9f, 42f, 12f)
                verticalLineTo(20f)
                curveTo(42f, 21.1f, 41.1f, 22f, 40f, 22f)
                horizontalLineTo(24f)
                curveTo(22.9f, 22f, 22f, 21.1f, 22f, 20f)
                verticalLineTo(12f)
                curveTo(22f, 10.9f, 22.9f, 10f, 24f, 10f)
                close()
                moveTo(26f, 14f)
                verticalLineTo(18f)
                horizontalLineTo(38f)
                verticalLineTo(14f)
                close()
                moveTo(32f, 28f)
                curveTo(38.1f, 28f, 43f, 32.9f, 43f, 39f)
                curveTo(43f, 45.1f, 38.1f, 50f, 32f, 50f)
                curveTo(25.9f, 50f, 21f, 45.1f, 21f, 39f)
                curveTo(21f, 32.9f, 25.9f, 28f, 32f, 28f)
                close()
                moveTo(32f, 32f)
                curveTo(28.1f, 32f, 25f, 35.1f, 25f, 39f)
                curveTo(25f, 42.9f, 28.1f, 46f, 32f, 46f)
                curveTo(35.9f, 46f, 39f, 42.9f, 39f, 39f)
                curveTo(39f, 35.1f, 35.9f, 32f, 32f, 32f)
                close()
                moveTo(29.2f, 35.1f)
                lineTo(32f, 37.9f)
                lineTo(34.8f, 35.1f)
                lineTo(37.6f, 37.9f)
                lineTo(34.8f, 40.7f)
                lineTo(37.6f, 43.5f)
                lineTo(34.8f, 46.3f)
                lineTo(32f, 43.5f)
                lineTo(29.2f, 46.3f)
                lineTo(26.4f, 43.5f)
                lineTo(29.2f, 40.7f)
                lineTo(26.4f, 37.9f)
                close()
            }
        }.build()

    private fun buildQq(): ImageVector =
        ImageVector.Builder(
            name = "Qq",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 192f,
            viewportHeight = 192f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 12f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(153.933f, 114.718f)
                curveToRelative(-2.953f, -9.509f, -6.349f, -17.5f, -11.573f, -30.58f)
                curveTo(143.172f, 49.786f, 128.922f, 22f, 96.101f, 22f)
                curveToRelative(-33.189f, 0f, -47.125f, 28.34f, -46.24f, 62.139f)
                curveToRelative(-5.241f, 13.097f, -8.62f, 21.033f, -11.573f, 30.579f)
                curveToRelative(-6.276f, 20.256f, -4.246f, 28.636f, -2.695f, 28.821f)
                curveToRelative(3.323f, 0.407f, 12.94f, -15.243f, 12.94f, -15.243f)
                curveToRelative(0f, 9.064f, 4.651f, 20.886f, 14.73f, 29.413f)
                curveToRelative(-4.873f, 1.499f, -15.82f, 5.532f, -13.217f, 9.953f)
                curveToRelative(2.105f, 3.57f, 36.217f, 2.275f, 46.056f, 1.165f)
                curveToRelative(9.838f, 1.11f, 43.95f, 2.405f, 46.055f, -1.165f)
                curveToRelative(2.602f, -4.403f, -8.362f, -8.454f, -13.217f, -9.953f)
                curveToRelative(10.078f, -8.546f, 14.73f, -20.367f, 14.73f, -29.413f)
                curveToRelative(0f, 0f, 9.617f, 15.65f, 12.94f, 15.243f)
                curveToRelative(1.569f, -0.203f, 3.6f, -8.583f, -2.677f, -28.821f)
                close()
            }
        }.build()

    private fun buildChevronUp(): ImageVector =
        ImageVector.Builder(
            name = "ChevronUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 15f)
                lineTo(12f, 9f)
                lineTo(18f, 15f)
            }
        }.build()

    private fun buildLanguage(): ImageVector =
        ImageVector.Builder(
            name = "Language",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.99f, 2f)
                curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveTo(6.47f, 22f, 11.99f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
                close()
                moveTo(18.92f, 8f)
                horizontalLineTo(15.97f)
                curveTo(15.65f, 6.75f, 15.19f, 5.55f, 14.59f, 4.44f)
                curveTo(16.43f, 5.07f, 17.96f, 6.35f, 18.92f, 8f)
                close()
                moveTo(12f, 4.04f)
                curveTo(12.83f, 5.24f, 13.48f, 6.57f, 13.91f, 8f)
                horizontalLineTo(10.09f)
                curveTo(10.52f, 6.57f, 11.17f, 5.24f, 12f, 4.04f)
                close()
                moveTo(4.26f, 14f)
                curveTo(4.1f, 13.36f, 4f, 12.69f, 4f, 12f)
                reflectiveCurveTo(4.1f, 10.64f, 4.26f, 10f)
                horizontalLineTo(7.64f)
                curveTo(7.56f, 10.66f, 7.5f, 11.32f, 7.5f, 12f)
                reflectiveCurveTo(7.56f, 13.34f, 7.64f, 14f)
                horizontalLineTo(4.26f)
                close()
                moveTo(5.08f, 16f)
                horizontalLineTo(8.03f)
                curveTo(8.35f, 17.25f, 8.81f, 18.45f, 9.41f, 19.56f)
                curveTo(7.57f, 18.93f, 6.04f, 17.66f, 5.08f, 16f)
                close()
                moveTo(8.03f, 8f)
                horizontalLineTo(5.08f)
                curveTo(6.04f, 6.34f, 7.57f, 5.07f, 9.41f, 4.44f)
                curveTo(8.81f, 5.55f, 8.35f, 6.75f, 8.03f, 8f)
                close()
                moveTo(12f, 19.96f)
                curveTo(11.17f, 18.76f, 10.52f, 17.43f, 10.09f, 16f)
                horizontalLineTo(13.91f)
                curveTo(13.48f, 17.43f, 12.83f, 18.76f, 12f, 19.96f)
                close()
                moveTo(14.34f, 14f)
                horizontalLineTo(9.66f)
                curveTo(9.57f, 13.34f, 9.5f, 12.68f, 9.5f, 12f)
                reflectiveCurveTo(9.57f, 10.65f, 9.66f, 10f)
                horizontalLineTo(14.34f)
                curveTo(14.43f, 10.65f, 14.5f, 11.32f, 14.5f, 12f)
                reflectiveCurveTo(14.43f, 13.34f, 14.34f, 14f)
                close()
                moveTo(14.59f, 19.56f)
                curveTo(15.19f, 18.45f, 15.65f, 17.25f, 15.97f, 16f)
                horizontalLineTo(18.92f)
                curveTo(17.96f, 17.65f, 16.43f, 18.93f, 14.59f, 19.56f)
                close()
                moveTo(16.36f, 14f)
                curveTo(16.44f, 13.34f, 16.5f, 12.68f, 16.5f, 12f)
                reflectiveCurveTo(16.44f, 10.66f, 16.36f, 10f)
                horizontalLineTo(19.74f)
                curveTo(19.9f, 10.64f, 20f, 11.31f, 20f, 12f)
                reflectiveCurveTo(19.9f, 13.36f, 19.74f, 14f)
                horizontalLineTo(16.36f)
                close()
            }
        }.build()

    private fun buildStatistics(): ImageVector =
        ImageVector.Builder(
            name = "Statistics",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(1f, 25f)
                horizontalLineTo(31f)
                verticalLineTo(28f)
                curveTo(31f, 29.66f, 29.66f, 31f, 28f, 31f)
                horizontalLineTo(4f)
                curveTo(2.34f, 31f, 1f, 29.66f, 1f, 28f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(2f, 20f)
                horizontalLineTo(6f)
                curveTo(6.55f, 20f, 7f, 20.45f, 7f, 21f)
                verticalLineTo(27f)
                horizontalLineTo(1f)
                verticalLineTo(21f)
                curveTo(1f, 20.45f, 1.45f, 20f, 2f, 20f)
                close()
                moveTo(10f, 15f)
                horizontalLineTo(14f)
                curveTo(14.55f, 15f, 15f, 15.45f, 15f, 16f)
                verticalLineTo(27f)
                horizontalLineTo(9f)
                verticalLineTo(16f)
                curveTo(9f, 15.45f, 9.45f, 15f, 10f, 15f)
                close()
                moveTo(18f, 17f)
                horizontalLineTo(22f)
                curveTo(22.55f, 17f, 23f, 17.45f, 23f, 18f)
                verticalLineTo(27f)
                horizontalLineTo(17f)
                verticalLineTo(18f)
                curveTo(17f, 17.45f, 17.45f, 17f, 18f, 17f)
                close()
                moveTo(26f, 11f)
                horizontalLineTo(30f)
                curveTo(30.55f, 11f, 31f, 11.45f, 31f, 12f)
                verticalLineTo(27f)
                horizontalLineTo(25f)
                verticalLineTo(12f)
                curveTo(25f, 11.45f, 25.45f, 11f, 26f, 11f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                curveTo(13.66f, 3f, 15f, 4.34f, 15f, 6f)
                curveTo(15f, 7.66f, 13.66f, 9f, 12f, 9f)
                curveTo(10.34f, 9f, 9f, 7.66f, 9f, 6f)
                curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
                close()
                moveTo(20f, 9f)
                curveTo(21.66f, 9f, 23f, 10.34f, 23f, 12f)
                curveTo(23f, 13.66f, 21.66f, 15f, 20f, 15f)
                curveTo(18.34f, 15f, 17f, 13.66f, 17f, 12f)
                curveTo(17f, 10.34f, 18.34f, 9f, 20f, 9f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2f, 16f)
                lineTo(4f, 16f)
                lineTo(10.8f, 7.5f)
                lineTo(20f, 12f)
                lineTo(28.5f, 2f)
                lineTo(30f, 2f)
            }
        }.build()

    private fun buildBookmark(): ImageVector =
        ImageVector.Builder(
            name = "Bookmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 22f,
            viewportHeight = 30f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 0f)
                horizontalLineTo(4f)
                curveTo(1.791f, 0f, 0f, 1.791f, 0f, 4f)
                verticalLineTo(26f)
                curveTo(0f, 28.209f, 1.791f, 30f, 4f, 30f)
                lineTo(11f, 23f)
                lineTo(18f, 30f)
                curveTo(20.209f, 30f, 22f, 28.209f, 22f, 26f)
                verticalLineTo(4f)
                curveTo(22f, 1.791f, 20.209f, 0f, 18f, 0f)
                close()
            }
        }.build()

    private fun buildStorage(): ImageVector =
        ImageVector.Builder(
            name = "Storage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 36f,
            viewportHeight = 36f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(33f, 6.69f)
                horizontalLineToRelative(0f)
                curveToRelative(-0.18f, -3.41f, -9.47f, -4.33f, -15f, -4.33f)
                reflectiveCurveTo(3f, 3.29f, 3f, 6.78f)
                verticalLineToRelative(22.59f)
                curveToRelative(0f, 3.49f, 9.43f, 4.43f, 15f, 4.43f)
                reflectiveCurveToRelative(15f, -0.93f, 15f, -4.43f)
                lineTo(33f, 6.78f)
                reflectiveCurveToRelative(0f, 0f, 0f, 0f)
                reflectiveCurveTo(33f, 6.7f, 33f, 6.69f)
                close()
                moveTo(31f, 14.25f)
                curveToRelative(-0.33f, 0.86f, -5.06f, 2.45f, -13f, 2.45f)
                arcToRelative(37.45f, 37.45f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -11f, dy1 = -1.36f)
                verticalLineToRelative(2.08f)
                arcToRelative(43.32f, 43.32f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 11f, dy1 = 1.28f)
                curveToRelative(4f, 0f, 9.93f, -0.48f, 13f, -2f)
                verticalLineToRelative(5.17f)
                curveToRelative(-0.33f, 0.86f, -5.06f, 2.45f, -13f, 2.45f)
                arcToRelative(37.45f, 37.45f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -11f, dy1 = -1.4f)
                lineTo(7f, 25f)
                arcToRelative(43.32f, 43.32f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 11f, dy1 = 1.28f)
                curveToRelative(4f, 0f, 9.93f, -0.48f, 13f, -2f)
                verticalLineToRelative(5.1f)
                curveToRelative(-0.35f, 0.86f, -5.08f, 2.45f, -13f, 2.45f)
                reflectiveCurveTo(5.3f, 30.2f, 5f, 29.37f)
                lineTo(5f, 6.82f)
                curveTo(5.3f, 6f, 10f, 4.36f, 18f, 4.36f)
                curveToRelative(7.77f, 0f, 12.46f, 1.53f, 13f, 2.37f)
                curveToRelative(-0.52f, 0.87f, -5.21f, 2.39f, -13f, 2.39f)
                arcTo(37.6f, 37.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 7f, y1 = 7.76f)
                lineTo(7f, 9.85f)
                arcToRelative(43.53f, 43.53f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 11f, dy1 = 1.27f)
                curveToRelative(4f, 0f, 9.93f, -0.48f, 13f, -2f)
                close()
            }
        }.build()

    private fun buildHome(): ImageVector =
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

    private fun buildMessage(): ImageVector =
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

    private fun buildProfile(): ImageVector =
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

    private fun buildEditOrSign(): ImageVector =
        ImageVector.Builder(
            name = "EditOrSign",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(888f, 37f)
                quadToRelative(-10f, -9f, -22.5f, -9f)
                reflectiveQuadTo(844f, 37f)
                lineToRelative(-63f, 63f)
                lineToRelative(125f, 125f)
                lineToRelative(63f, -63f)
                quadToRelative(9f, -9f, 9f, -21.5f)
                reflectiveQuadToRelative(-9f, -21.5f)
                close()
                moveTo(438f, 700f)
                quadToRelative(-4f, 0f, -7f, 3f)
                reflectiveQuadToRelative(-6f, 3f)
                lineToRelative(-150f, 50f)
                horizontalLineToRelative(-12f)
                quadToRelative(-8f, 0f, -11f, -7f)
                quadToRelative(-2f, -4f, -2f, -12f)
                lineToRelative(50f, -150f)
                quadToRelative(0f, -4f, 3f, -8f)
                lineToRelative(3f, -4f)
                lineToRelative(432f, -425f)
                lineToRelative(125f, 125f)
                close()
                moveTo(63f, 156f)
                quadToRelative(0f, -25f, 13f, -46.5f)
                reflectiveQuadTo(110f, 75f)
                reflectiveQuadToRelative(46f, -13f)
                horizontalLineToRelative(407f)
                quadToRelative(13f, 0f, 22f, 9f)
                reflectiveQuadToRelative(9f, 22.5f)
                reflectiveQuadToRelative(-8.5f, 22.5f)
                reflectiveQuadToRelative(-22.5f, 9f)
                lineTo(156f, 125f)
                quadToRelative(-14f, 0f, -22.5f, 8.5f)
                reflectiveQuadTo(125f, 156f)
                verticalLineToRelative(688f)
                quadToRelative(0f, 14f, 8.5f, 22.5f)
                reflectiveQuadTo(156f, 875f)
                horizontalLineToRelative(688f)
                quadToRelative(14f, 0f, 22.5f, -8.5f)
                reflectiveQuadTo(875f, 844f)
                lineTo(875f, 469f)
                quadToRelative(0f, -14f, 8.5f, -23f)
                reflectiveQuadToRelative(22.5f, -9f)
                reflectiveQuadToRelative(23f, 9f)
                reflectiveQuadToRelative(9f, 23f)
                verticalLineToRelative(375f)
                quadToRelative(0f, 25f, -13f, 46.5f)
                reflectiveQuadTo(890.5f, 925f)
                reflectiveQuadTo(844f, 938f)
                lineTo(156f, 938f)
                quadToRelative(-25f, 0f, -46f, -13f)
                reflectiveQuadToRelative(-34f, -34.5f)
                reflectiveQuadTo(63f, 844f)
                close()
            }
        }.build()

    private fun buildSearch(): ImageVector =
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

    private fun buildPlus(): ImageVector =
        ImageVector.Builder(
            name = "Plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 1f)
                horizontalLineTo(6f)
                verticalLineTo(6f)
                lineTo(1f, 6f)
                verticalLineTo(10f)
                horizontalLineTo(6f)
                verticalLineTo(15f)
                horizontalLineTo(10f)
                verticalLineTo(10f)
                horizontalLineTo(15f)
                verticalLineTo(6f)
                lineTo(10f, 6f)
                verticalLineTo(1f)
                close()
            }
        }.build()

    private fun buildViews(): ImageVector =
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

    private fun buildComment(): ImageVector =
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

    private fun buildPersonFill(): ImageVector =
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

    private fun buildStarOutline(): ImageVector =
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

    private fun buildStarFilled(): ImageVector =
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

    private fun buildShare(): ImageVector =
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

    private fun buildExplore(): ImageVector =
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

    private fun buildNew(): ImageVector =
        ImageVector.Builder(
            name = "New",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8f, 0f)
                lineTo(5.69f, 1.65f)
                lineTo(2.857f, 1.871f)
                lineTo(2.148f, 4.621f)
                lineTo(0.121f, 6.611f)
                lineTo(1.346f, 9.174f)
                lineTo(1.072f, 12f)
                lineTo(3.658f, 13.176f)
                lineTo(5.264f, 15.518f)
                lineTo(8f, 14.758f)
                lineTo(10.736f, 15.518f)
                lineTo(12.342f, 13.176f)
                lineTo(14.928f, 12f)
                lineTo(14.654f, 9.174f)
                lineTo(15.88f, 6.61f)
                lineTo(13.853f, 4.62f)
                lineTo(13.143f, 1.87f)
                lineTo(10.312f, 1.65f)
                close()
                moveTo(7f, 4f)
                horizontalLineTo(9f)
                verticalLineTo(7f)
                lineTo(8.5f, 9f)
                horizontalLineTo(7.5f)
                lineTo(7f, 7f)
                close()
                moveTo(8f, 10f)
                curveTo(8.552f, 10f, 9f, 10.448f, 9f, 11f)
                curveTo(9f, 11.552f, 8.552f, 12f, 8f, 12f)
                curveTo(7.448f, 12f, 7f, 11.552f, 7f, 11f)
                curveTo(7f, 10.448f, 7.448f, 10f, 8f, 10f)
                close()
            }
        }.build()

    private fun buildFilterList(): ImageVector =
        ImageVector.Builder(
            name = "FilterList",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 6f)
                horizontalLineTo(19f)
                moveTo(21f, 12f)
                horizontalLineTo(16f)
                moveTo(21f, 18f)
                horizontalLineTo(16f)
                moveTo(7f, 20f)
                verticalLineTo(13.561f)
                curveTo(7f, 13.353f, 7f, 13.249f, 6.98f, 13.15f)
                curveTo(6.961f, 13.061f, 6.932f, 12.976f, 6.891f, 12.896f)
                curveTo(6.844f, 12.805f, 6.779f, 12.724f, 6.649f, 12.562f)
                lineTo(3.351f, 8.438f)
                curveTo(3.221f, 8.276f, 3.156f, 8.195f, 3.109f, 8.104f)
                curveTo(3.068f, 8.024f, 3.039f, 7.939f, 3.02f, 7.85f)
                curveTo(3f, 7.751f, 3f, 7.647f, 3f, 7.439f)
                verticalLineTo(5.6f)
                curveTo(3f, 5.04f, 3f, 4.76f, 3.109f, 4.546f)
                curveTo(3.205f, 4.358f, 3.358f, 4.205f, 3.546f, 4.109f)
                curveTo(3.76f, 4f, 4.04f, 4f, 4.6f, 4f)
                horizontalLineTo(13.4f)
                curveTo(13.96f, 4f, 14.24f, 4f, 14.454f, 4.109f)
                curveTo(14.642f, 4.205f, 14.795f, 4.358f, 14.891f, 4.546f)
                curveTo(15f, 4.76f, 15f, 5.04f, 15f, 5.6f)
                verticalLineTo(7.439f)
                curveTo(15f, 7.647f, 15f, 7.751f, 14.98f, 7.85f)
                curveTo(14.961f, 7.939f, 14.932f, 8.024f, 14.891f, 8.104f)
                curveTo(14.844f, 8.195f, 14.779f, 8.276f, 14.649f, 8.438f)
                lineTo(11.351f, 12.562f)
                curveTo(11.221f, 12.724f, 11.156f, 12.805f, 11.109f, 12.896f)
                curveTo(11.068f, 12.976f, 11.039f, 13.061f, 11.02f, 13.15f)
                curveTo(11f, 13.249f, 11f, 13.353f, 11f, 13.561f)
                verticalLineTo(17f)
                lineTo(7f, 20f)
                close()
            }
        }.build()

    private fun buildCheckboxEnabled(): ImageVector =
        ImageVector.Builder(
            name = "CheckboxEnabled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 4f)
                horizontalLineTo(19f)
                curveTo(19.552f, 4f, 20f, 4.448f, 20f, 5f)
                verticalLineTo(19f)
                curveTo(20f, 19.552f, 19.552f, 20f, 19f, 20f)
                horizontalLineTo(5f)
                curveTo(4.448f, 20f, 4f, 19.552f, 4f, 19f)
                verticalLineTo(5f)
                curveTo(4f, 4.448f, 4.448f, 4f, 5f, 4f)
                close()
                moveTo(7.5f, 12.2f)
                lineTo(10.6f, 15.3f)
                lineTo(16.8f, 8.7f)
            }
        }.build()

    private fun buildCheckboxDisabled(): ImageVector =
        ImageVector.Builder(
            name = "CheckboxDisabled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 4f)
                horizontalLineTo(19f)
                curveTo(19.552f, 4f, 20f, 4.448f, 20f, 5f)
                verticalLineTo(19f)
                curveTo(20f, 19.552f, 19.552f, 20f, 19f, 20f)
                horizontalLineTo(5f)
                curveTo(4.448f, 20f, 4f, 19.552f, 4f, 19f)
                verticalLineTo(5f)
                curveTo(4f, 4.448f, 4.448f, 4f, 5f, 4f)
                close()
            }
        }.build()

    private fun buildThreeDots(): ImageVector =
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

    private fun buildHeart(): ImageVector =
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

    private fun buildHistory(): ImageVector =
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

    private fun buildTrashcan(): ImageVector =
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

    private fun buildReload(): ImageVector =
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

    private fun buildSync(): ImageVector =
        ImageVector.Builder(
            name = "Sync",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.43f, 4.25f)
                curveTo(18.2319f, 4.25259f, 18.0426f, 4.33244f, 17.9025f, 4.47253f)
                curveTo(17.7625f, 4.61263f, 17.6826f, 4.80189f, 17.68f, 5f)
                verticalLineTo(7.43f)
                lineTo(16.84f, 6.59f)
                curveTo(15.971f, 5.71363f, 14.8924f, 5.07396f, 13.7067f, 4.73172f)
                curveTo(12.5209f, 4.38948f, 11.2673f, 4.35604f, 10.065f, 4.63458f)
                curveTo(8.86267f, 4.91312f, 7.7515f, 5.49439f, 6.83703f, 6.32318f)
                curveTo(5.92255f, 7.15198f, 5.23512f, 8.20078f, 4.84001f, 9.37f)
                curveTo(4.79887f, 9.46531f, 4.77824f, 9.56821f, 4.77947f, 9.67202f)
                curveTo(4.7807f, 9.77583f, 4.80375f, 9.87821f, 4.84714f, 9.97252f)
                curveTo(4.89052f, 10.0668f, 4.95326f, 10.151f, 5.03129f, 10.2194f)
                curveTo(5.10931f, 10.2879f, 5.20087f, 10.3392f, 5.30001f, 10.37f)
                curveTo(5.38273f, 10.3844f, 5.4673f, 10.3844f, 5.55001f, 10.37f)
                curveTo(5.70646f, 10.3684f, 5.85861f, 10.3186f, 5.98568f, 10.2273f)
                curveTo(6.11275f, 10.136f, 6.20856f, 10.0078f, 6.26001f, 9.86f)
                curveTo(6.53938f, 9.0301f, 7.00847f, 8.27681f, 7.63001f, 7.66f)
                curveTo(8.70957f, 6.58464f, 10.1713f, 5.98085f, 11.695f, 5.98085f)
                curveTo(13.2188f, 5.98085f, 14.6805f, 6.58464f, 15.76f, 7.66f)
                lineTo(16.6f, 8.5f)
                horizontalLineTo(14.19f)
                curveTo(13.9911f, 8.5f, 13.8003f, 8.57902f, 13.6597f, 8.71967f)
                curveTo(13.519f, 8.86032f, 13.44f, 9.05109f, 13.44f, 9.25f)
                curveTo(13.44f, 9.44891f, 13.519f, 9.63968f, 13.6597f, 9.78033f)
                curveTo(13.8003f, 9.92098f, 13.9911f, 10f, 14.19f, 10f)
                horizontalLineTo(18.43f)
                curveTo(18.5289f, 10.0013f, 18.627f, 9.98286f, 18.7186f, 9.94565f)
                curveTo(18.8102f, 9.90844f, 18.8934f, 9.85324f, 18.9633f, 9.78333f)
                curveTo(19.0333f, 9.71341f, 19.0885f, 9.6302f, 19.1257f, 9.5386f)
                curveTo(19.1629f, 9.44699f, 19.1814f, 9.34886f, 19.18f, 9.25f)
                verticalLineTo(5f)
                curveTo(19.18f, 4.80109f, 19.101f, 4.61032f, 18.9603f, 4.46967f)
                curveTo(18.8197f, 4.32902f, 18.6289f, 4.25f, 18.43f, 4.25f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.68f, 13.68f)
                curveTo(18.5837f, 13.6422f, 18.4808f, 13.6244f, 18.3774f, 13.6277f)
                curveTo(18.274f, 13.6311f, 18.1724f, 13.6555f, 18.0787f, 13.6995f)
                curveTo(17.9851f, 13.7435f, 17.9015f, 13.8062f, 17.8329f, 13.8836f)
                curveTo(17.7643f, 13.9611f, 17.7123f, 14.0517f, 17.68f, 14.15f)
                curveTo(17.4006f, 14.9799f, 16.9316f, 15.7332f, 16.31f, 16.35f)
                curveTo(15.2305f, 17.4254f, 13.7688f, 18.0291f, 12.245f, 18.0291f)
                curveTo(10.7213f, 18.0291f, 9.25957f, 17.4254f, 8.18001f, 16.35f)
                lineTo(7.34001f, 15.51f)
                horizontalLineTo(9.81002f)
                curveTo(10.0089f, 15.51f, 10.1997f, 15.431f, 10.3403f, 15.2903f)
                curveTo(10.481f, 15.1497f, 10.56f, 14.9589f, 10.56f, 14.76f)
                curveTo(10.56f, 14.5611f, 10.481f, 14.3703f, 10.3403f, 14.2297f)
                curveTo(10.1997f, 14.089f, 10.0089f, 14.01f, 9.81002f, 14.01f)
                horizontalLineTo(5.57001f)
                curveTo(5.47115f, 14.0086f, 5.37302f, 14.0271f, 5.28142f, 14.0643f)
                curveTo(5.18982f, 14.1016f, 5.1066f, 14.1568f, 5.03669f, 14.2267f)
                curveTo(4.96677f, 14.2966f, 4.91158f, 14.3798f, 4.87436f, 14.4714f)
                curveTo(4.83715f, 14.563f, 4.81867f, 14.6611f, 4.82001f, 14.76f)
                verticalLineTo(19f)
                curveTo(4.82001f, 19.1989f, 4.89903f, 19.3897f, 5.03968f, 19.5303f)
                curveTo(5.18034f, 19.671f, 5.3711f, 19.75f, 5.57001f, 19.75f)
                curveTo(5.76893f, 19.75f, 5.95969f, 19.671f, 6.10034f, 19.5303f)
                curveTo(6.241f, 19.3897f, 6.32001f, 19.1989f, 6.32001f, 19f)
                verticalLineTo(16.57f)
                lineTo(7.16001f, 17.41f)
                curveTo(8.02901f, 18.2864f, 9.10761f, 18.926f, 10.2934f, 19.2683f)
                curveTo(11.4791f, 19.6105f, 12.7327f, 19.6439f, 13.935f, 19.3654f)
                curveTo(15.1374f, 19.0869f, 16.2485f, 18.5056f, 17.163f, 17.6768f)
                curveTo(18.0775f, 16.848f, 18.7649f, 15.7992f, 19.16f, 14.63f)
                curveTo(19.1926f, 14.5362f, 19.2061f, 14.4368f, 19.1995f, 14.3377f)
                curveTo(19.1929f, 14.2386f, 19.1664f, 14.1418f, 19.1216f, 14.0532f)
                curveTo(19.0768f, 13.9645f, 19.0146f, 13.8858f, 18.9387f, 13.8217f)
                curveTo(18.8629f, 13.7576f, 18.7749f, 13.7094f, 18.68f, 13.68f)
                close()
            }
        }.build()

    private fun buildReply(): ImageVector =
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

    private fun buildSetting(): ImageVector =
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

    private fun buildBook(): ImageVector =
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

    private fun buildCopy(): ImageVector =
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

    private fun buildSave(): ImageVector =
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

    private fun buildInfoCircle(): ImageVector =
        ImageVector.Builder(
            name = "InfoCircle",
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
                moveTo(12.0f, 21.0f)
                curveTo(16.971f, 21.0f, 21.0f, 16.971f, 21.0f, 12.0f)
                curveTo(21.0f, 7.029f, 16.971f, 3.0f, 12.0f, 3.0f)
                curveTo(7.029f, 3.0f, 3.0f, 7.029f, 3.0f, 12.0f)
                curveTo(3.0f, 16.971f, 7.029f, 21.0f, 12.0f, 21.0f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12.0f, 11.0f)
                verticalLineTo(16.0f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(13.15f, 8.5f)
                curveTo(13.15f, 9.135f, 12.635f, 9.65f, 12.0f, 9.65f)
                curveTo(11.365f, 9.65f, 10.85f, 9.135f, 10.85f, 8.5f)
                curveTo(10.85f, 7.865f, 11.365f, 7.35f, 12.0f, 7.35f)
                curveTo(12.635f, 7.35f, 13.15f, 7.865f, 13.15f, 8.5f)
                close()
            }
        }.build()

    @Suppress("ConstPropertyName")
    const val Back: String = "◀"
}
