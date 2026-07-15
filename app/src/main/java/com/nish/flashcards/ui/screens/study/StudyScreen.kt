package com.nish.flashcards.ui.screens.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nish.flashcards.FlashcardViewModel
import com.nish.flashcards.engine.ReviewQuality
import com.nish.flashcards.ui.components.PillButton
import com.nish.flashcards.ui.components.PillStyle

// PM Insight: This is the CORE VALUE screen — where learning happens.
// The flip animation is satisfying (engagement), the 4-button review
// system feeds the SM-2 algorithm (science), and the progress bar
// gives a sense of completion (motivation).
// Every element here serves the core loop: see → recall → rate → repeat.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    viewModel: FlashcardViewModel,
    deckId: String,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val cards by viewModel.studyCards.collectAsState()
    val currentIndex by viewModel.currentCardIndex.collectAsState()
    val stats by viewModel.studyStats.collectAsState()
    var isFlipped by remember { mutableStateOf(false) }

    LaunchedEffect(deckId) {
        viewModel.startStudySession(deckId)
    }

    // Reset flip when card changes
    LaunchedEffect(currentIndex) {
        isFlipped = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.endStudySession(deckId)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (cards.isEmpty()) {
            // Loading or no cards
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (currentIndex >= cards.size) {
            // Study complete
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("✅", fontSize = 56.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Session Complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Reviewed: ${stats.reviewed}", style = MaterialTheme.typography.bodyLarge)
                Text("Need review: ${stats.againCount}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(32.dp))
                PillButton(text = "Done", onClick = {
                    viewModel.endStudySession(deckId)
                    onFinish()
                })
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Progress bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Card ${currentIndex + 1} of ${cards.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${stats.remaining} remaining", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { stats.progress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Flashcard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { isFlipped = !isFlipped },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (!isFlipped) {
                                Text(
                                    text = cards[currentIndex].front,
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Tap to reveal answer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = cards[currentIndex].back,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Review buttons (only show after flip)
                if (isFlipped) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReviewButton("Again", ReviewQuality.AGAIN, MaterialTheme.colorScheme.error, viewModel, modifier = Modifier.weight(1f))
                        ReviewButton("Hard", ReviewQuality.HARD, MaterialTheme.colorScheme.secondary, viewModel, modifier = Modifier.weight(1f))
                        ReviewButton("Good", ReviewQuality.GOOD, MaterialTheme.colorScheme.primary, viewModel, modifier = Modifier.weight(1f))
                        ReviewButton("Easy", ReviewQuality.EASY, MaterialTheme.colorScheme.tertiary, viewModel, modifier = Modifier.weight(1f))
                    }
                } else {
                    Text(
                        "How well did you know this?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewButton(
    text: String,
    quality: ReviewQuality,
    color: androidx.compose.ui.graphics.Color,
    viewModel: FlashcardViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { viewModel.reviewCurrentCard(quality) },
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()
        )
    }
}