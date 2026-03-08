package me.thenano.yamibo.yamibo_app.thread.render.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun HtmlRenderer(html: String, modifier: Modifier = Modifier) {
    val blocks = remember(html) { HtmlParser.parseHtml(html) }
    Column(modifier = modifier) {
        blocks.forEach { block ->
            HtmlBlockRenderer(block)
        }
    }
}

@Composable
private fun HtmlBlockRenderer(block: HtmlBlock) {
    val colors = YamiboTheme.colors
    val uriHandler = LocalUriHandler.current

    when (block) {
        is HtmlBlock.Text -> {
            var showLinkDialog by remember { mutableStateOf<String?>(null) }

            ClickableText(
                text = block.annotatedString,
                style = TextStyle(color = colors.textDark, fontSize = 15.sp, lineHeight = 24.sp),
                modifier = Modifier.padding(vertical = 2.dp),
                onClick = { offset ->
                    block.annotatedString.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { annotation ->
                            showLinkDialog = annotation.item
                        }
                }
            )

            if (showLinkDialog != null) {
                val url = showLinkDialog ?: ""
                val fullUrl = if (url.startsWith("http")) url else "https://bbs.yamibo.com/$url"
                AlertDialog(
                    onDismissRequest = { showLinkDialog = null },
                    title = { Text("外部連結", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) },
                    text = { Text("即將開啟連結：\n${fullUrl}\n\n是否以瀏覽器開啟？") },
                    confirmButton = {
                        TextButton(onClick = {
                            try {
                                uriHandler.openUri(fullUrl)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            showLinkDialog = null
                        }) {
                            Text("確認", color = colors.brownPrimary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLinkDialog = null }) {
                            Text("取消", color = colors.textDark.copy(alpha = 0.5f))
                        }
                    },
                    containerColor = colors.creamSurface,
                    titleContentColor = colors.textDark,
                    textContentColor = colors.textDark.copy(alpha = 0.7f)
                )
            }
        }
        is HtmlBlock.Image -> {
            val url = if (block.url.startsWith("http")) block.url else "https://bbs.yamibo.com/${block.url}"
            KamelImage(
                resource = asyncPainterResource(data = url),
                contentDescription = block.alt,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentScale = ContentScale.FillWidth,
                onLoading = {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.brownPrimary)
                    }
                },
                onFailure = {
                    Text("Image Error: ${it.message}", color = Color.Red, fontSize = 12.sp)
                }
            )
        }
        is HtmlBlock.Collapse -> {
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.creamBackground),
                onClick = { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text(
                        text = if (expanded) "▼ ${block.title ?: "點擊收起"}" else "▶ ${block.title ?: "點擊展開"}",
                        color = colors.brownPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            block.contentBlocks.forEach { HtmlBlockRenderer(it) }
                        }
                    }
                }
            }
        }
        is HtmlBlock.Locked -> {
            val dashPattern = remember { PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = Color(0xFFD32F2F),
                            style = Stroke(width = 4f, pathEffect = dashPattern),
                            cornerRadius = CornerRadius(24f, 24f)
                        )
                    }
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔒",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "本帖隱藏內容需要積分: ${block.cost}",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    block.contentBlocks.forEach { HtmlBlockRenderer(it) }
                }
            }
        }
        is HtmlBlock.Quote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.creamSurface)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(colors.brownPrimary)
                )
                Column(modifier = Modifier.padding(12.dp)) {
                    block.contentBlocks.forEach { HtmlBlockRenderer(it) }
                }
            }
        }
        is HtmlBlock.Code -> {
            Surface(
                color = Color(0xFF2B2B2B), // very dark gray for code
                contentColor = Color(0xFFA9B7C6), // standard light text
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text(
                    text = block.codeText,
                    modifier = Modifier.padding(12.dp),
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
            }
        }
    }
}
