package me.thenano.yamibo.yamibo_app.forum.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.littlesurvival.dto.model.PageNav
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Page Navigation bar with prev/next and popup page picker */
@Composable
fun PageNavigation(pageNav: PageNav, onPageChange: (Int) -> Unit) {
    val colors = YamiboTheme.colors
    val current = pageNav.currentPage ?: 1
    val total = pageNav.totalPages ?: 1
    var showPagePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            /** prev button */
            PageButton(
                text = "上一頁",
                enabled = pageNav.prevUrl != null,
                onClick = { if (current > 1) onPageChange(current - 1) }
            )

            /** current page display / picker trigger */
            Surface(
                onClick = { showPagePicker = true },
                shape = RoundedCornerShape(12.dp),
                color = colors.brownDeep,
            ) {
                Text(
                    text = "第${current}頁",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            /** next button */
            PageButton(
                text = "下一頁",
                enabled = pageNav.nextUrl != null,
                onClick = { if (current < total) onPageChange(current + 1) }
            )
        }
    }

    /** Popup page picker dialog */
    if (showPagePicker) {
        PagePickerDialog(
            currentPage = current,
            totalPages = total,
            onPageSelected = { page ->
                //supress just for ignore ide clean up code warning, it's work fine.
                showPagePicker = false
                onPageChange(page)
            },
            onDismiss = {
                showPagePicker = false
            }
        )
    }
}

/** Prev/Next button */
@Composable
private fun PageButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) colors.creamSurface else colors.brownLight.copy(alpha = 0.3f),
        shadowElevation = if (enabled) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (enabled) colors.brownDeep else colors.brownLight,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

/** Dialog popup for page selection */
@Composable
private fun PagePickerDialog(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = YamiboTheme.colors
    val pages = (1..totalPages).toList()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "選擇頁面",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textDark
                )
                Spacer(Modifier.height(16.dp))

                /** Grid of page buttons */
                pages.chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { page ->
                            val isActive = page == currentPage
                            Surface(
                                onClick = { onPageSelected(page) },
                                shape = RoundedCornerShape(10.dp),
                                color =
                                    if (isActive) colors.brownDeep
                                    else colors.creamBackground,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$page",
                                    modifier =
                                        Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    color = if (isActive) Color.White else colors.textDark,
                                    fontWeight =
                                        if (isActive) FontWeight.Bold
                                        else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        /** fill remaining space */
                        val remaining = 5 - row.size
                        repeat(remaining) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(8.dp))

                /** Cancel button */
                TextButton(onClick = onDismiss) {
                    Text(text = "取消", color = colors.brownPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
