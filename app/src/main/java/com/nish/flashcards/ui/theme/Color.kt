package com.nish.flashcards.ui.theme

import androidx.compose.ui.graphics.Color

// Apple-inspired minimalist palette
// Single accent (indigo), clean neutrals, no gradients

// Light theme
val LightBackground = Color(0xFFF2F2F7)      // iOS systemGroupedBackground
val LightSurface = Color(0xFFFFFFFF)         // Card surfaces
val LightSurfaceVariant = Color(0xFFE5E5EA)  // Secondary backgrounds
val LightOnSurface = Color(0xFF1C1C1E)      // Primary text
val LightOnSurfaceVariant = Color(0xFF6B6B70) // Secondary text
val LightHairline = Color(0xFFC6C6C8)        // Dividers

// Dark theme
val DarkBackground = Color(0xFF000000)       // Pure black (Apple OLED style)
val DarkSurface = Color(0xFF1C1C1E)          // Card surfaces
val DarkSurfaceVariant = Color(0xFF2C2C2E)   // Secondary backgrounds
val DarkOnSurface = Color(0xFFFFFFFF)        // Primary text
val DarkOnSurfaceVariant = Color(0xFF8E8E93) // Secondary text
val DarkHairline = Color(0xFF38383A)         // Dividers

// Single accent — Indigo (Apple-style)
val Accent = Color(0xFF5E5CE6)               // Apple systemIndigo
val AccentVariant = Color(0xFF7D7AFF)        // Lighter indigo for dark mode

// Semantic
val Success = Color(0xFF34C759)              // Apple green
val Warning = Color(0xFFFF9500)              // Apple orange
val Error = Color(0xFFFF3B30)                // Apple red