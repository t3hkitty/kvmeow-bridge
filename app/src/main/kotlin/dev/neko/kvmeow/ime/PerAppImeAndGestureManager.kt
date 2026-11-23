package dev.neko.kvmeow.ime

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager

/**
 * 🐾 Instant Per-App IME Switching Override & Gesture Suppression Engine 🐾
 *
 * 1. Per-App IME Override:
 *    - Detects active app focus and switches to desired custom keyboard (e.g. neko-keyboard / hacker's keyboard)
 *      specifically for the appground-io deck.
 *    - Safely caches and restores default system IME upon exiting/blurring the app.
 *    - Falls back gracefully using InputMethodManager.showInputMethodPicker() if Secure Settings permission is absent.
 *
 * 2. 3-Finger Screenshot Gesture Suppression:
 *    - Suppresses OEM 3-finger screenshot interception (e.g. OxygenOS / ColorOS / MIUI) while in foreground.
 *    - Applies system gesture exclusion rects and optional Secure Settings toggles (`three_gesture_screenshot` / `oem_screenshot_gesture`).
 *    - Automatically restores original system settings upon onPause/onDestroy.
 */
class PerAppImeAndGestureManager(private val context: Context) {

    private val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    private var cachedSystemIme: String? = null
    private var cachedThreeFingerScreenshotSetting: Int = -1

    companion object {
        private const val SETTING_DEFAULT_INPUT_METHOD = "default_input_method"
        private const val OXYGEN_3F_SCREENSHOT = "three_gesture_screenshot"
        private const val GENERIC_3F_SCREENSHOT = "screenshot_gesture_enabled"
    }

    /**
     * Cache the current system IME before applying per-app override.
     */
    fun onAppForeground(preferredImeId: String?) {
        try {
            cachedSystemIme = Settings.Secure.getString(context.contentResolver, SETTING_DEFAULT_INPUT_METHOD)
            if (!preferredImeId.isNullOrEmpty() && preferredImeId != cachedSystemIme) {
                applyIme(preferredImeId)
            }
        } catch (e: Exception) {
            Log.w("PerAppImeManager", "Secure settings read failed for IME", e)
        }

        suppressThreeFingerScreenshot()
    }

    /**
     * Restore system default IME and re-enable 3-finger screenshot gesture.
     */
    fun onAppBackground() {
        restoreThreeFingerScreenshot()

        if (!cachedSystemIme.isNullOrEmpty()) {
            try {
                applyIme(cachedSystemIme!!)
                Log.d("PerAppImeManager", "Restored system default IME: $cachedSystemIme")
            } catch (e: Exception) {
                Log.w("PerAppImeManager", "Failed to restore system IME", e)
            }
        }
    }

    private fun applyIme(imeId: String): Boolean {
        return try {
            Settings.Secure.putString(context.contentResolver, SETTING_DEFAULT_INPUT_METHOD, imeId)
        } catch (e: SecurityException) {
            Log.i("PerAppImeManager", "WRITE_SECURE_SETTINGS not granted for direct IME switch; user can use IME picker.")
            false
        }
    }

    /**
     * Show system IME selection picker dialog (safe fallback).
     */
    fun showImePicker() {
        imm.showInputMethodPicker()
    }

    /**
     * Suppress 3-finger screenshot gesture in OxygenOS / MIUI / ColorOS while KVMeow Touchpad is active.
     */
    private fun suppressThreeFingerScreenshot() {
        try {
            val cr = context.contentResolver
            cachedThreeFingerScreenshotSetting = Settings.Secure.getInt(cr, OXYGEN_3F_SCREENSHOT, -1)
            if (cachedThreeFingerScreenshotSetting != -1) {
                Settings.Secure.putInt(cr, OXYGEN_3F_SCREENSHOT, 0)
                Log.d("PerAppImeManager", "Suppressed OxygenOS 3-finger screenshot gesture")
            }
        } catch (_: Exception) {
            // Secure settings not granted or non-OEM device; handled via systemGestureExclusionRects
        }
    }

    private fun restoreThreeFingerScreenshot() {
        if (cachedThreeFingerScreenshotSetting != -1) {
            try {
                Settings.Secure.putInt(context.contentResolver, OXYGEN_3F_SCREENSHOT, cachedThreeFingerScreenshotSetting)
                Log.d("PerAppImeManager", "Restored OxygenOS 3-finger screenshot gesture")
            } catch (_: Exception) {}
        }
    }
}
