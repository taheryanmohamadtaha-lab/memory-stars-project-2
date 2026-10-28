package com.memorystars.leitner.domain

import java.time.Instant
import java.time.ZoneId

/** منطق خالص و آزمون‌پذیر لایتنر. */
object Leitner {
    val DEFAULT_INTERVALS = listOf(1, 2, 4, 7, 14)
    const val MAX_BOX = 5

    fun nextBoxCorrect(box: Int): Int = minOf(box + 1, MAX_BOX)
    fun nextBoxWrong(): Int = 1

    /** موعد بعدی: ابتدای روز (به وقت محلی) پس از [interval] روز از امروز. */
    fun nextDue(
        box: Int,
        now: Long,
        intervals: List<Int> = DEFAULT_INTERVALS,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val days = intervals.getOrElse(box - 1) { 1 }.coerceAtLeast(1)
        return Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            .plusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun isDue(dueAt: Long, now: Long): Boolean = dueAt <= now
}
