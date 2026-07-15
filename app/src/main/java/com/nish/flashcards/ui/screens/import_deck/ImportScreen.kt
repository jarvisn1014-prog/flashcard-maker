package com.nish.flashcards.ui.screens.import_deck

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.nish.flashcards.ui.components.PillButton

// PM Insight: This is the INPUT screen — the first step in the core loop.
// The easier this is, the more likely users complete the flow.
// We support two input methods: paste text, or (future) upload PDF.
// For MVP, text paste is the fastest path to value.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: FlashcardViewModel,
    onBack: () -> Unit,
    onGenerate: (deckName: String, sourceText: String) -> Unit
) {
    var deckName by remember { mutableStateOf("") }
    var sourceText by remember { mutableStateOf("") }
    val apiKey by viewModel.apiKey.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Deck") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Deck name
            Column {
                Text(
                    "Deck Name",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deckName,
                    onValueChange = { deckName = it },
                    placeholder = { Text("e.g., Biology Chapter 5") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Source text
            Column {
                Text(
                    "Study Material",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Paste your notes, textbook content, or any text you want to turn into flashcards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    placeholder = { Text("Paste your study material here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // API key warning
            if (apiKey.isBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                ) {
                    Text(
                        "⚠️ You need an API key to generate flashcards. Go to Settings to add your free Gemini API key.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Generate button
            PillButton(
                text = "Generate Flashcards",
                onClick = {
                    if (deckName.isNotBlank() && sourceText.isNotBlank()) {
                        onGenerate(deckName, sourceText)
                    }
                },
                enabled = deckName.isNotBlank() && sourceText.isNotBlank() && apiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}