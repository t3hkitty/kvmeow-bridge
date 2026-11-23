package net.artkitty.kvmeowboard.ui

import androidx.compose.ui.graphics.Color

enum class DeckThemeMode {
    TRON,
    DARK_KAWAII,
    BRIGHT_KAWAII
}

data class DeckPalette(
    val bg: Color,
    val surface: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val buttonBg: Color,
    val buttonSelected: Color,
    val statusActive: Color
)

object KvMeowTheme {
    val Tron = DeckPalette(
        bg = Color(0xFF070B14),
        surface = Color(0xFF0D1424),
        cardBg = Color(0xFF111A2E),
        cardBorder = Color(0xFF00F0FF).copy(alpha = 0.6f),
        textPrimary = Color(0xFFE6F7FF),
        textSecondary = Color(0xFF80DEEA),
        accentPrimary = Color(0xFF00F0FF),  // Cyan
        accentSecondary = Color(0xFFFF9E00), // Amber
        buttonBg = Color(0xFF172338),
        buttonSelected = Color(0xFF00ACC1),
        statusActive = Color(0xFF00E676)
    )

    val DarkKawaii = DeckPalette(
        bg = Color(0xFF141022),
        surface = Color(0xFF1E1833),
        cardBg = Color(0xFF1E1833),
        cardBorder = Color(0xFF3B2859),
        textPrimary = Color(0xFFF8F2FA),
        textSecondary = Color(0xFFC084FC),
        accentPrimary = Color(0xFFA855F7),
        accentSecondary = Color(0xFFF472B6),
        buttonBg = Color(0xFF2C2248),
        buttonSelected = Color(0xFFA855F7),
        statusActive = Color(0xFF4ADE80)
    )

    val BrightKawaii = DeckPalette(
        bg = Color(0xFFFFF5FA),
        surface = Color(0xFFF8EEF5),
        cardBg = Color(0xFFF8EEF5),
        cardBorder = Color(0xFFE2C4DC),
        textPrimary = Color(0xFF181524),      // High-contrast ink
        textSecondary = Color(0xFF4A3E65),
        accentPrimary = Color(0xFF6366F1),    // Indigo
        accentSecondary = Color(0xFFEC4899),  // Bubblegum
        buttonBg = Color(0xFFEDE0EC),
        buttonSelected = Color(0xFF7C3AED),
        statusActive = Color(0xFF059669)
    )

    fun getPalette(mode: DeckThemeMode): DeckPalette = when (mode) {
        DeckThemeMode.TRON -> Tron
        DeckThemeMode.DARK_KAWAII -> DarkKawaii
        DeckThemeMode.BRIGHT_KAWAII -> BrightKawaii
    }
}
