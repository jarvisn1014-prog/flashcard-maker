package com.nish.flashcards.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Apple-inspired typography scale
// SF Pro style: tight tracking on display, comfortable body
// Body min 17sp (Apple HIG), no smaller body text

val FlashcardTypography = Typography(
    // Display — large numbers, deck titles on hero screens
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.36.sp
    ),

    // Headlines — screen titles
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.35.sp
    ),

    // Body — 17sp minimum per Apple HIG
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Regular,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = -0.43.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Regular,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = -0.24.sp
    ),

    // Labels — button text, metadata
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = -0.24.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = -0.08.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.06.sp
    )
)