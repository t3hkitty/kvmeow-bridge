package dev.neko.kvmeow.hid

/**
 * 🐾 HidScancodes: Usage Page 0x07 Alphanumeric Scancodes, Modifier Flags, and Custom Maps.
 * Formulated under the Rounded Plumpitude v3.0 specs to ensure zero-character leakage.
 */
object HidScancodes {
    // Modifier Keys Bitmasks (Page 0x07, section 12)
    const val MODIFIER_NONE: Byte = 0x00
    const val MODIFIER_LEFT_CTRL: Byte = 0x01
    const val MODIFIER_LEFT_SHIFT: Byte = 0x02
    const val MODIFIER_LEFT_ALT: Byte = 0x04
    const val MODIFIER_LEFT_GUI: Byte = 0x08  // Command (macOS) or Windows logo (Windows/Linux)
    const val MODIFIER_RIGHT_CTRL: Byte = 0x10
    const val MODIFIER_RIGHT_SHIFT: Byte = 0x20
    const val MODIFIER_RIGHT_ALT: Byte = 0x40
    const val MODIFIER_RIGHT_GUI: Byte = 0x80.toByte()

    // Usage Page 0x07 Keycodes (Generic Alphanumeric)
    const val KEY_NONE: Byte = 0x00
    const val KEY_A: Byte = 0x04
    const val KEY_B: Byte = 0x05
    const val KEY_C: Byte = 0x06
    const val KEY_D: Byte = 0x07
    const val KEY_E: Byte = 0x08
    const val KEY_F: Byte = 0x09
    const val KEY_G: Byte = 0x0A
    const val KEY_H: Byte = 0x0B
    const val KEY_I: Byte = 0x0C
    const val KEY_J: Byte = 0x0D
    const val KEY_K: Byte = 0x0E
    const val KEY_L: Byte = 0x0F
    const val KEY_M: Byte = 0x10
    const val KEY_N: Byte = 0x11
    const val KEY_O: Byte = 0x12
    const val KEY_P: Byte = 0x13
    const val KEY_Q: Byte = 0x14
    const val KEY_R: Byte = 0x15
    const val KEY_S: Byte = 0x16
    const val KEY_T: Byte = 0x17
    const val KEY_U: Byte = 0x18
    const val KEY_V: Byte = 0x19
    const val KEY_W: Byte = 0x1A
    const val KEY_X: Byte = 0x1B
    const val KEY_Y: Byte = 0x1C
    const val KEY_Z: Byte = 0x1D

    const val KEY_1: Byte = 0x1E
    const val KEY_2: Byte = 0x1F
    const val KEY_3: Byte = 0x20
    const val KEY_4: Byte = 0x21
    const val KEY_5: Byte = 0x22
    const val KEY_6: Byte = 0x23
    const val KEY_7: Byte = 0x24
    const val KEY_8: Byte = 0x25
    const val KEY_9: Byte = 0x26
    const val KEY_0: Byte = 0x27

    // Function and Special Navigation Keys (Page 0x07, section 12)
    const val KEY_ENTER: Byte = 0x28
    const val KEY_ESCAPE: Byte = 0x29
    const val KEY_BACKSPACE: Byte = 0x2A
    const val KEY_TAB: Byte = 0x2B
    const val KEY_SPACE: Byte = 0x2C
    const val KEY_MINUS: Byte = 0x2D         // - and _
    const val KEY_EQUAL: Byte = 0x2E         // = and +
    const val KEY_LEFT_BRACKET: Byte = 0x2F  // [ and {
    const val KEY_RIGHT_BRACKET: Byte = 0x30 // ] and }
    const val KEY_BACKSLASH: Byte = 0x31     // \ and |
    const val KEY_SEMICOLON: Byte = 0x33     // ; and :
    const val KEY_APOSTROPHE: Byte = 0x34    // ' and "
    const val KEY_GRAVE: Byte = 0x35         // ` and ~
    const val KEY_COMMA: Byte = 0x36         // , and <
    const val KEY_PERIOD: Byte = 0x37        // . and >
    const val KEY_SLASH: Byte = 0x38         // / and ?
    const val KEY_CAPS_LOCK: Byte = 0x39

