package dev.neko.kvmeow.ui

import dev.neko.kvmeow.hid.HidScancodes

/**
 * 🐾 3-State Sticky Modifier (Off, One-Shot Latched, Locked) 🐾
 */
enum class ModifierLockState {
    OFF,
    LATCHED, // One-shot: applies to next stroke, then resets
    LOCKED   // Persistent: stays held until tapped again
}

data class StickyModifiers(
    val ctrl: ModifierLockState = ModifierLockState.OFF,
    val altOpt: ModifierLockState = ModifierLockState.OFF,
    val cmdWin: ModifierLockState = ModifierLockState.OFF,
    val shift: ModifierLockState = ModifierLockState.OFF
) {
    /**
     * Compute the standard USB HID modifier bitmask (Page 0x07)
     */
    fun toBitmask(): Byte {
        var mask = 0
        if (ctrl != ModifierLockState.OFF) mask = mask or HidScancodes.MODIFIER_LEFT_CTRL.toInt()
        if (altOpt != ModifierLockState.OFF) mask = mask or HidScancodes.MODIFIER_LEFT_ALT.toInt()
        if (cmdWin != ModifierLockState.OFF) mask = mask or HidScancodes.MODIFIER_LEFT_GUI.toInt()
        if (shift != ModifierLockState.OFF) mask = mask or HidScancodes.MODIFIER_LEFT_SHIFT.toInt()
        return mask.toByte()
    }

    fun hasAnyActive(): Boolean =
        ctrl != ModifierLockState.OFF || altOpt != ModifierLockState.OFF ||
        cmdWin != ModifierLockState.OFF || shift != ModifierLockState.OFF

    /**
     * Auto-release one-shot latched modifiers after dispatching a keystroke
     */
    fun consumeLatches(): StickyModifiers {
        return copy(
            ctrl = if (ctrl == ModifierLockState.LATCHED) ModifierLockState.OFF else ctrl,
            altOpt = if (altOpt == ModifierLockState.LATCHED) ModifierLockState.OFF else altOpt,
            cmdWin = if (cmdWin == ModifierLockState.LATCHED) ModifierLockState.OFF else cmdWin,
            shift = if (shift == ModifierLockState.LATCHED) ModifierLockState.OFF else shift
        )
    }

    fun releaseAll(): StickyModifiers = StickyModifiers()
}
