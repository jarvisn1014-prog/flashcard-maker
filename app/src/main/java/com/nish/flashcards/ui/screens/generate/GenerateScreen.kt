package com.nish.flashcards.ui.screens.generate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nish.flashcards.FlashcardViewModel
import com.nish.flashcards.UiState
import com.nish.flashcards.ui.components.PillButton

// PM Insight: This is the REVIEW screen — after AI generates cards,
// the user can edit, add, remove, or approve before saving.
// This is a critical trust-building step: users see WHAT the AI made
// and have CONTROL over it. AI suggests, human decides.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    viewModel: FlashcardViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val state by viewModel.generationState.collectAsState()
    val pendingCards by viewModel.pendingCards.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Cards") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetGenerationState()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating flashcards with AI...", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("This usually takes 5-10 seconds", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("❌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generation Failed", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        PillButton(text = "Go Back", onClick = onBack, style = com.nish.flashcards.ui.components.PillStyle.Outlined)
                    }
                }

                is UiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "${pendingCards.size} cards generated. Review and edit before saving.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(pendingCards) { index, card ->
                                CardEditorItem(
                                    index = index,
                                    front = card.front,
                                    back = card.back,
                                    onFrontChange = { viewModel.updatePendingCard(index, it, card.back) },
                                    onBackChange = { viewModel.updatePendingCard(index, card.front, it) },
                                    onDelete = { viewModel.removePendingCard(index) }
                                )
                            }
                        }

                        // Save bar
                        Surface(color = MaterialTheme.colorScheme.surface) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${pendingCards.size} cards", style = MaterialTheme.typography.labelMedium)
                                PillButton(
                                    text = "Save Deck",
                                    onClick = onDone,
                                    enabled = pendingCards.isNotEmpty()
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Idle — shouldn't happen, but handle gracefully
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No active generation", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        PillButton(text = "Back to Decks", onClick = onBack, style = com.nish.flashcards.ui.components.PillStyle.Outlined)
                    }
                }
            }
        }
    }
}

@Composable
private fun CardEditorItem(
    index: Int,
    front: String,
    back: String,
    onFrontChange: (String) -> Unit,
    onBackChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Card ${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = front,
                onValueChange = onFrontChange,
                label = { Text("Question") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = back,
                onValueChange = onBackChange,
                label = { Text("Answer") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}