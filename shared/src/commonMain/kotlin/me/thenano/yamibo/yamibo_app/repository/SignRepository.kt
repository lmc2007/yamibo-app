package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.core.YamiboResult

interface SignRepository {
    enum class ActionStatus {
        SUCCESS,
        ALREADY_SIGNED,
        REPAIR_SUCCESS,
    }

    data class CalendarDay(
        val day: Int,
        val isSigned: Boolean,
        val isToday: Boolean,
    )

    data class RepairOption(
        val value: String,
        val label: String,
    )

    data class InfoSection(
        val title: String,
        val items: List<String>,
    )

    data class SignPageInfo(
        val currentDateText: String?,
        val monthLabel: String?,
        val notice: String?,
        val calendarDays: List<CalendarDay>,
        val repairOptions: List<RepairOption>,
        val myActivity: List<String>,
        val statistics: List<String>,
        val extraSections: List<InfoSection>,
        val signActionUrl: String?,
        val repairActionPrefix: String?,
        val hasSignedToday: Boolean,
        val lastSignDateKey: String?,
    )

    data class DailyRecord(
        val dateKey: String,
        val isSigned: Boolean,
        val updatedAt: Long,
        val lastMessage: String?,
    )

    data class ActionResult(
        val status: ActionStatus,
        val message: String,
        val repairCount: Int,
        val pageInfo: SignPageInfo,
    )

    suspend fun fetchPageInfo(): YamiboResult<SignPageInfo>

    suspend fun runAutoSign(allowRepair: Boolean): YamiboResult<ActionResult>

    suspend fun getTodayRecord(): DailyRecord?

    fun getKnownSignedToday(): Boolean?

    suspend fun isSignedToday(): Boolean

    suspend fun markTodaySigned(message: String? = null)

    fun getCachedPageInfo(): SignPageInfo?

    fun cacheObservedHtml(html: String): SignPageInfo?
}
