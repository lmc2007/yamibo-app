package me.thenano.yamibo.yamibo_app.repository

interface SystemNotificationRepository {
    data class ProgressNotificationModel(
        val notificationId: Int,
        val title: String,
        val text: String,
        val progress: Int,
        val indeterminate: Boolean,
        val ongoing: Boolean,
        val canCancel: Boolean,
        val runId: String? = null,
    )

    suspend fun showProgress(model: ProgressNotificationModel)

    suspend fun showCompleted(notificationId: Int, title: String, text: String)

    suspend fun showFailed(notificationId: Int, title: String, text: String)

    suspend fun dismiss(notificationId: Int)
}
