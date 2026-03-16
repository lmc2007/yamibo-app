package me.thenano.yamibo.yamibo_app.thread.detail.novel.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Loading skeleton with shimmer */
@Composable
internal fun ThreadLoadingSkeleton() {
    val colors = YamiboTheme.colors
    val shimmerColor = colors.brownPrimary.copy(alpha = 0.12f)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val shimmerAnim = rememberInfiniteTransition(label = "thread_shimmer")
        val shimmerX by
        shimmerAnim.animateFloat(
            initialValue = -widthPx,
            targetValue = widthPx * 2f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
            label = "thread_shimmer_x"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colors.creamBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            /** Header placeholder */
            item {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .shimmer(shimmerX, shimmerColor)
                )
            }

            /** Content preview placeholder */
            item {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmer(shimmerX, shimmerColor)
                )
            }

            /** Page section placeholders */
            items(4) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .shimmer(shimmerX, shimmerColor)
                )
            }
        }
    }
}

/** Shimmer modifier */
private fun Modifier.shimmer(translateX: Float, baseColor: Color): Modifier =
    this.drawBehind {
        val brush =
            Brush.linearGradient(
                colors =
                    listOf(
                        baseColor.copy(alpha = 0.25f),
                        baseColor.copy(alpha = 0.50f),
                        baseColor.copy(alpha = 0.25f),
                    ),
                start = Offset(translateX, 0f),
                end = Offset(translateX + size.width, size.height)
            )
        drawRect(brush)
    }

/** Error content */
@Composable
internal fun ThreadErrorContent(message: String, onRetry: () -> Unit) {
    val colors = YamiboTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(colors.creamBackground).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "載入失敗",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.brownDeep
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = colors.brownPrimary.copy(alpha = 0.75f),
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(20.dp))
                Surface(
                    onClick = onRetry,
                    shape = RoundedCornerShape(50),
                    color = colors.brownDeep,
                    contentColor = Color.White
                ) {
                    Text(
                        text = "重試",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
