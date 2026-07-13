package me.thenano.yamibo.yamibo_app.profile.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

/** Slider row used by settings pages. */
@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueDisplay: (Float) -> String = { it.toString() },
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YamiboTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textDark
            )
            Text(
                text = valueDisplay(value),
                fontSize = 14.sp,
                color = colors.brownPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = colors.brownPrimary,
                activeTrackColor = colors.brownPrimary,
                inactiveTrackColor = colors.brownLight.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
