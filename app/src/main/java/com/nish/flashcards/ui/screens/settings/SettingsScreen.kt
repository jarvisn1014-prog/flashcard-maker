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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nish.flashcards.FlashcardViewModel
import com.nish.flashcards.UiState
import com.nish.flashcards.data.remote.ProviderConfig
import com.nish.flashcards.ui.components.PillButton
import com.nish.flashcards.ui.components.PillStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FlashcardViewModel,
    onBack: () -> Unit
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val ollamaKey by viewModel.ollamaKey.collectAsState()
    val provider by viewModel.provider.collectAsState()
    val keyValidation by viewModel.keyValidation.collectAsState()
    val uriHandler = LocalUriHandler.current

    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var ollamaInput by remember(ollamaKey) { mutableStateOf(ollamaKey) }
    var selectedProvider by remember(provider) { mutableStateOf(provider) }

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

            // Provider selection
            Column {
                Text(
                    "AI Provider",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose which AI provider to use for generating flashcards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedProvider == ProviderConfig.GEMINI,
                        onClick = {
                            selectedProvider = ProviderConfig.GEMINI
                            viewModel.setProvider(ProviderConfig.GEMINI)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Gemini") }
                    SegmentedButton(
                        selected = selectedProvider == ProviderConfig.OLLAMA,
                        onClick = {
                            selectedProvider = ProviderConfig.OLLAMA
                            viewModel.setProvider(ProviderConfig.OLLAMA)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Ollama Cloud") }
                }
            }

            HorizontalDivider()

            // Gemini API Key (shown when Gemini is selected)
            if (selectedProvider == ProviderConfig.GEMINI) {
                Column {
                    Text(
                        "Gemini API Key",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Get a free key from Google AI Studio. 1500 requests/day on free tier.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("Paste your Gemini API key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") }) {
                        Text("Get free key from Google AI Studio →")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ValidationStatus(keyValidation)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(
                            text = "Validate",
                            onClick = { viewModel.validateApiKey(keyInput, ProviderConfig.GEMINI) },
                            style = PillStyle.Outlined,
                            enabled = keyInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        )
                        PillButton(
                            text = "Save",
                            onClick = { viewModel.saveApiKey(keyInput); onBack() },
                            enabled = keyInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Ollama Cloud Key (shown when Ollama is selected)
            if (selectedProvider == ProviderConfig.OLLAMA) {
                Column {
                    Text(
                        "Ollama Cloud API Key",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Uses GLM-5.2 for structured generation. Your key is stored locally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = ollamaInput,
                        onValueChange = { ollamaInput = it },
                        placeholder = { Text("Paste your Ollama Cloud API key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { uriHandler.openUri("https://ollama.com") }) {
                        Text("Get your Ollama Cloud key →")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ValidationStatus(keyValidation)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(
                            text = "Validate",
                            onClick = { viewModel.validateApiKey(ollamaInput, ProviderConfig.OLLAMA) },
                            style = PillStyle.Outlined,
                            enabled = ollamaInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        )
                        PillButton(
                            text = "Save",
                            onClick = { viewModel.saveOllamaKey(ollamaInput); onBack() },
                            enabled = ollamaInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider()

            // About
            Column {
                Text("About", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Flashcard Maker v1.0", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("AI-powered flashcards with spaced repetition. Built with Kotlin + Jetpack Compose.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ValidationStatus(keyValidation: UiState<Boolean>) {
    when (val v = keyValidation) {
        is UiState.Loading -> Text("Validating...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        is UiState.Success -> {
            if (v.data) Text("✅ API key is valid!", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            else Text("❌ API key is invalid. Please check and try again.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Error -> Text("❌ ${v.message}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
        else -> {}
    }
}