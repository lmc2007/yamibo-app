package me.thenano.yamibo.yamibo_app.profile.settings.bound

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.LocalNovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsSlider
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state

import kotlin.math.roundToInt

private const val PREVIEW_TEXT = "我是YamiboApp的作者TheNano，這是一個第三方個人獨立開發的開源App"

@Composable
fun NovelReaderPreviewSetting() {
    val colors = YamiboTheme.colors
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val fontSize = novelSettingsRepo.fontSize.state()
    val lineSpacing = novelSettingsRepo.lineSpacing.state()
    val contentWidthFraction = novelSettingsRepo.contentWidthFraction.state()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.creamSurface)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(contentWidthFraction)
        ) {
            Text(
                text = PREVIEW_TEXT,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * lineSpacing).sp,
                color = colors.textDark
            )
        }
    }
}

@Composable
fun NovelFontSizeSetting() {
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val fontSize = novelSettingsRepo.fontSize.state()

    SettingsSlider(
        label = "文字大小",
        value = fontSize.toFloat(),
        valueRange = 10f..40f,
        steps = 29,
        valueDisplay = { "${it.toInt()} sp" },
        onValueChange = { novelSettingsRepo.fontSize.setValue(it.toInt()) }
    )
}

@Composable
fun NovelLineSpacingSetting() {
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val lineSpacing = novelSettingsRepo.lineSpacing.state()

    SettingsSlider(
        label = "行距比例",
        value = lineSpacing,
        valueRange = 1.0f..3.0f,
        steps = 39,
        valueDisplay = { "${(it * 100f).roundToInt() / 100f}x" },
        onValueChange = { novelSettingsRepo.lineSpacing.setValue(it) }
    )
}

@Composable
fun NovelContentWidthSetting() {
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val contentWidthFraction = novelSettingsRepo.contentWidthFraction.state()

    SettingsSlider(
        label = "內容寬度",
        value = contentWidthFraction,
        valueRange = 0.6f..1.0f,
        steps = 39,
        valueDisplay = { "${(it * 100f).roundToInt()}%" },
        onValueChange = { novelSettingsRepo.contentWidthFraction.setValue(it) }
    )
}
