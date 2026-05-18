package me.thenano.yamibo.yamibo_app.profile.sign

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedAppMessage
import me.thenano.yamibo.yamibo_app.i18n.localizedMessage
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalSignRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.repository.SignRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.webview.IPlatformWebView

internal class ISignInfoScreen(
    private val onInfoLoaded: () -> Unit = {},
) : Navigatable {
    override val id = buildId("sign-info")

    @Composable
    override fun Content() {
        SignInfoScreen(onInfoLoaded = onInfoLoaded)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignInfoScreen(onInfoLoaded: () -> Unit) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val signRepository = LocalSignRepository.current
    val coroutineScope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf(signRepository.getCachedPageInfo()) }

    LaunchedEffect(Unit) {
        if (info != null) {
            onInfoLoaded()
        }
    }

    fun refreshInfo() {
        coroutineScope.launch {
            loading = true
            errorMessage = null
            /** This when converts repository sign-info results into the SignInfoScreen loading/error/render state. */
            when (val result = signRepository.fetchPageInfo()) {
                is YamiboResult.Success -> {
                    info = result.value
                    errorMessage = null
                    onInfoLoaded()
                }

                is YamiboResult.NotLoggedIn -> errorMessage = result.localizedMessage()
                is YamiboResult.NoPermission -> errorMessage = localizedAppMessage(result.reason)
                is YamiboResult.Maintenance -> errorMessage = result.localizedMessage()
                is YamiboResult.Failure -> errorMessage = localizedAppMessage(result.reason)
            }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = appString(Res.string.ui_sign_in_information),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Text(YamiboIcons.Back, color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.brownDeep,
                    scrolledContainerColor = colors.brownDeep,
                ),
            )
        },
        containerColor = colors.creamBackground,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = colors.brownPrimary,
                    )
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SectionCard(title = appString(Res.string.ui_read_failed), items = listOf(localizedAppMessage(errorMessage!!)))
                        OutlinedButton(
                            onClick = ::refreshInfo,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.creamBackground,
                                contentColor = colors.brownPrimary,
                            ),
                            border = BorderStroke(1.dp, colors.brownLight.copy(alpha = 0.65f)),
                        ) {
                            Text(appString(Res.string.ui_manually_refresh_information))
                        }
                        ElevatedButton(
                            onClick = { navigator.navigate(IPlatformWebView(YamiboRoute.Sign.build())) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.brownPrimary,
                                contentColor = colors.creamSurface,
                            ),
                        ) {
                            Text(appString(Res.string.ui_open_check_in_page_webview))
                        }
                    }
                }

                info != null -> {
                    val pageInfo = info!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val summaryItems = buildList {
                            pageInfo.currentDateText?.let { add(it) }
                            pageInfo.monthLabel?.let { add(appString(Res.string.sign_calendar_month, it)) }
                            add(if (pageInfo.hasSignedToday) appString(Res.string.ui_today_s_status_signed_in) else appString(Res.string.ui_today_s_status_not_signed_in))
                            if (pageInfo.repairOptions.isNotEmpty()) {
                                add(appString(Res.string.sign_repair_options, pageInfo.repairOptions.joinToString { it.label }))
                            }
                        }
                        if (summaryItems.isNotEmpty()) {
                            SectionCard(title = appString(Res.string.ui_summary), items = summaryItems)
                        }
                        pageInfo.notice?.let { SectionCard(title = appString(Res.string.ui_check_in_announcement), items = listOf(it)) }
                        if (pageInfo.calendarDays.isNotEmpty()) {
                            CalendarSectionCard(
                                title = appString(Res.string.ui_sign_in_form_for_month),
                                monthLabel = pageInfo.monthLabel,
                                days = pageInfo.calendarDays,
                            )
                        }
                        if (pageInfo.myActivity.isNotEmpty()) {
                            SectionCard(title = appString(Res.string.ui_my_check_in_status_2), items = pageInfo.myActivity)
                        }
                        if (pageInfo.statistics.isNotEmpty()) {
                            SectionCard(title = appString(Res.string.ui_check_in_statistics), items = pageInfo.statistics)
                        }
                        pageInfo.extraSections.forEach { section ->
                            SectionCard(title = section.title, items = section.items)
                        }
                        ElevatedButton(
                            onClick = { navigator.navigate(IPlatformWebView(YamiboRoute.Sign.build())) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.brownPrimary,
                                contentColor = colors.creamSurface,
                            ),
                        ) {
                            Text(appString(Res.string.ui_open_check_in_page_webview))
                        }
                        OutlinedButton(
                            onClick = ::refreshInfo,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.creamBackground,
                                contentColor = colors.brownPrimary,
                            ),
                            border = BorderStroke(1.dp, colors.brownLight.copy(alpha = 0.65f)),
                        ) {
                            Text(appString(Res.string.ui_manually_refresh_information))
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SectionCard(
                            title = appString(Res.string.ui_sign_in_information_has_not_obtained_yet),
                            items = listOf(appString(Res.string.ui_please_open_check_in_page_webview_first_let_program_capture)),
                        )
                        ElevatedButton(
                            onClick = { navigator.navigate(IPlatformWebView(YamiboRoute.Sign.build())) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.brownPrimary,
                                contentColor = colors.creamSurface,
                            ),
                        ) {
                            Text(appString(Res.string.ui_open_check_in_page_webview))
                        }
                        OutlinedButton(
                            onClick = ::refreshInfo,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.creamBackground,
                                contentColor = colors.brownPrimary,
                            ),
                            border = BorderStroke(1.dp, colors.brownLight.copy(alpha = 0.65f)),
                        ) {
                            Text(appString(Res.string.ui_manually_refresh_information))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSectionCard(
    title: String,
    monthLabel: String?,
    days: List<SignRepository.CalendarDay>,
) {
    val colors = YamiboTheme.colors
    val rows = days.chunked(7)
    val weekLabels = listOf(appString(Res.string.ui_one), appString(Res.string.ui_two), appString(Res.string.ui_three), appString(Res.string.ui_four), appString(Res.string.ui_five), appString(Res.string.ui_six), appString(Res.string.ui_day))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textDark,
            )
            monthLabel?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = colors.textDark.copy(alpha = 0.62f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                weekLabels.forEach { label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = colors.brownPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.brownPrimary,
                        )
                    }
                }
            }
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { day ->
                        CalendarDayCell(day = day)
                    }
                    repeat(7 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDayCell(day: SignRepository.CalendarDay) {
    val colors = YamiboTheme.colors
    val backgroundColor = when {
        day.isToday && day.isSigned -> colors.orangeAccent.copy(alpha = 0.20f)
        day.isToday -> colors.redAccent.copy(alpha = 0.12f)
        day.isSigned -> colors.brownPrimary.copy(alpha = 0.14f)
        else -> colors.creamBackground
    }
    val borderColor = when {
        day.isToday -> colors.redAccent.copy(alpha = 0.55f)
        day.isSigned -> colors.brownPrimary.copy(alpha = 0.32f)
        else -> colors.brownLight.copy(alpha = 0.2f)
    }
    val textColor = when {
        day.isToday -> colors.redAccent
        day.isSigned -> colors.brownPrimary
        else -> colors.textDark
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.day.toString(),
                fontSize = 15.sp,
                fontWeight = if (day.isToday || day.isSigned) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor,
            )
            Text(
                text = when {
                    day.isToday && day.isSigned -> appString(Res.string.ui_signed_today)
                    day.isToday -> appString(Res.string.ui_today_2)
                    day.isSigned -> appString(Res.string.ui_signed)
                    else -> ""
                },
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    items: List<String>,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textDark,
            )
            items.forEach { item ->
                Text(
                    text = item,
                    fontSize = 14.sp,
                    color = colors.textDark.copy(alpha = 0.82f),
                )
            }
        }
    }
}