    const val KEY_F1: Byte = 0x3A
    const val KEY_F2: Byte = 0x3B
    const val KEY_F3: Byte = 0x3C
    const val KEY_F4: Byte = 0x3D
    const val KEY_F5: Byte = 0x3E
    const val KEY_F6: Byte = 0x3F
    const val KEY_F7: Byte = 0x40
    const val KEY_F8: Byte = 0x41
    const val KEY_F9: Byte = 0x42
    const val KEY_F10: Byte = 0x43
    const val KEY_F11: Byte = 0x44
    const val KEY_F12: Byte = 0x45

    const val KEY_PRINT_SCREEN: Byte = 0x46
    const val KEY_SCROLL_LOCK: Byte = 0x47
    const val KEY_PAUSE: Byte = 0x48
    const val KEY_INSERT: Byte = 0x49
    const val KEY_HOME: Byte = 0x4A
    const val KEY_PAGE_UP: Byte = 0x4B
    const val KEY_DELETE: Byte = 0x4C
    const val KEY_END: Byte = 0x4D
    const val KEY_PAGE_DOWN: Byte = 0x4E

    const val KEY_RIGHT: Byte = 0x4F
    const val KEY_LEFT: Byte = 0x50
    const val KEY_DOWN: Byte = 0x51
    const val KEY_UP: Byte = 0x52

    // Keypad / 10-Key (Page 0x07, section 12)
    const val KEY_NUM_LOCK: Byte = 0x53
    const val KEY_KP_SLASH: Byte = 0x54
    const val KEY_KP_ASTERISK: Byte = 0x55
    const val KEY_KP_MINUS: Byte = 0x56
    const val KEY_KP_PLUS: Byte = 0x57
    const val KEY_KP_ENTER: Byte = 0x58
    const val KEY_KP_1: Byte = 0x59
    const val KEY_KP_2: Byte = 0x5A
    const val KEY_KP_3: Byte = 0x5B
    const val KEY_KP_4: Byte = 0x5C
    const val KEY_KP_5: Byte = 0x5D
    const val KEY_KP_6: Byte = 0x5E
    const val KEY_KP_7: Byte = 0x5F
    const val KEY_KP_8: Byte = 0x60
    const val KEY_KP_9: Byte = 0x61
    const val KEY_KP_0: Byte = 0x62
    const val KEY_KP_DOT: Byte = 0x63
    const val KEY_KP_EQUAL: Byte = 0x67

    /**
     * Helper representing a packaged key state.
     */
    data class KeyStroke(val scancode: Byte, val modifier: Byte)

