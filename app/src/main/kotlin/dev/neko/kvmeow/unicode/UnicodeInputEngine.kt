package dev.neko.kvmeow.unicode

import dev.neko.kvmeow.hid.BluetoothHidManager
import dev.neko.kvmeow.hid.HidScancodes

/**
 * 🐾 Emoji & Unicode Hex-Codepoint Dispatcher & Keystroke Pipeline 🐾
 *
 * Bypasses raw HID 8-bit keycode limits by translating Unicode code points & Emojis
 * into OS-native entry sequences or clipboard paste fallback:
 *
 * 1. macOS:
 *    - Option (Alt) + 4-digit UTF-16 Hex digits.
 *    - For Supplementary Multilingual Plane (SMP) emojis (e.g., U+1F600 😀),
 *      automatically breaks down into High and Low Surrogate Hex pairs.
 * 2. Linux (IBus / GTK / Qt):
 *    - Ctrl + Shift + U -> Hex digits -> Enter.
 * 3. Windows:
 *    - Hex digits + Alt + X (WordPad/Office/RichEdit/Notepad)
 *    - Auto fallback: Clipboard injection + Ctrl + V (or Cmd + V).
 */
object UnicodeInputEngine {

    enum class TargetOs {
        WINDOWS,
        MACOS,
        LINUX
    }

    enum class EmojiMode {
        HEX_CHORD,
        CLIPBOARD_PASTE
    }

    /**
     * Dispatch an emoji or string text through OS-specific hex chord sequence.
     */
    fun sendUnicodeHex(
        text: String,
        targetOs: TargetOs,
        hidManager: BluetoothHidManager,
        onClipboardFallback: ((String) -> Unit)? = null
    ) {
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            // If it's a standard ASCII printable character (32..126), dispatch directly
            if (codePoint in 32..126 && codePoint != 92 /* backslash */) {
                hidManager.sendKeyStroke(codePoint.toChar())
            } else {
                when (targetOs) {
                    TargetOs.MACOS -> {
                        sendMacUnicodeHex(codePoint, hidManager, onClipboardFallback, text.substring(i, i + charCount))
                    }
                    TargetOs.LINUX -> {
                        sendLinuxUnicodeHex(codePoint, hidManager)
                    }
                    TargetOs.WINDOWS -> {
                        sendWindowsUnicodeHex(codePoint, hidManager, onClipboardFallback, text.substring(i, i + charCount))
                    }
                }
            }
            i += charCount
            try {
                Thread.sleep(12)
            } catch (_: Exception) {}
        }
    }

    /**
     * macOS: Option + Hex Digits (with surrogate pairs for codePoint > 0xFFFF)
     */
    private fun sendMacUnicodeHex(
        codePoint: Int,
        hidManager: BluetoothHidManager,
        onClipboardFallback: ((String) -> Unit)?,
        emojiStr: String
    ) {
        if (codePoint <= 0xFFFF) {
            val hex = String.format("%04X", codePoint)
            sendOptionHexDigits(hex, hidManager)
        } else if (codePoint <= 0x10FFFF) {
            // Calculate UTF-16 Surrogates
            val chars = Character.toChars(codePoint)
            if (chars.size == 2) {
                val highHex = String.format("%04X", chars[0].code)
                val lowHex = String.format("%04X", chars[1].code)
                sendOptionHexDigits(highHex, hidManager)
                sendOptionHexDigits(lowHex, hidManager)
            } else {
                onClipboardFallback?.invoke(emojiStr)
            }
        } else {
            onClipboardFallback?.invoke(emojiStr)
        }
    }

    private fun sendOptionHexDigits(hex: String, hidManager: BluetoothHidManager) {
        for (digit in hex) {
            val scancode = getHexScancode(digit)
            if (scancode != HidScancodes.KEY_NONE) {
                // macOS Unicode Hex Input requires Option modifier held down
                hidManager.sendRawKeycode(scancode, HidScancodes.MODIFIER_LEFT_ALT)
                try { Thread.sleep(8) } catch (_: Exception) {}
            }
        }
    }

    /**
     * Linux: Ctrl + Shift + U -> Hex Code Point -> Enter
     */
    private fun sendLinuxUnicodeHex(codePoint: Int, hidManager: BluetoothHidManager) {
        val hex = Integer.toHexString(codePoint)
        // 1. Press Ctrl + Shift + U
        val ctrlShift = (HidScancodes.MODIFIER_LEFT_CTRL.toInt() or HidScancodes.MODIFIER_LEFT_SHIFT.toInt()).toByte()
        hidManager.sendRawKeycode(HidScancodes.KEY_U, ctrlShift)
        try { Thread.sleep(15) } catch (_: Exception) {}

        // 2. Type hex digits
        for (digit in hex) {
            val scancode = getHexScancode(digit)
            if (scancode != HidScancodes.KEY_NONE) {
                hidManager.sendRawKeycode(scancode, HidScancodes.MODIFIER_NONE)
                try { Thread.sleep(6) } catch (_: Exception) {}
            }
        }

        // 3. Press Enter to commit unicode sequence
        hidManager.sendRawKeycode(HidScancodes.KEY_ENTER, HidScancodes.MODIFIER_NONE)
        try { Thread.sleep(10) } catch (_: Exception) {}
    }

    /**
     * Windows: Hex Code Point -> Alt + X
     */
    private fun sendWindowsUnicodeHex(
        codePoint: Int,
        hidManager: BluetoothHidManager,
        onClipboardFallback: ((String) -> Unit)?,
        emojiStr: String
    ) {
        // Fallback for emojis on Windows is often cleaner via clipboard paste
        if (codePoint > 0xFFFF && onClipboardFallback != null) {
            onClipboardFallback(emojiStr)
            return
        }

        val hex = String.format("%04X", codePoint)
        // Type hex characters first
        for (digit in hex) {
            val scancode = getHexScancode(digit)
            if (scancode != HidScancodes.KEY_NONE) {
                hidManager.sendRawKeycode(scancode, HidScancodes.MODIFIER_NONE)
                try { Thread.sleep(8) } catch (_: Exception) {}
            }
        }
        // Press Alt + X to convert preceding hex into Unicode glyph
        hidManager.sendRawKeycode(HidScancodes.KEY_X, HidScancodes.MODIFIER_LEFT_ALT)
        try { Thread.sleep(15) } catch (_: Exception) {}
    }

    fun getHexScancode(ch: Char): Byte {
        return when (ch.uppercaseChar()) {
            '0' -> HidScancodes.KEY_0
            '1' -> HidScancodes.KEY_1
            '2' -> HidScancodes.KEY_2
            '3' -> HidScancodes.KEY_3
            '4' -> HidScancodes.KEY_4
            '5' -> HidScancodes.KEY_5
            '6' -> HidScancodes.KEY_6
            '7' -> HidScancodes.KEY_7
            '8' -> HidScancodes.KEY_8
            '9' -> HidScancodes.KEY_9
            'A' -> HidScancodes.KEY_A
            'B' -> HidScancodes.KEY_B
            'C' -> HidScancodes.KEY_C
            'D' -> HidScancodes.KEY_D
            'E' -> HidScancodes.KEY_E
            'F' -> HidScancodes.KEY_F
            else -> HidScancodes.KEY_NONE
        }
    }
}
