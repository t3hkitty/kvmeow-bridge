package net.artkitty.kvmeowboard.hid

import net.artkitty.kvmeowboard.ui.HostOs

/**
 * 🔣 UnicodeHexEncoder 🔣
 * Encodes Unicode emojis and characters (including SMP supplementary plane surrogates)
 * into OS-native HID hex keyboard sequences over Report ID 1 without network or server:
 *
 * - macOS: Holds Option (0x04) while typing 4 or 8 hex digits, then releases Option.
 * - Windows: Types hex digits followed by Alt + X (0x04 + 0x1B).
 * - Linux: Types Ctrl + Shift + U (0x03 + 0x18), hex digits, and Enter (0x28).
 */
object UnicodeHexEncoder {

    fun sendEmoji(
        emoji: String,
        targetOs: HostOs,
        controller: BluetoothHidController
    ) {
        val codepoints = emoji.codePoints().toArray()
        for (cp in codepoints) {
            when (targetOs) {
                HostOs.MACOS -> sendMacOsHex(cp, controller)
                HostOs.WINDOWS -> sendWindowsHex(cp, controller)
                HostOs.LINUX -> sendLinuxHex(cp, controller)
            }
        }
    }

    fun sendUnicodeText(
        text: String,
        targetOs: HostOs,
        controller: BluetoothHidController
    ) = sendEmoji(text, targetOs, controller)

    private fun sendMacOsHex(codePoint: Int, controller: BluetoothHidController) {
        // In macOS Unicode Hex Input, SMP characters (codePoint > 0xFFFF) are entered as UTF-16 surrogate pairs
        val chars = Character.toChars(codePoint)
        for (ch in chars) {
            val hexStr = String.format("%04X", ch.code)
            // Hold Left Option (0x04)
            for (digit in hexStr) {
                val scancode = hexDigitToScancode(digit)
                controller.sendKeyTap(0x04.toByte(), scancode, holdMs = 15L)
                Thread.sleep(12L)
            }
        }
    }

    private fun sendWindowsHex(codePoint: Int, controller: BluetoothHidController) {
        // Windows Wordpad/Office hex mode: type hex codepoint then Alt+X
        val hexStr = Integer.toHexString(codePoint).uppercase()
        for (digit in hexStr) {
            val scancode = hexDigitToScancode(digit)
            controller.sendKeyTap(0x00, scancode, holdMs = 12L)
            Thread.sleep(10L)
        }
        // Alt + X: modifier 0x04, keycode 'x' (0x1B)
        controller.sendKeyTap(0x04.toByte(), 0x1B.toByte(), holdMs = 20L)
    }

    private fun sendLinuxHex(codePoint: Int, controller: BluetoothHidController) {
        // Linux IBus Unicode entry: Ctrl + Shift + U, hex digits, Enter
        // Ctrl (0x01) + Shift (0x02) = 0x03, 'u' = 0x18
        controller.sendKeyTap(0x03.toByte(), 0x18.toByte(), holdMs = 25L)
        Thread.sleep(20L)

        val hexStr = Integer.toHexString(codePoint).uppercase()
        for (digit in hexStr) {
            val scancode = hexDigitToScancode(digit)
            controller.sendKeyTap(0x00, scancode, holdMs = 12L)
            Thread.sleep(10L)
        }

        // Enter (0x28)
        controller.sendKeyTap(0x00, 0x28.toByte(), holdMs = 15L)
    }

    private fun hexDigitToScancode(c: Char): Byte {
        return when (c.uppercaseChar()) {
            '1' -> 0x1E
            '2' -> 0x1F
            '3' -> 0x20
            '4' -> 0x21
            '5' -> 0x22
            '6' -> 0x23
            '7' -> 0x24
            '8' -> 0x25
            '9' -> 0x26
            '0' -> 0x27
            'A' -> 0x04
            'B' -> 0x05
            'C' -> 0x06
            'D' -> 0x07
            'E' -> 0x08
            'F' -> 0x09
            else -> 0x27
        }
    }
}