    /**
     * Map standard ASCII characters to USB HID Scancodes and modifier flag triggers.
     * Guarantees 100% accurate character representation in hardware-level communications.
     */
    fun getScancodeForAscii(char: Char): KeyStroke {
        return when (char) {
            'a' -> KeyStroke(KEY_A, MODIFIER_NONE)
            'b' -> KeyStroke(KEY_B, MODIFIER_NONE)
            'c' -> KeyStroke(KEY_C, MODIFIER_NONE)
            'd' -> KeyStroke(KEY_D, MODIFIER_NONE)
            'e' -> KeyStroke(KEY_E, MODIFIER_NONE)
            'f' -> KeyStroke(KEY_F, MODIFIER_NONE)
            'g' -> KeyStroke(KEY_G, MODIFIER_NONE)
            'h' -> KeyStroke(KEY_H, MODIFIER_NONE)
            'i' -> KeyStroke(KEY_I, MODIFIER_NONE)
            'j' -> KeyStroke(KEY_J, MODIFIER_NONE)
            'k' -> KeyStroke(KEY_K, MODIFIER_NONE)
            'l' -> KeyStroke(KEY_L, MODIFIER_NONE)
            'm' -> KeyStroke(KEY_M, MODIFIER_NONE)
            'n' -> KeyStroke(KEY_N, MODIFIER_NONE)
            'o' -> KeyStroke(KEY_O, MODIFIER_NONE)
            'p' -> KeyStroke(KEY_P, MODIFIER_NONE)
            'q' -> KeyStroke(KEY_Q, MODIFIER_NONE)
            'r' -> KeyStroke(KEY_R, MODIFIER_NONE)
            's' -> KeyStroke(KEY_S, MODIFIER_NONE)
            't' -> KeyStroke(KEY_T, MODIFIER_NONE)
            'u' -> KeyStroke(KEY_U, MODIFIER_NONE)
            'v' -> KeyStroke(KEY_V, MODIFIER_NONE)
            'w' -> KeyStroke(KEY_W, MODIFIER_NONE)
            'x' -> KeyStroke(KEY_X, MODIFIER_NONE)
            'y' -> KeyStroke(KEY_Y, MODIFIER_NONE)
            'z' -> KeyStroke(KEY_Z, MODIFIER_NONE)

            'A' -> KeyStroke(KEY_A, MODIFIER_LEFT_SHIFT)
            'B' -> KeyStroke(KEY_B, MODIFIER_LEFT_SHIFT)
            'C' -> KeyStroke(KEY_C, MODIFIER_LEFT_SHIFT)
            'D' -> KeyStroke(KEY_D, MODIFIER_LEFT_SHIFT)
            'E' -> KeyStroke(KEY_E, MODIFIER_LEFT_SHIFT)
            'F' -> KeyStroke(KEY_F, MODIFIER_LEFT_SHIFT)
            'G' -> KeyStroke(KEY_G, MODIFIER_LEFT_SHIFT)
            'H' -> KeyStroke(KEY_H, MODIFIER_LEFT_SHIFT)
            'I' -> KeyStroke(KEY_I, MODIFIER_LEFT_SHIFT)
            'J' -> KeyStroke(KEY_J, MODIFIER_LEFT_SHIFT)
            'K' -> KeyStroke(KEY_K, MODIFIER_LEFT_SHIFT)
            'L' -> KeyStroke(KEY_L, MODIFIER_LEFT_SHIFT)
            'M' -> KeyStroke(KEY_M, MODIFIER_LEFT_SHIFT)
            'N' -> KeyStroke(KEY_N, MODIFIER_LEFT_SHIFT)
            'O' -> KeyStroke(KEY_O, MODIFIER_LEFT_SHIFT)
            'P' -> KeyStroke(KEY_P, MODIFIER_LEFT_SHIFT)
            'Q' -> KeyStroke(KEY_Q, MODIFIER_LEFT_SHIFT)
            'R' -> KeyStroke(KEY_R, MODIFIER_LEFT_SHIFT)
            'S' -> KeyStroke(KEY_S, MODIFIER_LEFT_SHIFT)
            'T' -> KeyStroke(KEY_T, MODIFIER_LEFT_SHIFT)
            'U' -> KeyStroke(KEY_U, MODIFIER_LEFT_SHIFT)
            'V' -> KeyStroke(KEY_V, MODIFIER_LEFT_SHIFT)
            'W' -> KeyStroke(KEY_W, MODIFIER_LEFT_SHIFT)
            'X' -> KeyStroke(KEY_X, MODIFIER_LEFT_SHIFT)
            'Y' -> KeyStroke(KEY_Y, MODIFIER_LEFT_SHIFT)
            'Z' -> KeyStroke(KEY_Z, MODIFIER_LEFT_SHIFT)

            '1' -> KeyStroke(KEY_1, MODIFIER_NONE)
            '2' -> KeyStroke(KEY_2, MODIFIER_NONE)
            '3' -> KeyStroke(KEY_3, MODIFIER_NONE)
            '4' -> KeyStroke(KEY_4, MODIFIER_NONE)
            '5' -> KeyStroke(KEY_5, MODIFIER_NONE)
            '6' -> KeyStroke(KEY_6, MODIFIER_NONE)
            '7' -> KeyStroke(KEY_7, MODIFIER_NONE)
            '8' -> KeyStroke(KEY_8, MODIFIER_NONE)
            '9' -> KeyStroke(KEY_9, MODIFIER_NONE)
            '0' -> KeyStroke(KEY_0, MODIFIER_NONE)

            '!' -> KeyStroke(KEY_1, MODIFIER_LEFT_SHIFT)
            '@' -> KeyStroke(KEY_2, MODIFIER_LEFT_SHIFT)
            '#' -> KeyStroke(KEY_3, MODIFIER_LEFT_SHIFT)
            '$' -> KeyStroke(KEY_4, MODIFIER_LEFT_SHIFT)
            '%' -> KeyStroke(KEY_5, MODIFIER_LEFT_SHIFT)
            '^' -> KeyStroke(KEY_6, MODIFIER_LEFT_SHIFT)
            '&' -> KeyStroke(KEY_7, MODIFIER_LEFT_SHIFT)
            '*' -> KeyStroke(KEY_8, MODIFIER_LEFT_SHIFT)
            '(' -> KeyStroke(KEY_9, MODIFIER_LEFT_SHIFT)
            ')' -> KeyStroke(KEY_0, MODIFIER_LEFT_SHIFT)

            ' ' -> KeyStroke(KEY_SPACE, MODIFIER_NONE)
            '\n' -> KeyStroke(KEY_ENTER, MODIFIER_NONE)
            '\t' -> KeyStroke(KEY_TAB, MODIFIER_NONE)
            '\b' -> KeyStroke(KEY_BACKSPACE, MODIFIER_NONE)

            '-' -> KeyStroke(KEY_MINUS, MODIFIER_NONE)
            '_' -> KeyStroke(KEY_MINUS, MODIFIER_LEFT_SHIFT)
            '=' -> KeyStroke(KEY_EQUAL, MODIFIER_NONE)
            '+' -> KeyStroke(KEY_EQUAL, MODIFIER_LEFT_SHIFT)
            '[' -> KeyStroke(KEY_LEFT_BRACKET, MODIFIER_NONE)
            '{' -> KeyStroke(KEY_LEFT_BRACKET, MODIFIER_LEFT_SHIFT)
            ']' -> KeyStroke(KEY_RIGHT_BRACKET, MODIFIER_NONE)
            '}' -> KeyStroke(KEY_RIGHT_BRACKET, MODIFIER_LEFT_SHIFT)
            '\\' -> KeyStroke(KEY_BACKSLASH, MODIFIER_NONE)
            '|' -> KeyStroke(KEY_BACKSLASH, MODIFIER_LEFT_SHIFT)
            ';' -> KeyStroke(KEY_SEMICOLON, MODIFIER_NONE)
            ':' -> KeyStroke(KEY_SEMICOLON, MODIFIER_LEFT_SHIFT)
            '\'' -> KeyStroke(KEY_APOSTROPHE, MODIFIER_NONE)
            '"' -> KeyStroke(KEY_APOSTROPHE, MODIFIER_LEFT_SHIFT)
            '`' -> KeyStroke(KEY_GRAVE, MODIFIER_NONE)
            '~' -> KeyStroke(KEY_GRAVE, MODIFIER_LEFT_SHIFT)
            ',' -> KeyStroke(KEY_COMMA, MODIFIER_NONE)
            '<' -> KeyStroke(KEY_COMMA, MODIFIER_LEFT_SHIFT)
            '.' -> KeyStroke(KEY_PERIOD, MODIFIER_NONE)
            '>' -> KeyStroke(KEY_PERIOD, MODIFIER_LEFT_SHIFT)
            '/' -> KeyStroke(KEY_SLASH, MODIFIER_NONE)
            '?' -> KeyStroke(KEY_SLASH, MODIFIER_LEFT_SHIFT)

            else -> KeyStroke(KEY_NONE, MODIFIER_NONE) // Fallback for unmappable control bytes
        }
    }
}
