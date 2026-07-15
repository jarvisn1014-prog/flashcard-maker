package com.nish.flashcards.engine

import com.nish.flashcards.data.model.Flashcard
import java.util.concurrent.TimeUnit

// PM Insight: This is the "invisible science" that makes the app work.
// SM-2 is the algorithm used by Anki (the gold standard for spaced repetition).
// It's not AI — it's cognitive science from 1987. But it's what makes
// flashcard apps actually work vs. just being card viewers.
//
// The algorithm:
// 1. User rates recall quality (0-5): Again=0, Hard=3, Good=4, Easy=5
// 2. If quality < 3: reset repetitions to 0, interval = 1 day
// 3. If quality >= 3:
//    - repetitions == 0: interval = 1 day
//    - repetitions == 1: interval = 3 days
//    - repetitions >= 2: interval = round(previousInterval * easinessFactor)
// 4. Update easinessFactor: EF = EF + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
//    - Clamp EF to minimum 1.3
// 5. Schedule next review: now + interval days

object SpacedRepetitionEngine {

    fun reviewCard(card: Flashcard, quality: ReviewQuality): Flashcard {
        val q = quality.value

        val newRepetitions: Int
        val newInterval: Int
        val newEasiness: Float

        if (q < 3) {
            // Failed recall — start over
            newRepetitions = 0
            newInterval = 1
        } else {
            newRepetitions = card.repetitions + 1
            newInterval = when (card.repetitions) {
                0 -> 1
                1 -> 3
                else -> (card.intervalDays * card.easinessFactor).toInt().coerceAtLeast(1)
            }
        }

        // Update easiness factor
        // Formula: EF' = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02))
        newEasiness = (card.easinessFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).toFloat()
        newEasiness = newEasiness.coerceIn(1.3f, 2.8f)

        val nextReviewMs = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(newInterval.toLong())

        return card.copy(
            easinessFactor = newEasiness,
            intervalDays = newInterval,
            repetitions = newRepetitions,
            nextReviewDate = nextReviewMs,
            lastReviewedAt = System.currentTimeMillis()
        )
    }

    fun isDue(card: Flashcard, now: Long = System.currentTimeMillis()): Boolean {
        return card.nextReviewDate <= now
    }

    fun getDueCount(cards: List<Flashcard>, now: Long = System.currentTimeMillis()): Int {
        return cards.count { isDue(it, now) }
    }

    // Get study order: due cards first (oldest first), then new cards
    fun getStudyQueue(cards: List<Flashcard>, now: Long = System.currentTimeMillis()): List<Flashcard> {
        return cards.sortedWith(
            compareBy<Flashcard>(
                { if (isDue(it, now)) 0 else 1 },  // Due first
                { it.nextReviewDate }               // Then by oldest due
            )
        )
    }
}

enum class ReviewQuality(val value: Int, val label: String) {
    AGAIN(0, "Again"),
    HARD(3, "Hard"),
    GOOD(4, "Good"),
    EASY(5, "Easy")
}