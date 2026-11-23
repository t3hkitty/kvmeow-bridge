package net.artkitty.kvmeowboard.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.artkitty.kvmeowboard.hid.BluetoothHidController
import net.artkitty.kvmeowboard.hid.CustomHidMacro
import net.artkitty.kvmeowboard.hid.ModifierStateMachine

enum class QuickMacroId(val defaultLabel: String) {
    COPY("Copy"),
    PASTE("Paste"),
    UNDO("Undo"),
    SELECT_ALL("Select All"),
    LOCK_HOST("Lock Host")
}

private data class MacroChord(
    val modMask: Byte,
    val usageCode: Byte
)

private fun chordFor(macro: QuickMacroId, os: HostOs): MacroChord = when (macro) {
    QuickMacroId.COPY -> when (os) {
        HostOs.MACOS -> MacroChord(0x08, 0x06) // Cmd + C
        HostOs.WINDOWS, HostOs.LINUX -> MacroChord(0x01, 0x06) // Ctrl + C
    }
    QuickMacroId.PASTE -> when (os) {
        HostOs.MACOS -> MacroChord(0x08, 0x19) // Cmd + V
        HostOs.WINDOWS, HostOs.LINUX -> MacroChord(0x01, 0x19) // Ctrl + V
    }
    QuickMacroId.UNDO -> when (os) {
        HostOs.MACOS -> MacroChord(0x08, 0x1D) // Cmd + Z
        HostOs.WINDOWS, HostOs.LINUX -> MacroChord(0x01, 0x1D) // Ctrl + Z
    }
    QuickMacroId.SELECT_ALL -> when (os) {
        HostOs.MACOS -> MacroChord(0x08, 0x04) // Cmd + A
        HostOs.WINDOWS, HostOs.LINUX -> MacroChord(0x01, 0x04) // Ctrl + A
    }
    QuickMacroId.LOCK_HOST -> when (os) {
        HostOs.MACOS -> MacroChord(0x09, 0x14) // Ctrl (0x01) + Cmd (0x08) + Q (0x14)
        HostOs.WINDOWS -> MacroChord(0x08, 0x0F) // Win (0x08) + L (0x0F)
        HostOs.LINUX -> MacroChord(0x08, 0x0F) // Super (0x08) + L (0x0F)
    }
}

@Composable
fun QuickMacroBar(
    activeOs: HostOs,
    modifierMachine: ModifierStateMachine,
    hidController: BluetoothHidController,
    enabledMacros: List<QuickMacroId> = QuickMacroId.entries,
    customLabels: Map<QuickMacroId, String> = emptyMap(),
    customMacros: List<CustomHidMacro> = emptyList(),
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Built-in OS-Adaptive Macros
        enabledMacros.forEach { macro ->
            val chord = chordFor(macro, activeOs)
            val label = customLabels[macro]?.takeIf { it.isNotBlank() } ?: macro.defaultLabel

            DeckKeyCap(
                label = label,
                isActive = false,
                onClick = {
                    val mergedMask = (modifierMachine.currentMask().toInt() or chord.modMask.toInt()).toByte()
                    hidController.sendKeyTap(mergedMask, chord.usageCode, holdMs = 20L)
                    modifierMachine.consumeOneShots()
                },
                modifier = Modifier.widthIn(min = 64.dp)
            )
        }

        // 2. User-Defined Custom HID Macros
        customMacros.forEach { custom ->
            DeckKeyCap(
                label = custom.label,
                isActive = false,
                onClick = {
                    val mergedMask = (modifierMachine.currentMask().toInt() or custom.modifierMask.toInt()).toByte()
                    hidController.sendKeyTap(mergedMask, custom.usageCode, holdMs = 20L)
                    modifierMachine.consumeOneShots()
                },
                modifier = Modifier.widthIn(min = 64.dp)
            )
        }
    }
}

@Composable
fun DeckKeyCap(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalDeckPalette.current
    val bg = if (isActive) palette.buttonSelected else palette.buttonBg
    val fg = if (isActive) palette.bg else palette.textPrimary

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier
            .height(36.dp)
            .border(
                1.dp,
                if (isActive) palette.accentPrimary else palette.cardBorder,
                RoundedCornerShape(8.dp)
            )
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
