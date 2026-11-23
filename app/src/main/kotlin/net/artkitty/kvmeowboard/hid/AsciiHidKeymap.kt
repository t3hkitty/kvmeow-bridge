package net.artkitty.kvmeowboard.hid

/**
 * 🔤 AsciiHidKeymap 🔤
 * Translates standard ASCII printable characters and control keys into standard USB HID Usage Page 0x07 scancodes.
 * Supports typing complete strings over Report ID 1 (Keyboard).
 */
object AsciiHidKeymap {

    const val MODIFIER_NONE: Byte = 0x00
    const val MODIFIER_SHIFT: Byte = 0x02

    val SCANCODE_ENTER: Byte = 0x28
    val SCANCODE_ESCAPE: Byte = 0x29
    val SCANCODE_BACKSPACE: Byte = 0x2A
    val SCANCODE_TAB: Byte = 0x2B
    val SCANCODE_SPACE: Byte = 0x2C

    val SCANCODE_RIGHT: Byte = 0x4F
    val SCANCODE_LEFT: Byte = 0x50
    val SCANCODE_DOWN: Byte = 0x51
    val SCANCODE_UP: Byte = 0x52

    data class KeyEntry(val modifier: Byte, val scancode: Byte)

    fun getEntryForChar(c: Char): KeyEntry? {
        return when (c) {
            in 'a'..'z' -> KeyEntry(MODIFIER_NONE, (0x04 + (c - 'a')).toByte())
            in 'A'..'Z' -> KeyEntry(MODIFIER_SHIFT, (0x04 + (c - 'A')).toByte())
            '1' -> KeyEntry(MODIFIER_NONE, 0x1E)
            '2' -> KeyEntry(MODIFIER_NONE, 0x1F)
            '3' -> KeyEntry(MODIFIER_NONE, 0x20)
            '4' -> KeyEntry(MODIFIER_NONE, 0x21)
            '5' -> KeyEntry(MODIFIER_NONE, 0x22)
            '6' -> KeyEntry(MODIFIER_NONE, 0x23)
            '7' -> KeyEntry(MODIFIER_NONE, 0x24)
            '8' -> KeyEntry(MODIFIER_NONE, 0x25)
            '9' -> KeyEntry(MODIFIER_NONE, 0x26)
            '0' -> KeyEntry(MODIFIER_NONE, 0x27)
            ' ' -> KeyEntry(MODIFIER_NONE, SCANCODE_SPACE)
            '\n' -> KeyEntry(MODIFIER_NONE, SCANCODE_ENTER)
            '\t' -> KeyEntry(MODIFIER_NONE, SCANCODE_TAB)
            '.' -> KeyEntry(MODIFIER_NONE, 0x37)
            ',' -> KeyEntry(MODIFIER_NONE, 0x36)
            '/' -> KeyEntry(MODIFIER_NONE, 0x38)
            '-' -> KeyEntry(MODIFIER_NONE, 0x2D)
            '=' -> KeyEntry(MODIFIER_NONE, 0x2E)
            ';' -> KeyEntry(MODIFIER_NONE, 0x33)
            '\'' -> KeyEntry(MODIFIER_NONE, 0x34)
            '[' -> KeyEntry(MODIFIER_NONE, 0x2F)
            ']' -> KeyEntry(MODIFIER_NONE, 0x30)
            '\\' -> KeyEntry(MODIFIER_NONE, 0x31)
            '`' -> KeyEntry(MODIFIER_NONE, 0x35)
            // Shifted symbols
            '!' -> KeyEntry(MODIFIER_SHIFT, 0x1E)
            '@' -> KeyEntry(MODIFIER_SHIFT, 0x1F)
            '#' -> KeyEntry(MODIFIER_SHIFT, 0x20)
            '$' -> KeyEntry(MODIFIER_SHIFT, 0x21)
            '%' -> KeyEntry(MODIFIER_SHIFT, 0x22)
            '^' -> KeyEntry(MODIFIER_SHIFT, 0x23)
            '&' -> KeyEntry(MODIFIER_SHIFT, 0x24)
            '*' -> KeyEntry(MODIFIER_SHIFT, 0x25)
            '(' -> KeyEntry(MODIFIER_SHIFT, 0x27)
            ')' -> KeyEntry(MODIFIER_SHIFT, 0x27)
            '_' -> KeyEntry(MODIFIER_SHIFT, 0x2D)
            '+' -> KeyEntry(MODIFIER_SHIFT, 0x2E)
            '{' -> KeyEntry(MODIFIER_SHIFT, 0x2F)
            '}' -> KeyEntry(MODIFIER_SHIFT, 0x30)
            '|' -> KeyEntry(MODIFIER_SHIFT, 0x31)
            ':' -> KeyEntry(MODIFIER_SHIFT, 0x33)
            '"' -> KeyEntry(MODIFIER_SHIFT, 0x34)
            '<' -> KeyEntry(MODIFIER_SHIFT, 0x36)
            '>' -> KeyEntry(MODIFIER_SHIFT, 0x37)
            '?' -> KeyEntry(MODIFIER_SHIFT, 0x38)
            '~' -> KeyEntry(MODIFIER_SHIFT, 0x35)
            else -> null
        }
    }

    fun typeAsciiString(
        text: String,
        baseModifierMask: Byte,
        controller: BluetoothHidController,
        charDelayMs: Long = 14L
    ) {
        for (c in text) {
            val entry = getEntryForChar(c)
            if (entry != null) {
                val mod = (baseModifierMask.toInt() or entry.modifier.toInt()).toByte()
                controller.sendKeyTap(mod, entry.scancode, holdMs = 12L)
                Thread.sleep(charDelayMs)
            }
        }
    }
}
