package com.memorystars.leitner

import com.memorystars.leitner.data.AppSettings
import com.memorystars.leitner.data.CardEntity
import com.memorystars.leitner.data.DeckEntity
import com.memorystars.leitner.data.ReviewLog
import com.memorystars.leitner.domain.Leitner
import com.memorystars.leitner.domain.Rewards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LeitnerTest {
    private val utc = ZoneId.of("UTC")
    private val now = 1_700_000_000_000L // 2023-11-14T22:13:20Z
    private val day = 24L * 3600 * 1000

    @Test fun correctMovesOneBox() {
        assertEquals(2, Leitner.nextBoxCorrect(1))
        assertEquals(5, Leitner.nextBoxCorrect(4))
    }

    @Test fun boxFiveStaysOnCorrect() = assertEquals(5, Leitner.nextBoxCorrect(5))
    @Test fun wrongGoesToBoxOne() = assertEquals(1, Leitner.nextBoxWrong())

    @Test fun dueDateFollowsBoxInterval() {
        val startOfToday = now - now % day
        assertEquals(startOfToday + 1 * day, Leitner.nextDue(1, now, zone = utc))
        assertEquals(startOfToday + 4 * day, Leitner.nextDue(3, now, zone = utc))
        assertEquals(startOfToday + 14 * day, Leitner.nextDue(5, now, zone = utc))
    }

    @Test fun customIntervalsAreUsed() {
        val startOfToday = now - now % day
        assertEquals(startOfToday + 3 * day, Leitner.nextDue(2, now, listOf(1, 3, 5, 9, 20), utc))
    }

    @Test fun dueDetection() {
        assertTrue(Leitner.isDue(now, now))
        assertFalse(Leitner.isDue(now + 1, now))
    }

    @Test fun statsCountsDueAndAccuracy() {
        val decks = listOf(DeckEntity(1, "d"))
        val cards = listOf(
            CardEntity(1, 1, "a", "b", dueAt = now - 1),
            CardEntity(2, 1, "c", "d", box = 4, dueAt = now + day)
        )
        val s0 = Rewards.compute(decks, cards, emptyList(), AppSettings(), now, utc)
        assertEquals(1, s0.due)
        assertEquals(1, s0.learned)
        assertNull(s0.accuracy)
        val logs = listOf(
            ReviewLog(1, 1, 1, now, true, 10),
            ReviewLog(2, 2, 1, now, false, 3),
            ReviewLog(3, 2, 1, now, true, 3, retry = true)
        )
        val s1 = Rewards.compute(decks, cards, logs, AppSettings(bonusXp = 15), now, utc)
        assertEquals(50, s1.accuracy)
        assertEquals(31, s1.xp)
        assertEquals(2, s1.reviewsToday)
    }

    @Test fun streakAndLevels() {
        val t = LocalDate.of(2024, 1, 10)
        val days = setOf(t, t.minusDays(1), t.minusDays(2), t.minusDays(5))
        assertEquals(3, Rewards.streak(days, t))
        assertEquals(3, Rewards.longestStreak(days))
        assertEquals(0, Rewards.streak(setOf(t.minusDays(3)), t))
        assertEquals(0, Rewards.levelFor(0))
        assertEquals(1, Rewards.levelFor(60))
        assertEquals(5, Rewards.levelFor(99999))
    }
}
