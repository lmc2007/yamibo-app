package me.thenano.yamibo.yamibo_app.repository.download

fun interface DownloadBackgroundController {
    fun onQueueChanged(entries: List<DownloadQueueEntry>)

    companion object {
        val None = DownloadBackgroundController { }
    }
}
