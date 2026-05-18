package me.thenano.yamibo.yamibo_app.favorite.updates

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedAppMessage
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun FavoriteUpdateStatusCard(
    state: FavoriteUpdateRepository.RunState,
    modifier: Modifier = Modifier,
    onCancel: ((String) -> Unit)? = null,
    onInterrupt: ((String) -> Unit)? = null,
    onResume: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
) {
    val colors = YamiboTheme.colors
    val snapshot = state.snapshotOrNull() ?: return
    val total = snapshot.totalCount.coerceAtLeast(0)
    val processed = (snapshot.completedCount + snapshot.skippedCount + snapshot.failedCount).coerceAtMost(total)
    val progress = if (total > 0) processed.toFloat() / total.toFloat() else 0f
    val boundedProgress = progress.coerceIn(0f, 1f)

    Surface(
        modifier = modifier.heightIn(max = 184.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = colors.creamSurface,
        border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title(),
                        color = colors.brownDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = snapshot.currentItem?.let(::localizedAppMessage) ?: snapshot.phase.name,
                        color = colors.textDark.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "$processed/${snapshot.totalCount}",
                    color = colors.brownDeep,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }

            LinearProgressIndicator(
                progress = { boundedProgress },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = colors.brownDeep,
                trackColor = colors.brownPrimary.copy(alpha = 0.16f),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricText(appString(Res.string.ui_renew), snapshot.detectedCount)
                MetricText(appString(Res.string.ui_jump_over), snapshot.skippedCount)
                MetricText(appString(Res.string.ui_mistake), snapshot.failedCount)
            }

            snapshot.warningMessage?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = localizedAppMessage(it.lineSequence().lastOrNull().orEmpty()),
                    color = colors.brownDeep,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            snapshot.errorMessage?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = localizedAppMessage(it),
                    color = Color(0xFFB74D42),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state is FavoriteUpdateRepository.RunState.Running && onCancel != null) {
                    CompactStatusButton(
                        text = appString(Res.string.common_cancel),
                        onClick = { onCancel(snapshot.runId) },
                        containerColor = Color(0xFF8C4B3B),
                        contentColor = Color.White,
                    )
                }
                if (state is FavoriteUpdateRepository.RunState.Running && onInterrupt != null) {
                    CompactStatusButton(
                        text = appString(Res.string.ui_interrupt),
                        onClick = { onInterrupt(snapshot.runId) },
                        containerColor = colors.brownPrimary.copy(alpha = 0.18f),
                        contentColor = colors.brownDeep,
                    )
                }
                if (state is FavoriteUpdateRepository.RunState.Interrupted && onResume != null) {
                    CompactStatusButton(
                        text = appString(Res.string.ui_continue),
                        onClick = onResume,
                        containerColor = Color(0xFF8C4B3B),
                        contentColor = Color.White,
                    )
                }
                if (state !is FavoriteUpdateRepository.RunState.Running && onHide != null) {
                    CompactStatusButton(
                        text = appString(Res.string.ui_hide),
                        onClick = onHide,
                        containerColor = colors.brownPrimary.copy(alpha = 0.18f),
                        contentColor = colors.brownDeep,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactStatusButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(34.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetricText(label: String, value: Int) {
    val colors = YamiboTheme.colors
    Text(
        text = "$label $value",
        color = colors.textDark.copy(alpha = 0.68f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

fun FavoriteUpdateRepository.RunState.snapshotOrNull(): FavoriteUpdateRepository.RunSnapshot? =
    when (this) {
        FavoriteUpdateRepository.RunState.Idle -> null
        is FavoriteUpdateRepository.RunState.Running -> snapshot
        is FavoriteUpdateRepository.RunState.Interrupted -> snapshot
        is FavoriteUpdateRepository.RunState.Failed -> snapshot
        is FavoriteUpdateRepository.RunState.Completed -> snapshot
    }

private fun FavoriteUpdateRepository.RunState.title(): String =
    when (this) {
        FavoriteUpdateRepository.RunState.Idle -> appString(Res.string.ui_favorite_updates)
        is FavoriteUpdateRepository.RunState.Running -> appString(Res.string.ui_checking_for_favorites_updates)
        is FavoriteUpdateRepository.RunState.Interrupted -> appString(Res.string.ui_favorite_update_has_interrupted)
        is FavoriteUpdateRepository.RunState.Failed -> appString(Res.string.ui_favorite_update_failed)
        is FavoriteUpdateRepository.RunState.Completed -> appString(Res.string.ui_favorite_update_completed)
    }

