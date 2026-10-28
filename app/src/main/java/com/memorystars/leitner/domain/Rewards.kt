package com.memorystars.leitner.domain

import com.memorystars.leitner.data.AppSettings
import com.memorystars.leitner.data.CardEntity
import com.memorystars.leitner.data.DeckEntity
import com.memorystars.leitner.data.ReviewLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Level(val name: String, val emoji: String, val minXp: Int)
data class Badge(val id: String, val title: String, val emoji: String, val desc: String)
data class DeckStat(val deck: DeckEntity, val total: Int, val learned: Int)

data class Stats(
    val totalCards: Int = 0,
    val learned: Int = 0,
    val practice: Int = 0,
    val due: Int = 0,
    val reviewsToday: Int = 0,
    val totalReviews: Int = 0,
    val correctReviews: Int = 0,
    val distinctReviewed: Int = 0,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val xp: Int = 0,
    val sessions: Int = 0,
    val goalEver: Boolean = false,
    val deckLearned: Boolean = false,
    val perDay: List<Pair<LocalDate, Int>> = emptyList(),
    val deckStats: List<DeckStat> = emptyList()
) {
    val level: Int get() = Rewards.levelFor(xp)
    /** درصد پاسخ درست در اولین تلاش؛ null یعنی هنوز داده‌ای نیست. */
    val accuracy: Int? get() = if (totalReviews == 0) null else correctReviews * 100 / totalReviews
}

object Rewards {
    const val XP_CORRECT = 10
    const val XP_TRY = 3
    const val XP_SESSION = 15
    const val XP_GOAL = 25

    val levels = listOf(
        Level("جوجه‌ستاره", "🐣", 0),
        Level("کاوشگر کوچک", "🧭", 60),
        Level("شاگرد باهوش", "🎒", 200),
        Level("دانشمند کوچولو", "🔬", 450),
        Level("قهرمان حافظه", "🏆", 800),
        Level("استاد ستاره‌ها", "👑", 1300)
    )

    val badges = listOf(
        Badge("first_card", "اولین فلش‌کارت", "🃏", "یک فلش‌کارت بساز"),
        Badge("first_session", "اولین جلسه کامل", "🎯", "یک جلسه مرور را تا آخر انجام بده"),
        Badge("r10", "۱۰ کارت مرورشده", "🌟", "۱۰ کارت مختلف را مرور کن"),
        Badge("r50", "۵۰ کارت مرورشده", "⭐", "۵۰ کارت مختلف را مرور کن"),
        Badge("r100", "۱۰۰ کارت مرورشده", "💫", "۱۰۰ کارت مختلف را مرور کن"),
        Badge("s3", "سه روز پشت‌سرهم", "🔥", "سه روز متوالی مرور کن"),
        Badge("s7", "هفت روز پشت‌سرهم", "🌈", "هفت روز متوالی مرور کن"),
        Badge("goal", "هدف روزانه", "🎈", "یک بار به هدف روزانه برس"),
        Badge("deck", "یادگیری یک دسته", "🏅", "همه کارت‌های یک دسته را به جعبه ۴ یا ۵ برسان")
    )

    fun levelFor(xp: Int): Int = levels.indexOfLast { xp >= it.minXp }.coerceAtLeast(0)

    fun unlocked(id: String, s: Stats): Boolean = when (id) {
        "first_card" -> s.totalCards >= 1
        "first_session" -> s.sessions >= 1
        "r10" -> s.distinctReviewed >= 10
        "r50" -> s.distinctReviewed >= 50
        "r100" -> s.distinctReviewed >= 100
        "s3" -> s.longestStreak >= 3
        "s7" -> s.longestStreak >= 7
        "goal" -> s.goalEver
        "deck" -> s.deckLearned
        else -> false
    }

    fun streak(days: Set<LocalDate>, today: LocalDate): Int {
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        while (d in days) { n++; d = d.minusDays(1) }
        return n
    }

    fun longestStreak(days: Set<LocalDate>): Int {
        var best = 0; var cur = 0; var prev: LocalDate? = null
        for (d in days.sorted()) {
            cur = if (prev != null && prev.plusDays(1) == d) cur + 1 else 1
            if (cur > best) best = cur
            prev = d
        }
        return best
    }

    fun compute(
        decks: List<DeckEntity>, cards: List<CardEntity>, logs: List<ReviewLog>,
        s: AppSettings, now: Long, zone: ZoneId = ZoneId.systemDefault()
    ): Stats {
        fun day(t: Long) = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()
        val today = day(now)
        val byDay = logs.groupBy { day(it.at) }
        val first = logs.filter { !it.retry }
        val deckStats = decks.map { d ->
            val cs = cards.filter { it.deckId == d.id }
            DeckStat(d, cs.size, cs.count { it.box >= 4 })
        }
        return Stats(
            totalCards = cards.size,
            learned = cards.count { it.box >= 4 },
            practice = cards.count { it.wrong > 0 && it.box <= 2 },
            due = cards.count { Leitner.isDue(it.dueAt, now) },
            reviewsToday = byDay[today]?.map { it.cardId }?.toSet()?.size ?: 0,
            totalReviews = first.size,
            correctReviews = first.count { it.correct },
            distinctReviewed = logs.map { it.cardId }.toSet().size,
            streak = streak(byDay.keys, today),
            longestStreak = longestStreak(byDay.keys),
            xp = logs.sumOf { it.xp } + s.bonusXp,
            sessions = s.sessions,
            goalEver = s.goalDay.isNotEmpty(),
            deckLearned = deckStats.any { it.total > 0 && it.learned == it.total },
            perDay = (6 downTo 0).map { i ->
                val d = today.minusDays(i.toLong())
                d to (byDay[d]?.count { !it.retry } ?: 0)
            },
            deckStats = deckStats
        )
    }
}
