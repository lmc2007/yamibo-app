package me.thenano.yamibo.yamibo_app.thread.detail.components

import me.thenano.yamibo.yamibo_app.i18n.i18n

import YamiboIcons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.controls.YamiboActionChip
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
internal fun DetailNoteActionButton(
    hasNote: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = colors.brownPrimary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = YamiboIcons.EditOrSign,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colors.brownDeep,
            )
            Text(
                text = if (hasNote) i18n("編輯筆記") else i18n("新增筆記"),
                color = colors.brownDeep,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun DetailNoteCard(
    content: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (content.isBlank()) return

    val colors = YamiboTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = colors.creamSurface,
        border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.16f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = YamiboIcons.EditOrSign,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.brownDeep,
                    )
                    Text(
                        text = i18n("筆記"),
                        color = colors.brownDeep,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                YamiboActionChip(i18n("編輯"), onClick = onEdit)
            }
            Text(
                text = content,
                color = colors.textDark.copy(alpha = 0.82f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun DetailNoteEditorDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = YamiboTheme.colors
    var content by remember(initialContent) { mutableStateOf(initialContent) }
    val hasExistingNote = initialContent.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.creamBackground,
        title = {
            Text(
                text = if (hasExistingNote) i18n("編輯筆記") else i18n("新增筆記"),
                color = colors.textDark,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                placeholder = { Text(i18n("輸入本地筆記...")) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.creamSurface,
                    unfocusedContainerColor = colors.creamSurface,
                    focusedTextColor = colors.textDark,
                    unfocusedTextColor = colors.textDark,
                    focusedIndicatorColor = colors.brownPrimary,
                    unfocusedIndicatorColor = colors.brownPrimary.copy(alpha = 0.24f),
                    cursorColor = colors.brownDeep,
                ),
            )
        },
        confirmButton = {
            Surface(
                onClick = { onSave(content) },
                color = colors.brownDeep,
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text(i18n("保存"), color = colors.creamBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasExistingNote) {
                    Surface(
                        onClick = onDelete,
                        color = colors.brownPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Box(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
                            Text(i18n("刪除"), color = colors.textStrong, fontSize = 13.sp)
                        }
                    }
                }
                Surface(
                    onClick = onDismiss,
                    color = colors.brownPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
                        Text(i18n("取消"), color = colors.textStrong, fontSize = 13.sp)
                    }
                }
            }
        },
    )
}

