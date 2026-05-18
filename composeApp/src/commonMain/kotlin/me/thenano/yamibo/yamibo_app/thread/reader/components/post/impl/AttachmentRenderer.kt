package me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.page.Attachment
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun AttachmentRenderer(attachments: List<Attachment>, modifier: Modifier = Modifier) {
    val colors = YamiboTheme.colors
    if (attachments.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = appString(Res.string.ui_attachment),
            color = colors.textDark,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        attachments.forEach { attachment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.creamSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Icon (Generic)
                Text(
                    text = "📎",
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.name,
                        color = colors.textDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = appString(Res.string.common_download_count, attachment.fileSize, attachment.downloadTimes),
                        color = colors.textDark.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                
                // You can add a download icon button here later if needed
            }
        }
    }
}


