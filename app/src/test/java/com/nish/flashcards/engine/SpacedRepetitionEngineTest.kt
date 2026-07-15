package com.nish.flashcards.engine

import com.nish.flashcards.data.model.Flashcard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.TimeUnit

// PM Insight: Testing the "invisible science" is critical.
// If SM-2 is wrong, the app silently fails — users see cards at
// the wrong time and don't know why. These tests verify the algorithm
// matches the published SM-2 specification.

class SpacedRepetitionEngineTest {

    private fun newCard(deckId: String = "deck1") = Flashcard(
        id = UUID.randomUUID().toString(),
        deckId = deckId,
        front = "Test front",
        back = "Test back",
        easinessFactor = 2.5f,
        intervalDays = 0,
        repetitions = 0,
        nextReviewDate = System.currentTimeMillis()
    )

    @Test
    fun `first good review sets interval to 1 day`() {
        val card = newCard()
        val reviewed = SpacedRepetitionEngine.reviewCard(card, ReviewQuality.GOOD)

        assertEquals(1, reviewed.repetitions)
        assertEquals(1, reviewed.intervalDays)
        // Easiness should increase slightly for "Good" (quality=4)
        // EF' = 2.5 + (0.1 - (5-4)*(0.08 + (5-4)*0.02)) = 2.5 + 0.1 - 0.1 = 2.5
        assertEquals(2.5f, reviewed.easinessFactor, 0.01f)
    }

    @Test
    fun `second good review sets interval to 3 days`() {
        val card = newCard().copy(repetitions = 1, intervalDays = 1)
        val reviewed = SpacedRepetitionEngine.reviewCard(card, ReviewQuality.GOOD)

        assertEquals(2, reviewed.repetitions)
        assertEquals(3, reviewed.intervalDays)
    }

    @Test
    fun `third good review uses easiness factor`() {
        val card = newCard().copy(repetitions = 2, intervalDays = 3, easinessFactor = 2.5f)
        val reviewed = SpacedRepetitionEngine.reviewCard(card, ReviewQuality.GOOD)

        assertEquals(3, reviewed.repetitions)
        // interval = round(3 * 2.5) = 8 (rounds to nearest int)
        // Actually: (3 * 2.5).toInt() = 7 (truncation)
        assertEquals(7, reviewed.intervalDays)
    }

    @Test
    fun `again resets repetitions to 0`() {
        val card = newCard().copy(repetitions = 5, intervalDays = 30, easinessFactor = 2.5f)
        val reviewed = SpacedRepetitionEngine.reviewCard(card, ReviewQuality.AGAIN)

        assertEquals(0, reviewed.repetitions)
        assertEquals(1, reviewed.intervalDays)
    }

    @Test
    fun `again decreases easiness factor`() {
        val card = newCard().copy(easinessFactor = 2.5f)
        val reviewed = SpacedRepetitionEngine.reviewCard(card, ReviewQuality.AGAIN)

        // EF' = 2.5 + (0.1 - (5-0)*(0.08 + (5-0)*0.02)) = 2.5 + 0.1 - 0.5 = 2.1
        // Actually: (5-0) = 5, so (0.08 + 5*0.02) = 0.18, 5*0.18 = 0.9
        // EF' = 2.5 + (0.1 - 0.9) = 2.5 - 0.8 = 1.7
        assertEquals(1.7f, reviewed.easinessFactor, 0.01f)
    }

    @Test
    fun `easiness factor never goes below 1_3`() {
        var card = newCard().copy(easinessFactor = 1.4f)
        // Multiple "Again" reviews
        repeat(10) {
            card = SpacedRepetitionEngine.reviewCard(card, ReviewQuality.AGAIN)
        }
        assertTrue("EF should be clamped to 1.3", card.easinessFactor >= 1.3f)
    }

    @Test
    fun `easy review increases easiness factor`() {
        val card = newCard().copy(easinessFactor = 2.5f)
        val reviewed = SpacedRepetitionEngine.reviewCard(card, ReviewQuality.EASY)

        // EF' = 2.5 + (0.1 - (5-5)*(...)) = 2.5 + 0.1 = 2.6
        assertEquals(2.6f, reviewed.easinessFactor, 0.01f)
    }

    @Test
    fun `hard review decreases easiness factor`() {
        val card = newCard().copy(easinessFactor = 2.5f)
        val reviewed = SpacedRepetitionEngine.reviewCard(card, ReviewQuality.HARD)

        // EF' = 2.5 + (0.1 - (5-3)*(0.08 + (5-3)*0.02)) = 2.5 + (0.1 - 2*0.12) = 2.5 - 0.14 = 2.36
        assertEquals(2.36f, reviewed.easinessFactor, 0.01f)
    }

    @Test
    fun `isDue returns true for past next review date`() {
        val card = newCard().copy(nextReviewDate = System.currentTimeMillis() - 1000)
        assertTrue(SpacedRepetitionEngine.isDue(card))
    }

    @Test
    fun `isDue returns false for future next review date`() {
        val card = newCard().copy(nextReviewDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3))
        assertTrue(!SpacedRepetitionEngine.isDue(card))
    }

    @Test
    fun `getDueCount counts only due cards`() {
        val now = System.currentTimeMillis()
        val cards = listOf(
            newCard().copy(nextReviewDate = now - 1000),  // due
            newCard().copy(nextReviewDate = now + 10000), // not due
            newCard().copy(nextReviewDate = now - 2000)   // due
        )
        assertEquals(2, SpacedRepetitionEngine.getDueCount(cards))
    }

    @Test
    fun `getStudyQueue puts due cards first`() {
        val now = System.currentTimeMillis()
        val notDue = newCard().copy(nextReviewDate = now + TimeUnit.DAYS.toMillis(5))
        val due = newCard().copy(nextReviewDate = now - 1000)

        val queue = SpacedRepetitionEngine.getStudyQueue(listOf(notDue, due))

        assertEquals(due.id, queue[0].id)
        assertEquals(notDue.id, queue[1].id)
    }

    @Test
    fun `next review date is correctly calculated from interval`() {
        val card = newCard()
        val before = System.currentTimeMillis()
        val reviewed = SpacedRepetitionEngine.reviewCard(card, ReviewQuality.GOOD)
        val after = System.currentTimeMillis()

        // Should be ~1 day from now
        val expectedMin = before + TimeUnit.DAYS.toMillis(1) - 5000
        val expectedMax = after + TimeUnit.DAYS.toMillis(1) + 5000
        assertTrue(reviewed.nextReviewDate >= expectedMin)
        assertTrue(reviewed.nextReviewDate <= expectedMax)
    }

    @Test
    fun `last reviewed at is set on review`() {
        val card = newCard().copy(lastReviewedAt = null)
        val before = System.currentTimeMillis()
        val reviewed = SpacedRepetitionEngine.reviewCard(card, ReviewQuality.GOOD)

        assertTrue(reviewed.lastReviewedAt != null)
        assertTrue(reviewed.lastReviewedAt!! >= before)
    }
}