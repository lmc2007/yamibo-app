package io.github.littlesurvival

import io.github.littlesurvival.dto.value.ForumId

/**
 * 百合會論壇板塊列表
 *
 * All forums on bbs.yamibo.com with their forum IDs.
 */
enum class YamiboForum(val forumName: String, val forumId: ForumId) {

    // ── 庙堂 ──
    /** 管理版 */
    MANAGEMENT("管理版", ForumId(16)),
    /** 使用指南 */
    GUIDE("使用指南", ForumId(370)),

    // ── 江湖 ──
    /** 動漫區 - 请不要在莉莉安女子学院里狂奔……你给我站住！！ */
    ANIME("動漫區", ForumId(5)),
    /** 百合会最萌世界杯专版 (動漫區子版) */
    ADORABLE_WORLD_CUP("百合会最萌世界杯专版", ForumId(52)),

    /** 海域區 - 风声水起 */
    SEA("海域區", ForumId(33)),

    /** 貼圖區 - 玩悦图色 */
    STICKER("貼圖區", ForumId(13)),
    /** 原创图作区 (貼圖區子版) */
    ORIGINAL_WORK("原创图作区", ForumId(46)),
    /** 中文百合漫画区 (貼圖區子版) */
    TRANSLATED_YURI_MANGA("中文百合漫画区", ForumId(30)),
    /** 百合漫画图源区 (貼圖區子版) */
    YURI_MANGA_SOURCE("百合漫画图源区", ForumId(37)),

    /** 文學區 - 天方夜谭 */
    LITERATURE("文學區", ForumId(49)),
    /** 轻小说/译文区 (文學區子版) */
    TRANSLATED_LIGHT_NOVEL("轻小说/译文区", ForumId(55)),
    /** TXT小说区 (文學區子版) */
    TXT_NOVEL("TXT小说区", ForumId(60)),

    /** 遊戲區 - 游戏人间 */
    GAMING("遊戲區", ForumId(44)),

    /** 影視區 - 观剧磕糖 */
    MOVIE_VISUAL("影視區", ForumId(379)),

    /** 資源交流區 - 海纳百川 */
    RESOURCE("資源交流區", ForumId(19)),
    /** 非百合資源區 (資源交流區子版) */
    NON_YURI_RESOURCE("非百合資源區", ForumId(27));
    companion object {
        val NOVEL_THREADS = arrayOf(LITERATURE, TRANSLATED_LIGHT_NOVEL)
        val MANGA_THREADS = arrayOf(ORIGINAL_WORK, TRANSLATED_YURI_MANGA, YURI_MANGA_SOURCE)

        fun isNovelForum(name: String) = NOVEL_THREADS.any { it.forumName == name }
        fun isNovelForum(forumId: ForumId) = NOVEL_THREADS.any { it.forumId == forumId }
        fun isMangaForum(name: String) = MANGA_THREADS.any { it.forumName == name }
        fun isMangaForum(forumId: ForumId) = MANGA_THREADS.any { it.forumId == forumId }
    }
}
