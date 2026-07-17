package com.nish.flashcards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nish.flashcards.data.local.FlashcardDatabase
import com.nish.flashcards.data.model.Deck
import com.nish.flashcards.data.model.Flashcard
import com.nish.flashcards.data.model.ReviewLog
import com.nish.flashcards.data.remote.FlashcardService
import com.nish.flashcards.engine.ReviewQuality
import com.nish.flashcards.engine.SpacedRepetitionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// PM Insight: The ViewModel is the bridge between data and UI.
// It holds all state — the UI never touches the database or API directly.
// This separation is critical: it means you can change the data layer
// (add Supabase, switch to OpenAI) without touching the UI.

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FlashcardDatabase.getInstance(application)
    private val service = FlashcardService()

    // ─── Reactive data flows ───
    val decks: StateFlow<List<Deck>> = db.deckDao().getAllDecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── API key ───
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    init {
        viewModelScope.launch {
            SettingsStore.getApiKey(application).collect { key ->
                _apiKey.value = key
            }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            SettingsStore.setApiKey(getApplication(), key)
        }
    }

    // ─── Card generation ───
    private val _generationState = MutableStateFlow<UiState<List<Flashcard>>>(UiState.Idle)
    val generationState: StateFlow<UiState<List<Flashcard>>> = _generationState.asStateFlow()

    private val _pendingCards = MutableStateFlow<List<Flashcard>>(emptyList())
    val pendingCards: StateFlow<List<Flashcard>> = _pendingCards.asStateFlow()

    // Tracks the deckId for the currently pending generated cards,
    // so savePendingCards can be called without requiring the UI to pass it back.
    private val _pendingDeckId = MutableStateFlow<String?>(null)

    fun generateFlashcards(deckName: String, sourceText: String) {
        val key = _apiKey.value
        if (key.isBlank()) {
            _generationState.value = UiState.Error("No API key set. Go to Settings to add your free Gemini API key.")
            return
        }
        if (sourceText.isBlank()) {
            _generationState.value = UiState.Error("Please enter some text to generate flashcards from.")
            return
        }

        // Set Loading synchronously BEFORE launching the coroutine so the UI
        // observes the loading state before navigation completes (fixes race).
        _generationState.value = UiState.Loading

        viewModelScope.launch {
            try {
                // First create the deck — use deck.id (UUID PK), NOT the Long
                // SQLite rowId returned by insertDeck (fixes deckId mismatch).
                val deck = Deck(name = deckName, sourceText = sourceText)
                db.deckDao().insertDeck(deck)

                // Generate cards via AI — pass the real UUID deck id
                val result = service.generateFlashcards(key, sourceText, deck.id)
                result.fold(
                    onSuccess = { cards ->
                        _pendingDeckId.value = deck.id
                        _pendingCards.value = cards
                        _generationState.value = UiState.Success(cards)
                    },
                    onFailure = { error ->
                        _generationState.value = UiState.Error(error.message ?: "Failed to generate flashcards")
                        // Delete the empty deck since generation failed
                        db.deckDao().deleteDeck(deck)
                    }
                )
            } catch (e: Exception) {
                _generationState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Save the pending cards to the database (after user review)
    fun savePendingCards() {
        val deckId = _pendingDeckId.value ?: return
        viewModelScope.launch {
            val cards = _pendingCards.value
            if (cards.isNotEmpty()) {
                db.cardDao().insertCards(cards)
                db.deckDao().updateCardCount(deckId, cards.size)
                _pendingCards.value = emptyList()
                _pendingDeckId.value = null
                _generationState.value = UiState.Idle
            }
        }
    }

    // Edit a pending card before saving
    fun updatePendingCard(index: Int, front: String, back: String) {
        val cards = _pendingCards.value.toMutableList()
        if (index in cards.indices) {
            cards[index] = cards[index].copy(front = front, back = back)
            _pendingCards.value = cards
        }
    }

    fun removePendingCard(index: Int) {
        val cards = _pendingCards.value.toMutableList()
        if (index in cards.indices) {
            cards.removeAt(index)
            _pendingCards.value = cards
        }
    }

    fun addPendingCard(front: String, back: String, deckId: String) {
        val cards = _pendingCards.value.toMutableList()
        cards.add(Flashcard(deckId = deckId, front = front, back = back))
        _pendingCards.value = cards
    }

    fun resetGenerationState() {
        _generationState.value = UiState.Idle
        _pendingCards.value = emptyList()
    }

    // ─── Study mode ───
    private val _studyCards = MutableStateFlow<List<Flashcard>>(emptyList())
    val studyCards: StateFlow<List<Flashcard>> = _studyCards.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    private val _studyStats = MutableStateFlow(StudyStats())
    val studyStats: StateFlow<StudyStats> = _studyStats.asStateFlow()

    fun startStudySession(deckId: String) {
        viewModelScope.launch {
            val cards = db.cardDao().getCardsForDeckSync(deckId)
            val queue = SpacedRepetitionEngine.getStudyQueue(cards)
            _studyCards.value = queue
            _currentCardIndex.value = 0
            _studyStats.value = StudyStats(total = queue.size)
        }
    }

    fun reviewCurrentCard(quality: ReviewQuality) {
        val cards = _studyCards.value
        val index = _currentCardIndex.value
        if (index !in cards.indices) return

        val card = cards[index]
        val updatedCard = SpacedRepetitionEngine.reviewCard(card, quality)

        viewModelScope.launch {
            db.cardDao().updateCard(updatedCard)
            db.reviewLogDao().insertLog(ReviewLog(cardId = card.id, quality = quality.value))

            // Update stats
            _studyStats.value = _studyStats.value.copy(
                reviewed = _studyStats.value.reviewed + 1,
                againCount = if (quality == ReviewQuality.AGAIN) _studyStats.value.againCount + 1 else _studyStats.value.againCount
            )

            // Move to next card
            if (index + 1 < cards.size) {
                _currentCardIndex.value = index + 1
            }
            // If last card, we're done — UI checks index == size - 1
        }
    }

    fun endStudySession(deckId: String) {
        viewModelScope.launch {
            db.deckDao().updateLastStudied(deckId, System.currentTimeMillis())
            _studyCards.value = emptyList()
            _currentCardIndex.value = 0
        }
    }

    // ─── Deck management ───
    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            db.deckDao().deleteDeck(deck)
        }
    }

    // ─── API key validation ───
    private val _keyValidation = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val keyValidation: StateFlow<UiState<Boolean>> = _keyValidation.asStateFlow()

    fun validateApiKey(key: String) {
        viewModelScope.launch {
            _keyValidation.value = UiState.Loading
            val result = service.validateApiKey(key)
            result.fold(
                onSuccess = { valid -> _keyValidation.value = UiState.Success(valid) },
                onFailure = { error -> _keyValidation.value = UiState.Error(error.message ?: "Validation failed") }
            )
        }
    }

    fun getDueCountForDeck(deckId: String, callback: (Int) -> Unit) {
        viewModelScope.launch {
            val count = db.cardDao().getDueCount(deckId, System.currentTimeMillis())
            callback(count)
        }
    }

    fun getCardCountForDeck(deckId: String, callback: (Int) -> Unit) {
        viewModelScope.launch {
            val count = db.cardDao().getCardCount(deckId)
            callback(count)
        }
    }

    // ─── Cards for deck (flow) ───
    fun getCardsForDeck(deckId: String): Flow<List<Flashcard>> {
        return db.cardDao().getCardsForDeck(deckId)
    }
}

data class StudyStats(
    val total: Int = 0,
    val reviewed: Int = 0,
    val againCount: Int = 0
) {
    val remaining: Int get() = total - reviewed
    val progress: Float get() = if (total > 0) reviewed.toFloat() / total else 0f
}