package me.thenano.yamibo.yamibo_app.profile.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.repository.scheme.YamiboColorScheme
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun ThemeSelectorContent(
    currentMode: String,
    currentSchemeName: String,
    onModeChange: (String) -> Unit,
    onSchemeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YamiboTheme.colors

    Column(modifier = modifier) {
        Text(
            text = "顏色主題",
            color = colors.textDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Mode Segmented Control (System / Light / Dark)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, colors.brownLight.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("SYSTEM" to "系統", "LIGHT" to "淺色", "DARK" to "深色").forEach { (mode, label) ->
                val isSelected = currentMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onModeChange(mode) }
                        .background(if (isSelected) colors.brownPrimary else Color.Transparent)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else colors.textDark.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Scheme Selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(YamiboColorScheme.all) { scheme ->
                ThemeCard(
                    scheme = scheme,
                    isSelected = scheme.name == currentSchemeName,
                    onClick = { onSchemeChange(scheme.name) }
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    scheme: YamiboColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = YamiboTheme.colors
    val borderColor = if (isSelected) Color(scheme.brownPrimary) else Color.Transparent
    val backgroundColor = Color(scheme.brownDeep)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) borderColor else colors.brownLight.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            // Mock UI header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(scheme.creamSurface))
            )

            // Mock main content area with primary color dot
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(scheme.brownPrimary))
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(scheme.brownLight).copy(alpha = 0.5f))
                )
            }

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(scheme.brownPrimary))
                        .border(1.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = scheme.name,
            color = if (isSelected) colors.textDark else colors.textDark.copy(alpha = 0.5f),
            fontSize = 10.sp,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
