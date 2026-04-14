package me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.page.Poll
import io.github.littlesurvival.dto.page.PollStatus
import io.github.littlesurvival.dto.page.PollType
import io.github.littlesurvival.dto.value.PollOptionId
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun PollRenderer(
    poll: Poll,
    modifier: Modifier = Modifier,
    onVote: ((List<PollOptionId>) -> Unit)? = null
) {
    val colors = YamiboTheme.colors
    var selectedOptions by remember { mutableStateOf(setOf<PollOptionId>()) }
    val isNotVoted = poll.status == PollStatus.NotVoted
    val isMultipleChoice = poll.type == PollType.MultipleChoice

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

            if (poll.endTime.text.isNotEmpty()) {
                Text(
                    text = poll.endTime.specialText ?: poll.endTime.text,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isNotVoted) {
                            if (isMultipleChoice) {
                                Checkbox(
                                    checked = selectedOptions.contains(option.option),
                                    onCheckedChange = { checked ->
                                        selectedOptions = if (checked) {
                                            selectedOptions + option.option
                                        } else {
                                            selectedOptions - option.option
                                        }
                                    },
                                    modifier = Modifier.padding(end = 8.dp).size(24.dp),
                                    colors = CheckboxDefaults.colors(checkedColor = colors.brownPrimary)
                                )
                            } else {
                                RadioButton(
                                    selected = selectedOptions.contains(option.option),
                                    onClick = { selectedOptions = setOf(option.option) },
                                    modifier = Modifier.padding(end = 8.dp).size(24.dp),
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.brownPrimary)
                                )
                            }
                        }

                        Text(
                            text = "${index + 1}. ${option.optionName}",
                            color = colors.textDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

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
                        val percentageScaled = ((option.percentage ?: 0f) * 100).toInt() / 100f
                        Text(
                            text = "${percentageScaled}% (${option.totalVoted ?: 0})",
                            color = colors.textDark.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.widthIn(min = 60.dp)
                        )
                    }
                }
            }

            // Submit Button
            if (isNotVoted && onVote != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onVote(selectedOptions.toList()) },
                    enabled = selectedOptions.isNotEmpty(),
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brownPrimary,
                        disabledContainerColor = colors.brownLight.copy(alpha = 0.5f)
                    )
                ) {
                    Text(text = "提交投票", color = colors.creamBackground)
                }
            }
        }
    }
}
