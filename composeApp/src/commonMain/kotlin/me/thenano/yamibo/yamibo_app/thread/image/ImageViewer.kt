package me.thenano.yamibo.yamibo_app.thread.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import org.jetbrains.compose.resources.painterResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.image_icon

val LocalReaderOverlayVisible = compositionLocalOf { false }
val LocalImageClickListener = compositionLocalOf<(() -> Unit)?> { null }
val LocalImageDoubleClickListener = compositionLocalOf<((String) -> Unit)?> { null }

/**
 * A unified image viewer for posts and manga reading.
 * Includes loading spinners, detailed error messaging, a retry button,
 * and an optional long-press context menu (`MangaImageContextMenu`).
 */
@Composable
fun ImageViewer(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.FillWidth,
    enableContextMenu: Boolean = true,
    isDarkTheme: Boolean = false,
    enableCrossfade: Boolean = true
) {
    var retryKey by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    val context = LocalPlatformContext.current
    val authRepo = LocalAuthRepository.current
    val colors = YamiboTheme.colors
    val isOverlayOpen = LocalReaderOverlayVisible.current

    val onSingleTap = LocalImageClickListener.current
    val onDoubleTap = LocalImageDoubleClickListener.current
    
    // Formatting styles
    val errorBgColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFF3F3F3)
    val errorTextColor = if (isDarkTheme) Color.White else colors.textDark
    val errorSubTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else colors.textDark.copy(alpha = 0.6f)
    val errorUrlColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else colors.brownDeep
    
    // Resolve full URL
    val fullUrl = if (url.startsWith("http")) url else "https://bbs.yamibo.com/$url"

    // Construct Cookie and Referer headers
    val cookie = authRepo.cookieStore.load() ?: ""
    val referer = "https://bbs.yamibo.com/"

    // Build the Image Request with Header caching and crossfade
    val imageRequest = remember(fullUrl, cookie, retryKey, enableCrossfade) {
        val builder = ImageRequest.Builder(context)
            .data(fullUrl)
            .httpHeaders(
                NetworkHeaders.Builder()
                    .add("Cookie", cookie)
                    .add("Referer", referer)
                    .build()
            )
            .crossfade(enableCrossfade)

        if (retryKey > 0) {
            // Bypass memory/disk cache if the user explicitly clicked "Retry"
            builder.memoryCachePolicy(CachePolicy.READ_ONLY)
                .diskCachePolicy(CachePolicy.READ_ONLY)
        }
        builder.build()
    }

    Box(
        modifier = modifier.then(
            Modifier.pointerInput(isOverlayOpen, enableContextMenu) {
                detectTapGestures(
                    onTap = {
                        // Let single tap toggle the overlay
                        onSingleTap?.invoke()
                    },
                    onDoubleTap = {
                        onDoubleTap?.invoke(fullUrl)
                    },
                    onLongPress = {
                        if (enableContextMenu && !isOverlayOpen) {
                            showMenu = true
                        }
                    }
                )
            }
        ),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = contentDescription ?: "Yamibo Image",
            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
            contentScale = contentScale
        ) {
            val state by painter.state.collectAsState()
            when (state) {
                is AsyncImagePainter.State.Loading, is AsyncImagePainter.State.Empty -> {
                    // Loading State
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.brownPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                is AsyncImagePainter.State.Error -> {
                    // Error State
                    val errorState = state as AsyncImagePainter.State.Error
                    val errorMsg = errorState.result.throwable.message ?: "Unknown Error"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(errorBgColor, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.image_icon),
                            contentDescription = "Image Load Failed",
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "圖片載入失敗",
                            color = errorTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = errorMsg,
                            color = errorSubTextColor,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = fullUrl,
                            color = errorUrlColor,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(textDecoration = TextDecoration.Underline)
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { retryKey++ },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.brownPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "重新載入",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                is AsyncImagePainter.State.Success -> {
                    // Success State
                    SubcomposeAsyncImageContent()
                }
            }
        }

        // Context Menu Pop-Up
        ImageContextMenu(
            visible = enableContextMenu && showMenu,
            imageUrl = fullUrl,
            onDismiss = { showMenu = false }
        )
    }
}
