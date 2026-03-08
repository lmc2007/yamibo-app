package me.thenano.yamibo.yamibo_app.thread.render.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.page.Poll
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun PollRenderer(poll: Poll, modifier: Modifier = Modifier) {
    val colors = YamiboTheme.colors

    Surface(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = colors.creamSurface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📊",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = poll.pollInfo,
                    color = colors.brownPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (poll.endTime.isNotEmpty()) {
                Text(
                    text = poll.endTime,
                    color = colors.textDark.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Options
            poll.option.forEachIndexed { index, option ->
                val progress = (option.percentage ?: 0f) / 100f
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        text = "${index + 1}. ${option.optionName}",
                        color = colors.textDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = colors.brownPrimary,
                            trackColor = colors.creamBackground,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Label
                        Text(
                            text = String.format("%.2f%% (%d)", option.percentage ?: 0f, option.totalVoted ?: 0),
                            color = colors.textDark.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.widthIn(min = 60.dp)
                        )
                    }
                }
            }
        }
    }
}
