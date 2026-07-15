package com.nish.flashcards.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nish.flashcards.FlashcardViewModel
import com.nish.flashcards.ui.screens.deck_list.DeckListScreen
import com.nish.flashcards.ui.screens.import_deck.ImportScreen
import com.nish.flashcards.ui.screens.generate.GenerateScreen
import com.nish.flashcards.ui.screens.study.StudyScreen
import com.nish.flashcards.ui.screens.settings.SettingsScreen

// PM Insight: Navigation defines the user journey.
// 4 screens, linear flow: Deck List → Import → Generate → Study
// Settings is accessible from Deck List
// This IS the user flow — each screen is a step in the core loop.

@Composable
fun FlashcardNavigation() {
    val navController = rememberNavController()
    val viewModel: FlashcardViewModel = viewModel()

    NavHost(navController = navController, startDestination = "decks") {

        composable("decks") {
            DeckListScreen(
                viewModel = viewModel,
                onImportClick = { navController.navigate("import") },
                onStudyClick = { deckId -> navController.navigate("study/$deckId") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable("import") {
            ImportScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onGenerate = { deckName, sourceText ->
                    viewModel.generateFlashcards(deckName, sourceText)
                    navController.navigate("generate")
                }
            )
        }

        composable("generate") {
            GenerateScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDone = {
                    viewModel.resetGenerationState()
                    navController.navigate("decks") {
                        popUpTo("decks") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "study/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId") ?: ""
            StudyScreen(
                viewModel = viewModel,
                deckId = deckId,
                onBack = { navController.popBackStack() },
                onFinish = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}