package dev.neko.kvmeow.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 🐾 KVMeow Bridge Preferences & State Persistence Store 🐾
 *
 * Persists:
 * - IME visibility & per-app IME profile
 * - Target OS selection (Windows, macOS, Linux)
 * - Emoji Encoding Mode (Hex Chord vs Clipboard Paste)
 * - Enabled Computer slots (Tab 1: Win 11, Tab 2: MacBook, optional Slot 3)
 * - KDE Connect Clipboard Sync toggle
 * - 10-Key open state, Accountant Full-screen state
 * - Calculator tape & memory values
 */
class DeckPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("kvmeow_deck_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TARGET_OS = "target_os"
        private const val KEY_EMOJI_MODE = "emoji_mode"
        private const val KEY_SLOT_3_ENABLED = "slot_3_enabled"
        private const val KEY_SLOT_1_NAME = "slot_1_name"
        private const val KEY_SLOT_1_MAC = "slot_1_mac"
        private const val KEY_SLOT_2_NAME = "slot_2_name"
        private const val KEY_SLOT_2_MAC = "slot_2_mac"
        private const val KEY_SLOT_3_NAME = "slot_3_name"
        private const val KEY_SLOT_3_MAC = "slot_3_mac"
        private const val KEY_ACTIVE_SLOT = "active_slot"
        private const val KEY_KDE_CLIPBOARD_SYNC = "kde_clipboard_sync"
        private const val KEY_NUMPAD_OPEN = "numpad_open"
        private const val KEY_NUMPAD_FULLSCREEN = "numpad_fullscreen"
        private const val KEY_CALC_TAPE = "calc_tape"
        private const val KEY_CALC_MEMORY = "calc_memory"
        private const val KEY_PER_APP_IME_ID = "per_app_ime_id"
        private const val KEY_SLOT_1_JIGGLE = "slot_1_jiggle"
        private const val KEY_SLOT_2_JIGGLE = "slot_2_jiggle"
        private const val KEY_SLOT_3_JIGGLE = "slot_3_jiggle"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_JITTER_INTERVAL_SEC = "jitter_interval_sec"
    }

    var slot1JiggleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SLOT_1_JIGGLE, false)
        set(value) = prefs.edit().putBoolean(KEY_SLOT_1_JIGGLE, value).apply()

    var slot2JiggleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SLOT_2_JIGGLE, false)
        set(value) = prefs.edit().putBoolean(KEY_SLOT_2_JIGGLE, value).apply()

    var slot3JiggleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SLOT_3_JIGGLE, false)
        set(value) = prefs.edit().putBoolean(KEY_SLOT_3_JIGGLE, value).apply()

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "DARK_KAWAII") ?: "DARK_KAWAII"
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    var jitterIntervalSec: Int
        get() = prefs.getInt(KEY_JITTER_INTERVAL_SEC, 45)
        set(value) = prefs.edit().putInt(KEY_JITTER_INTERVAL_SEC, value).apply()

    var targetOs: String
        get() = prefs.getString(KEY_TARGET_OS, "MACOS") ?: "MACOS"
        set(value) = prefs.edit().putString(KEY_TARGET_OS, value).apply()

    var emojiMode: String
        get() = prefs.getString(KEY_EMOJI_MODE, "HEX_CHORD") ?: "HEX_CHORD"
        set(value) = prefs.edit().putString(KEY_EMOJI_MODE, value).apply()

    var slot3Enabled: Boolean
        get() = prefs.getBoolean(KEY_SLOT_3_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SLOT_3_ENABLED, value).apply()

    var slot1Name: String
        get() = prefs.getString(KEY_SLOT_1_NAME, "Win 11") ?: "Win 11"
        set(value) = prefs.edit().putString(KEY_SLOT_1_NAME, value).apply()

    var slot1Mac: String
        get() = prefs.getString(KEY_SLOT_1_MAC, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SLOT_1_MAC, value).apply()

    var slot2Name: String
        get() = prefs.getString(KEY_SLOT_2_NAME, "MacBook") ?: "MacBook"
        set(value) = prefs.edit().putString(KEY_SLOT_2_NAME, value).apply()

    var slot2Mac: String
        get() = prefs.getString(KEY_SLOT_2_MAC, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SLOT_2_MAC, value).apply()

    var slot3Name: String
        get() = prefs.getString(KEY_SLOT_3_NAME, "Work Linux") ?: "Work Linux"
        set(value) = prefs.edit().putString(KEY_SLOT_3_NAME, value).apply()

    var slot3Mac: String
        get() = prefs.getString(KEY_SLOT_3_MAC, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SLOT_3_MAC, value).apply()

    var activeSlot: Int
        get() = prefs.getInt(KEY_ACTIVE_SLOT, 1)
        set(value) = prefs.edit().putInt(KEY_ACTIVE_SLOT, value).apply()

    var isKdeClipboardSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_KDE_CLIPBOARD_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_KDE_CLIPBOARD_SYNC, value).apply()

    var isNumpadOpen: Boolean
        get() = prefs.getBoolean(KEY_NUMPAD_OPEN, false)
        set(value) = prefs.edit().putBoolean(KEY_NUMPAD_OPEN, value).apply()

    var isNumpadFullscreen: Boolean
        get() = prefs.getBoolean(KEY_NUMPAD_FULLSCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_NUMPAD_FULLSCREEN, value).apply()

    var calcTape: String
        get() = prefs.getString(KEY_CALC_TAPE, "0") ?: "0"
        set(value) = prefs.edit().putString(KEY_CALC_TAPE, value).apply()

    var calcMemory: Double
        get() = prefs.getFloat(KEY_CALC_MEMORY, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_CALC_MEMORY, value.toFloat()).apply()

    var perAppImeId: String
        get() = prefs.getString(KEY_PER_APP_IME_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PER_APP_IME_ID, value).apply()
}
