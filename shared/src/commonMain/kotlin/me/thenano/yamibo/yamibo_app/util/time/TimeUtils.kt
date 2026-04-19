package me.thenano.yamibo.yamibo_app.util.time

expect fun currentTimeMillis(): Long

fun currentLocalDateKey(): String {
    return currentLocalDateKeyAt(currentTimeMillis())
}

fun currentLocalDateKeyAt(epochMillis: Long): String {
    val utcPlus8OffsetMillis = 8L * 60L * 60L * 1000L
    val epochDay = (epochMillis + utcPlus8OffsetMillis).floorDiv(86_400_000L)
    val (year, month, day) = civilFromDays(epochDay)
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun civilFromDays(epochDay: Long): Triple<Int, Int, Int> {
    val z = epochDay + 719468L
    val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
    val doe = z - era * 146097L
    val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
    val y = yoe + era * 400L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val d = doy - (153L * mp + 2L) / 5L + 1L
    val m = mp + if (mp < 10L) 3L else -9L
    val year = (y + if (m <= 2L) 1L else 0L).toInt()
    return Triple(year, m.toInt(), d.toInt())
}
