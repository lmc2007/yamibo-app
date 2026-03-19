package me.thenano.yamibo.yamibo_app.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.thenano.yamibo.yamibo_app.LocalAuthRepository

@Composable
fun ProfilePage() {
    val authRepository = LocalAuthRepository.current
    var userInfo by remember { mutableStateOf(authRepository.currentUser()) }
    var isLoading by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
    ) {
        // top profile card
        UserProfileCard(
            userInfo = userInfo,
            isLoading = isLoading,
            onRefresh = {
                isLoading = true
                authRepository.fetchStatus()
                userInfo = authRepository.currentUser()
                isLoading = false
            },
            onLogout = {
                isLoading = true
                authRepository.logOut()
                userInfo = authRepository.currentUser()
                isLoading = false
            },
            modifier = Modifier.padding(top = 12.dp)
        )
        Spacer(Modifier.height(8.dp))

        // content blocks
        SectionCard(title = "功能區塊 A", description = "這裡可以放設定、收藏、歷史紀錄等內容")

        SectionCard(title = "功能區塊 B", description = "可用於顯示統計、會員資訊或快捷入口")

        SectionCard(title = "功能區塊 C", description = "任何你未來想擴充的模組都可以放在這")

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    onClick: () -> Unit = {},
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("進入") }
        }
    }
}
