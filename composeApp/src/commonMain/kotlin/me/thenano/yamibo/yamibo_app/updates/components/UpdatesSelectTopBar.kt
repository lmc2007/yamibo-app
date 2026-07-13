package me.thenano.yamibo.yamibo_app.updates.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.i18n.i18n

@Composable
internal fun UpdatesSelectTopBar(
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onCancel: () -> Unit,
    onDeleteSelected: () -> Unit,
    selectedCount: Int,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = i18n("已選 {} 項", selectedCount),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textStrong,
            modifier = Modifier.weight(1f),
        )
        Surface(onClick = onSelectAll, shape = RoundedCornerShape(10.dp), color = colors.brownPrimary.copy(alpha = 0.12f)) {
            Text(i18n("全選"), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textStrong)
        }
        if (selectedCount > 0) {
            Surface(onClick = onDeleteSelected, shape = RoundedCornerShape(10.dp), color = Color(0xFFE53935).copy(alpha = 0.15f)) {
                Text(i18n("刪除"), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
            }
        }
        Surface(onClick = onClearAll, shape = RoundedCornerShape(10.dp), color = Color(0xFFE53935).copy(alpha = 0.1f)) {
            Text(i18n("清空"), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
        }
        Surface(onClick = onCancel, shape = RoundedCornerShape(10.dp), color = colors.brownPrimary.copy(alpha = 0.12f)) {
            Text(i18n("取消"), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textStrong)
        }
    }
}
