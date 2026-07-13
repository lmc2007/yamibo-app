package me.thenano.yamibo.yamibo_app.components.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.thenano.yamibo.yamibo_app.LocalChineseConversionRepository

@Composable
fun rememberConvertedText(text: String): String {
    val repository = LocalChineseConversionRepository.current
    val mode by repository.currentMode.collectAsState()
    var converted by remember(text, mode) { mutableStateOf(text) }

    LaunchedEffect(text, mode) {
        converted = repository.convert(text)
    }

    return converted
}
