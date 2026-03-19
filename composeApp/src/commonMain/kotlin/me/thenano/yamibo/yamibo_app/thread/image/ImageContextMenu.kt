package me.thenano.yamibo.yamibo_app.thread.image

import YamiboIcons
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.LocalPlatformContext
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/**
 * Long-press context menu for images.
 * Shows Copy, Share, Save actions.
 */
@Composable
fun ImageContextMenu(
    visible: Boolean,
    imageUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isBottomSheet: Boolean = false
) {
    if (isBottomSheet) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = null
                    ) { onDismiss() }
            ) {
                // Bottom sheet
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ContextMenuContainer(
                        imageUrl = imageUrl,
                        onDismiss = onDismiss,
                        isBottomSheet = true
                    )
                }
            }
        }
    } else {
        if (visible) {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = null
                        ) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    ContextMenuContainer(
                        imageUrl = imageUrl,
                        onDismiss = onDismiss,
                        isBottomSheet = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuContainer(
    imageUrl: String,
    onDismiss: () -> Unit,
    isBottomSheet: Boolean
) {
    val colors = YamiboTheme.colors
    val context = LocalPlatformContext.current
    val authRepo = LocalAuthRepository.current
    val scope = rememberCoroutineScope()
    
    val cookie = authRepo.cookieStore.load() ?: ""
    val referer = "https://bbs.yamibo.com/"

    Surface(
        color = colors.brownDeep,
        shape = if (isBottomSheet) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) else RoundedCornerShape(16.dp),
        modifier = if (isBottomSheet) Modifier.fillMaxWidth() else Modifier.wrapContentWidth().padding(32.dp)
    ) {
        Row(
            modifier = if (isBottomSheet) {
                Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 20.dp)
            } else {
                Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            },
            horizontalArrangement = if (isBottomSheet) Arrangement.SpaceEvenly else Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContextMenuItem(
                icon = YamiboIcons.Copy,
                label = "複製",
                onClick = {
                    scope.launch {
                        copyImageToClipboard(context, imageUrl, cookie, referer)
                        onDismiss()
                    }
                }
            )
            ContextMenuItem(
                icon = YamiboIcons.Share,
                label = "分享",
                onClick = {
                    scope.launch {
                        shareImageToApp(context, imageUrl, cookie, referer)
                        onDismiss()
                    }
                }
            )
            ContextMenuItem(
                icon = YamiboIcons.Save,
                label = "儲存",
                onClick = {
                    scope.launch {
                        saveImageToGallery(context, imageUrl, cookie, referer)
                        onDismiss()
                    }
                }
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
