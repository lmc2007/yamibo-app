package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.dto.value.ThreadId

class AndroidReadHistoryRepository : ReadHistoryRepository {

    override fun getThreadPosition(tid: ThreadId): ReadHistoryRepository.ThreadReadPosition? {
        TODO("Not yet implemented")
    }

    override fun saveThreadPosition(position: ReadHistoryRepository.ThreadReadPosition) {
        TODO("Not yet implemented")
    }

    override fun getMangaPosition(tid: ThreadId): Any? {
        TODO("Not yet implemented")
    }

    override fun saveMangaPosition(tid: ThreadId, position: Any) {
        TODO("Not yet implemented")
    }
}
