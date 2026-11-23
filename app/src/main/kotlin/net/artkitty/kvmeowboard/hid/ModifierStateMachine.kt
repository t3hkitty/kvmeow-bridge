package net.artkitty.kvmeowboard.hid

enum class ModifierState {
    OFF,
    ONESHOT,
    LOCKED
}

/**
 * 🐾 ModifierStateMachine 🐾
 * 3-state modifier logic:
 * - Single tap: ONESHOT (latched for next key/click, then auto-releases)
 * - Double-tap / long-press: LOCKED (persists until tapped again)
 * - releaseAll(): Clears all modifiers to 0x00
 */
class ModifierStateMachine {
    var ctrl: ModifierState = ModifierState.OFF
        private set
    var shift: ModifierState = ModifierState.OFF
        private set
    var altOpt: ModifierState = ModifierState.OFF
        private set
    var cmdWin: ModifierState = ModifierState.OFF
        private set

    companion object {
        const val MASK_CTRL: Byte = 0x01
        const val MASK_SHIFT: Byte = 0x02
        const val MASK_ALT: Byte = 0x04
        const val MASK_CMD_WIN: Byte = 0x08
    }

    fun toggleCtrl(asLock: Boolean = false) {
        ctrl = cycle(ctrl, asLock)
    }

    fun toggleShift(asLock: Boolean = false) {
        shift = cycle(shift, asLock)
    }

    fun toggleAltOpt(asLock: Boolean = false) {
        altOpt = cycle(altOpt, asLock)
    }

    fun toggleCmdWin(asLock: Boolean = false) {
        cmdWin = cycle(cmdWin, asLock)
    }

    private fun cycle(current: ModifierState, asLock: Boolean): ModifierState {
        return if (asLock) {
            if (current == ModifierState.LOCKED) ModifierState.OFF else ModifierState.LOCKED
        } else {
            when (current) {
                ModifierState.OFF -> ModifierState.ONESHOT
                ModifierState.ONESHOT -> ModifierState.LOCKED
                ModifierState.LOCKED -> ModifierState.OFF
            }
        }
    }

    fun currentMask(): Byte {
        var mask = 0
        if (ctrl != ModifierState.OFF) mask = mask or MASK_CTRL.toInt()
        if (shift != ModifierState.OFF) mask = mask or MASK_SHIFT.toInt()
        if (altOpt != ModifierState.OFF) mask = mask or MASK_ALT.toInt()
        if (cmdWin != ModifierState.OFF) mask = mask or MASK_CMD_WIN.toInt()
        return mask.toByte()
    }

    fun consumeOneShots() {
        if (ctrl == ModifierState.ONESHOT) ctrl = ModifierState.OFF
        if (shift == ModifierState.ONESHOT) shift = ModifierState.OFF
        if (altOpt == ModifierState.ONESHOT) altOpt = ModifierState.OFF
        if (cmdWin == ModifierState.ONESHOT) cmdWin = ModifierState.OFF
    }

    fun releaseAll() {
        ctrl = ModifierState.OFF
        shift = ModifierState.OFF
        altOpt = ModifierState.OFF
        cmdWin = ModifierState.OFF
    }

    fun hasAnyActive(): Boolean {
        return ctrl != ModifierState.OFF || shift != ModifierState.OFF ||
                altOpt != ModifierState.OFF || cmdWin != ModifierState.OFF
    }
}
