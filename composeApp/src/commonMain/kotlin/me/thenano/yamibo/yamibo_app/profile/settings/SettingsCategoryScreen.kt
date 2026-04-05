package me.thenano.yamibo.yamibo_app.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalMangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalNovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsChipRow
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsSlider
import me.thenano.yamibo.yamibo_app.profile.settings.components.ThemeSelectorContent
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

private const val PREVIEW_TEXT = "我是YamiboApp的作者TheNano，這是一個第三方個人獨立開發的開源App"

/** 根據 category 顯示對應的設定內容 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsCategoryScreen(category: String) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current

    val title = when (category) {
        "appearance" -> "外觀"
        "novel_reader" -> "小說閱讀器"
        "manga_reader" -> "漫畫閱讀器"
        else -> "設定"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Text("◀", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.brownDeep,
                    scrolledContainerColor = colors.brownDeep
                )
            )
        },
        containerColor = colors.creamBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            when (category) {
                "appearance" -> AppearanceContent()
                "novel_reader" -> NovelReaderContent()
                "manga_reader" -> MangaReaderContent()
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = YamiboTheme.colors
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textDark.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

// ─── Appearance ───

@Composable
private fun AppearanceContent() {
    val appSettingsRepo = LocalAppSettingsRepository.current
    val appSettings by appSettingsRepo.settings.collectAsState()

    ThemeSelectorContent(
        currentMode = appSettings.theme.mode,
        currentSchemeName = appSettings.theme.scheme,
        onModeChange = { newMode ->
            appSettingsRepo.update { it.copy(theme = it.theme.copy(mode = newMode)) }
        },
        onSchemeChange = { newScheme ->
            appSettingsRepo.update { it.copy(theme = it.theme.copy(scheme = newScheme)) }
        }
    )
}

// ─── Novel Reader ───

@Composable
private fun NovelReaderContent() {
    val colors = YamiboTheme.colors
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val settings by novelSettingsRepo.settings.collectAsState()

    // Preview Area
    SectionLabel("預覽")
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
                .fillMaxWidth(settings.contentWidthFraction)
        ) {
            Text(
                text = PREVIEW_TEXT,
                fontSize = settings.fontSize.sp,
                lineHeight = (settings.fontSize * settings.lineSpacing).sp,
                color = colors.textDark
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    // Font Size
    SectionLabel("字體大小")
    SettingsSlider(
        label = "文字大小",
        value = settings.fontSize.toFloat(),
        valueRange = 12f..28f,
        steps = 15,
        valueDisplay = { "${it.toInt()} sp" },
        onValueChange = { newSize ->
            novelSettingsRepo.update { it.copy(fontSize = newSize.toInt()) }
        }
    )

    Spacer(Modifier.height(24.dp))

    // Line Spacing
    SectionLabel("行距")
    SettingsChipRow(
        options = listOf(
            "1.25" to "1.25x",
            "1.5" to "1.5x",
            "1.75" to "1.75x",
            "2.0" to "2.0x"
        ),
        selectedValue = settings.lineSpacing.toString(),
        onSelect = { value ->
            novelSettingsRepo.update { it.copy(lineSpacing = value.toFloat()) }
        }
    )

    Spacer(Modifier.height(24.dp))

    // Content Width Fraction
    SectionLabel("頁寬")
    SettingsSlider(
        label = "內容寬度",
        value = settings.contentWidthFraction,
        valueRange = 0.6f..1.0f,
        steps = 7,
        valueDisplay = { "${(it * 100).toInt()}%" },
        onValueChange = { newFraction ->
            novelSettingsRepo.update { it.copy(contentWidthFraction = newFraction) }
        }
    )
}

// ─── Manga Reader ───

@Composable
private fun MangaReaderContent() {
    val mangaSettingsRepo = LocalMangaReaderSettingsRepository.current
    val settings by mangaSettingsRepo.settings.collectAsState()

    SectionLabel("閱讀模式")
    SettingsChipRow(
        options = listOf(
            "SINGLE_LTR" to "單頁(左至右)",
            "SINGLE_RTL" to "單頁(右至左)",
            "SINGLE_TTB" to "單頁(上至下)",
            "SCROLL_CONTINUOUS" to "捲動(連續)",
            "SCROLL_GAP" to "捲動(留空)"
        ),
        selectedValue = settings.readingMode,
        onSelect = { value ->
            mangaSettingsRepo.update { it.copy(readingMode = value) }
        }
    )

    Spacer(Modifier.height(24.dp))

    SectionLabel("輕觸區域")
    SettingsChipRow(
        options = listOf(
            "L_SHAPE" to "L式",
            "KINDLE" to "Kindle式",
            "EDGE" to "邊緣式",
            "LEFT_RIGHT" to "左右式",
            "DISABLED" to "停用"
        ),
        selectedValue = settings.touchZone,
        onSelect = { value ->
            mangaSettingsRepo.update { it.copy(touchZone = value) }
        }
    )
}
