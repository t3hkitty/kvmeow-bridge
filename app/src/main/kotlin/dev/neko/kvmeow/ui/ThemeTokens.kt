package dev.neko.kvmeow.ui

import androidx.compose.ui.graphics.Color

/**
 * 🎨 Three-Theme Engine Tokens
 * 1. TRON: Classic appground-io obsidian / electric cyan / neon amber grid styling.
 * 2. DARK_KAWAII: Deep indigo / plum / soft lavender / kawaii pink.
 * 3. BRIGHT_KAWAII: Crisp airy lavender / pastel mint / bubblegum pink / bright gold.
 */
enum class AppTheme {
    TRON,
    DARK_KAWAII,
    BRIGHT_KAWAII
}

data class ThemePalette(
    val background: Color,
    val surface: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val buttonBackground: Color,
    val buttonSelected: Color,
    val statusActive: Color
)

object ThemeManager {
    val TronPalette = ThemePalette(
        background = Color(0xFF0A0E14),
        surface = Color(0xFF101721),
        cardBackground = Color(0xFF141E2B),
        cardBorder = Color(0xFF00E5FF).copy(alpha = 0.6f),
        primaryText = Color(0xFFFFB300),      // Neon Amber
        secondaryText = Color(0xFF80DEEA),    // Cyan
        accentPrimary = Color(0xFF00E5FF),    // Electric Cyan
        accentSecondary = Color(0xFFFF80AB),
        buttonBackground = Color(0xFF1E293B),
        buttonSelected = Color(0xFF00ACC1),
        statusActive = Color(0xFF00E676)
    )

    val DarkKawaiiPalette = ThemePalette(
        background = Color(0xFF141022),
        surface = Color(0xFF1E1833),
        cardBackground = Color(0xFF1E1833),   // Deep indigo/plum surface
        cardBorder = Color(0xFF3B2859),
        primaryText = Color(0xFFF8F2FA),      // Crisp text
        secondaryText = Color(0xFFC084FC),    // Lavender accent
        accentPrimary = Color(0xFFA855F7),    // Vibrant purple
        accentSecondary = Color(0xFFFFAFCC),  // Kawaii pink
        buttonBackground = Color(0xFF2C2248),
        buttonSelected = Color(0xFFA855F7),
        statusActive = Color(0xFF4ADE80)
    )

    val BrightKawaiiPalette = ThemePalette(
        background = Color(0xFFFFF5FA),
        surface = Color(0xFFF8EEF5),
        cardBackground = Color(0xFFF8EEF5),   // Cream/blush surface
        cardBorder = Color(0xFFE2C4DC),       // Crisp border
        primaryText = Color(0xFF181524),      // High-contrast dark ink
        secondaryText = Color(0xFF4A3E65),    // Indigo/purple secondary
        accentPrimary = Color(0xFF9333EA),    // Vibrant violet
        accentSecondary = Color(0xFFFF66C4),  // Bubblegum
        buttonBackground = Color(0xFFEDE0EC),
        buttonSelected = Color(0xFFC084FC),
        statusActive = Color(0xFF059669)
    )

    fun getPalette(theme: AppTheme): ThemePalette {
        return when (theme) {
            AppTheme.TRON -> TronPalette
            AppTheme.DARK_KAWAII -> DarkKawaiiPalette
            AppTheme.BRIGHT_KAWAII -> BrightKawaiiPalette
        }
    }
}
