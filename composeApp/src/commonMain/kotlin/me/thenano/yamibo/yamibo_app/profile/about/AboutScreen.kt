package me.thenano.yamibo.yamibo_app.profile.about

import YamiboIcons
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.AppVersion
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboTopBar
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.update.IAppUpdateScreen
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.webview.IPlatformWebView
import org.jetbrains.compose.resources.painterResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.ic_discord
import yamibo_app.composeapp.generated.resources.ic_github
import yamibo_app.composeapp.generated.resources.logo_about

private const val GitHubUrl = "https://github.com/LittleSurvival/yamibo-app"
private const val DiscordUrl = "https://discord.gg/3nhKpxM7Hc"
private const val ChangelogUrl = "https://github.com/LittleSurvival/yamibo-app/blob/main/update/changelogs/1.changelog"

@Composable
internal fun AboutScreen() {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.creamBackground),
    ) {
        YamiboTopBar(title = i18n("關於"), onBack = navigator::pop)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.logo_about),
                contentDescription = null,
                modifier = Modifier
                    .width(270.dp)
                    .height(76.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = AppVersion.displayName,
                color = colors.textDark.copy(alpha = 0.58f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = i18n("app dev by thenano"),
                color = colors.textDark.copy(alpha = 0.58f),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(22.dp))

            AboutSection {
                AboutInfoRow(
                    title = i18n("版本"),
                    value = AppVersion.displayName,
                )
                AboutDivider()
                AboutActionRow(
                    icon = YamiboIcons.Reload,
                    title = i18n("檢查更新"),
                    onClick = { navigator.navigate(IAppUpdateScreen()) },
                )
                AboutDivider()
                AboutActionRow(
                    icon = YamiboIcons.Book,
                    title = i18n("更新日誌"),
                    onClick = {
                        navigator.navigate(
                            IPlatformWebView(
                                ChangelogUrl,
                                title = i18n("更新日誌"),
                                useBackIcon = true,
                            ),
                        )
                    },
                )
            }

            Spacer(Modifier.height(14.dp))

            AboutSection {
                AboutHeaderRow(title = i18n("社群"))
                AboutDivider()
                AboutResourceActionRow(
                    icon = painterResource(Res.drawable.ic_discord),
                    title = "Discord",
                    onClick = { navigator.navigate(IPlatformWebView(DiscordUrl, title = "Discord", useBackIcon = true)) },
                )
                AboutDivider()
                AboutResourceActionRow(
                    icon = painterResource(Res.drawable.ic_github),
                    title = "GitHub",
                    onClick = { navigator.navigate(IPlatformWebView(GitHubUrl, title = "GitHub", useBackIcon = true)) },
                )
            }
        }
    }
}

@Composable
private fun AboutSection(content: @Composable ColumnScope.() -> Unit) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(content = content)
    }
}

@Composable
private fun AboutHeaderRow(title: String) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = colors.textDark, fontSize = 15.sp)
    }
}

@Composable
private fun AboutInfoRow(
    title: String,
    value: String,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = colors.textDark, fontSize = 15.sp)
        Text(text = value, color = colors.textDark.copy(alpha = 0.58f), fontSize = 13.sp)
    }
}

@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(colors.creamBackground, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.brownPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            color = colors.textDark,
            fontSize = 15.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
        Text(">", color = colors.textDark.copy(alpha = 0.32f), fontSize = 14.sp)
    }
}

@Composable
private fun AboutResourceActionRow(
    icon: Painter,
    title: String,
    onClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(colors.creamBackground, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                tint = colors.brownPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            color = colors.textDark,
            fontSize = 15.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
        Text(">", color = colors.textDark.copy(alpha = 0.32f), fontSize = 14.sp)
    }
}

@Composable
private fun AboutDivider() {
    val colors = YamiboTheme.colors
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = colors.brownLight.copy(alpha = 0.15f),
    )
}
