package com.nish.flashcards.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// PM Insight: Data models define the product's domain.
// Flashcard apps have 3 core entities: Deck, Card, ReviewLog.
// This is the entire data model — simple, focused, single-purpose.

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sourceText: String = "",        // Original text used to generate cards
    val cardCount: Int = 0,             // Denormalized for fast display
    val createdAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long? = null
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deckId")]
)
data class Flashcard(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val front: String,
    val back: String,

    // SM-2 spaced repetition fields
    // PM Insight: These fields are invisible to users but are the core value.
    // The algorithm uses them to schedule reviews. Without them, we're just
    // a card viewer, not a learning tool.
    val easinessFactor: Float = 2.5f,    // SM-2: starts at 2.5, adjusts 1.3-2.5+
    val intervalDays: Int = 0,           // Days until next review
    val repetitions: Int = 0,            // Consecutive correct reviews
    val nextReviewDate: Long = System.currentTimeMillis(), // When to show next
    val lastReviewedAt: Long? = null
)

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = Flashcard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class ReviewLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cardId: String,
    val quality: Int,                    // 0=again, 3=hard, 4=good, 5=easy
    val reviewedAt: Long = System.currentTimeMillis()
)