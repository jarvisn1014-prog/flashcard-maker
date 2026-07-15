package com.nish.flashcards.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nish.flashcards.FlashcardViewModel
import com.nish.flashcards.UiState
import com.nish.flashcards.ui.components.PillButton
import com.nish.flashcards.ui.components.PillStyle

// PM Insight: The Settings screen is where BYOK comes to life.
// The API key input is not just a form field — it's the gateway to the product.
// We provide a direct link to get a free key, validation feedback,
// and clear instructions. This IS the onboarding flow.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FlashcardViewModel,
    onBack: () -> Unit
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val keyValidation by viewModel.keyValidation.collectAsState()
    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // API Key section
            Column {
                Text(
                    "API Key",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "This app uses your own Google Gemini API key. Your key is stored locally on your device and never sent to any server except Google's.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = { Text("Paste your Gemini API key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Get free key link
                TextButton(onClick = {
                    uriHandler.openUri("https://aistudio.google.com/app/apikey")
                }) {
                    Text("Get your free API key from Google AI Studio →")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Validation status
                when (val v = keyValidation) {
                    is UiState.Loading -> {
                        Text("Validating...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is UiState.Success -> {
                        if (v.data) {
                            Text("✅ API key is valid!", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("❌ API key is invalid. Please check and try again.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is UiState.Error -> {
                        Text("❌ ${v.message}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PillButton(
                        text = "Validate",
                        onClick = { viewModel.validateApiKey(keyInput) },
                        style = PillStyle.Outlined,
                        enabled = keyInput.isNotBlank()
                    )
                    PillButton(
                        text = "Save",
                        onClick = {
                            viewModel.saveApiKey(keyInput)
                            onBack()
                        },
                        enabled = keyInput.isNotBlank()
                    )
                }
            }

            HorizontalDivider()

            // About section
            Column {
                Text(
                    "About",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Flashcard Maker v1.0", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("AI-powered flashcards with spaced repetition. Built with Kotlin + Jetpack Compose.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Powered by Google Gemini API. All data stored locally on your device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}