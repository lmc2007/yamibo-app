package me.thenano.yamibo.yamibo_app.userspace

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import io.github.littlesurvival.dto.page.ProfilePage
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlRenderer
import me.thenano.yamibo.yamibo_app.components.UserAvatar
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest

@Composable
internal fun UserSpaceProfileHeader(
    profile: ProfilePage,
    isSelf: Boolean,
    onNavigateSection: (UserSpaceSection, UserSpaceSubPage) -> Unit,
    onOpenMessageCenter: () -> Unit,
    onOpenWebView: (String, String) -> Unit,
) {
    val colors = YamiboTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .background(colors.brownPrimary.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            val backgroundUrl = profile.avatarBackgroundUrl
            if (!backgroundUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = rememberImageRequest(backgroundUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                UserAvatar(profile.avatarUrl, size = 64)
                Spacer(Modifier.height(10.dp))
                Text(profile.username, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }

        StatPanel(profile)
        ActionGrid(isSelf, onNavigateSection, onOpenMessageCenter)
        if (!profile.signatureHtml.isNullOrBlank()) {
            ProfileSignatureCard(profile.signatureHtml)
        }
        ProfileInfoTable(profile, onOpenHomepage = { url -> onOpenWebView(appString(Res.string.ui_personal_homepage), url) })
    }
}

@Composable
private fun StatPanel(profile: ProfilePage) {
    val colors = YamiboTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).offset(y = (-24).dp),
        shape = RoundedCornerShape(5.dp),
        color = colors.creamSurface,
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.45f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            ProfileStat(appString(Res.string.ui_total_points), profile.totalPoints.toString(), Modifier.weight(1f))
            ProfileStat(appString(Res.string.ui_integral), appString(Res.string.userspace_points_value, profile.points.toString()), Modifier.weight(1f))
            ProfileStat(appString(Res.string.ui_object), profile.partner.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier) {
    val colors = YamiboTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.textDark, fontSize = 18.sp)
        Text(label, color = colors.brownPrimary.copy(alpha = 0.55f), fontSize = 11.sp)
    }
}

private data class SpaceAction(
    val group: UserSpaceSection?,
    val initialTab: UserSpaceSubPage,
    val label: String,
    val opensMessageCenter: Boolean = false,
)

@Composable
private fun ActionGrid(
    isSelf: Boolean,
    onNavigateSection: (UserSpaceSection, UserSpaceSubPage) -> Unit,
    onOpenMessageCenter: () -> Unit,
) {
    val actions = if (isSelf) {
        listOf(
            SpaceAction(UserSpaceSection.Threads, UserSpaceSubPage.Threads, appString(Res.string.ui_my_threads)),
            SpaceAction(UserSpaceSection.Blogs, UserSpaceSubPage.FriendBlogs, appString(Res.string.ui_my_blogs)),
            SpaceAction(UserSpaceSection.Friends, UserSpaceSubPage.Friends, appString(Res.string.ui_my_friends)),
            SpaceAction(null, UserSpaceSubPage.Profile, appString(Res.string.ui_message_reminder), opensMessageCenter = true),
        )
    } else {
        listOf(
            SpaceAction(UserSpaceSection.Threads, UserSpaceSubPage.Threads, appString(Res.string.ui_their_threads)),
            SpaceAction(UserSpaceSection.Blogs, UserSpaceSubPage.MyBlogs, appString(Res.string.ui_their_blogs)),
            SpaceAction(UserSpaceSection.Threads, UserSpaceSubPage.Replies, appString(Res.string.ui_their_replies)),
            SpaceAction(null, UserSpaceSubPage.Profile, appString(Res.string.ui_add_as_friend)),
        )
    }
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).offset(y = (-12).dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            actions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { action ->
                        Surface(
                            onClick = {
                                when {
                                    action.opensMessageCenter -> onOpenMessageCenter()
                                    action.group != null -> onNavigateSection(action.group, action.initialTab)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = colors.creamBackground,
                        ) {
                            Text(
                                text = action.label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                color = colors.brownDeep,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileSignatureCard(signatureHtml: String?) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(appString(Res.string.ui_personal_signature), color = colors.brownDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            HtmlRenderer(html = signatureHtml.orEmpty(), modifier = Modifier.fillMaxWidth())
        }
    }
}

private data class ProfileInfoRow(
    val label: String,
    val value: String,
    val url: String? = null,
)

@Composable
private fun ProfileInfoTable(profile: ProfilePage, onOpenHomepage: (String) -> Unit) {
    val rows = listOfNotNull(
        ProfileInfoRow("UID", profile.uid.value.toString()),
        ProfileInfoRow(appString(Res.string.ui_user_group), profile.userGroup),
        profile.adminGroup?.let { ProfileInfoRow(appString(Res.string.ui_management_group), it) },
        ProfileInfoRow(appString(Res.string.ui_gender), profile.gender ?: "-"),
        ProfileInfoRow(appString(Res.string.ui_birthday), profile.birthday ?: "-"),
        profile.birthplace?.let { ProfileInfoRow(appString(Res.string.ui_place_birth), it) },
        profile.education?.let { ProfileInfoRow(appString(Res.string.ui_educational_qualifications), it) },
        profile.customTitle?.let { ProfileInfoRow(appString(Res.string.ui_custom_title), it) },
        profile.homepage?.let { ProfileInfoRow(appString(Res.string.ui_personal_homepage), it, it) },
        ProfileInfoRow(appString(Res.string.ui_online_time), appString(Res.string.userspace_online_hours_value, profile.onlineHours.toString())),
        ProfileInfoRow(appString(Res.string.ui_registration_time), profile.registerTime?.text ?: "-"),
        ProfileInfoRow(appString(Res.string.ui_last_visited), profile.lastVisit?.text ?: "-"),
    )
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(appString(Res.string.ui_personal_data), color = colors.brownDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            rows.forEach { row ->
                val url = row.url
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.label, modifier = Modifier.weight(1f), color = colors.textDark, fontSize = 14.sp)
                    Text(
                        text = row.value,
                        modifier = if (url == null) Modifier else Modifier.clickable { onOpenHomepage(url) },
                        color = if (url == null) colors.brownPrimary.copy(alpha = 0.75f) else colors.orangeAccent,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                HorizontalDivider(color = colors.brownLight.copy(alpha = 0.35f))
            }
        }
    }
}


