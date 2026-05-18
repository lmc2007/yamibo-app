package me.thenano.yamibo.yamibo_app.thread.reader.components.novel

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.profile.settings.components.ThemeSelectorContent
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelChineseConversionSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelFontSizeSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelLineSpacingSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelSystemBarsBackgroundSetting
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state

@Composable
fun NovelReaderSettingsPanel(
    visible: Boolean,
    novelSettingsRepo: NovelReaderSettingsRepository,
    appSettingsRepo: AppSettingsRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YamiboTheme.colors
    val themeMode = appSettingsRepo.themeMode.state()
    val themeScheme = appSettingsRepo.themeScheme.state()

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            color = colors.creamSurface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Typography Section
                SectionTitle(appString(Res.string.ui_text_layout), color = colors.textDark)
                
                Spacer(Modifier.height(8.dp))
                NovelFontSizeSetting()
                
                Spacer(Modifier.height(16.dp))
                NovelLineSpacingSetting()

                Spacer(Modifier.height(16.dp))
                NovelSystemBarsBackgroundSetting()

                Spacer(Modifier.height(24.dp))

                SectionTitle(appString(Res.string.ui_conversion_between_simplified_traditional_chinese), color = colors.textDark)
                NovelChineseConversionSetting()

                Spacer(Modifier.height(24.dp))

                // Theme Section
                ThemeSelectorContent(
                    currentMode = themeMode,
                    currentScheme = themeScheme,
                    onModeChange = { appSettingsRepo.themeMode.setValue(it) },
                    onSchemeChange = { appSettingsRepo.themeScheme.setValue(it) }
                )
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

