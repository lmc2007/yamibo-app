package me.thenano.yamibo.yamibo_app.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import me.thenano.yamibo.yamibo_app.repository.chineseconversion.ChineseConversionMode

class AndroidChineseConversionRepositoryTest {
    @Test
    fun traditionalConversionUsesTaiwanTerms() = runBlocking {
        val repository = AndroidChineseConversionRepository()
        repository.setConversionMode(ChineseConversionMode.Traditional)

        val converted = repository.convert("这个软件的网络视频质量很好，服务器信息已更新。")

        assertTrue("軟體" in converted, converted)
        assertTrue("網路" in converted, converted)
        assertTrue("影片" in converted, converted)
        assertTrue("伺服器" in converted, converted)
        assertTrue("資訊" in converted, converted)
    }

    @Test
    fun traditionalConversionKeepsCommonFaceAndSideWords() = runBlocking {
        val repository = AndroidChineseConversionRepository()
        repository.setConversionMode(ChineseConversionMode.Traditional)

        val converted = repository.convert("我在后面看桌面、页面、画面和界面。里面很干净。")

        assertEquals("我在後面看桌面、頁面、畫面和介面。裡面很乾淨。", converted)
    }

    @Test
    fun simplifiedConversionFixesCommonDryStemPhrases() = runBlocking {
        val repository = AndroidChineseConversionRepository()
        repository.setConversionMode(ChineseConversionMode.Simplified)

        val converted = repository.convert("幹部說乾杯，乾燥天氣不要干擾工作。")

        assertEquals("干部说干杯，干燥天气不要干扰工作。", converted)
    }
}
