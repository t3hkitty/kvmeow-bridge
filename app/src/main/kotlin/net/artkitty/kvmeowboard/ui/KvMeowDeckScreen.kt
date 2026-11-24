package net.artkitty.kvmeowboard.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.artkitty.kvmeowboard.calc.AccountantCalculatorController
import net.artkitty.kvmeowboard.hid.*
import kotlin.math.abs

val LocalDeckPalette = compositionLocalOf { KvMeowTheme.DarkKawaii }

@Composable
fun KvMeowDeckScreen(
    themeMode: DeckThemeMode,
    onThemeChange: (DeckThemeMode) -> Unit,
    hosts: List<HostSlot>,
    activeHostId: String,
    onSelectHost: (HostSlot) -> Unit,
    onToggleHostJiggle: (String) -> Unit,
    showThirdSlotInSettings: Boolean,
    onToggleThirdSlotInSettings: (Boolean) -> Unit,
    bondedDevices: List<BluetoothDevice> = emptyList(),
    onAssignSlotMac: (slotId: String, macAddress: String?) -> Unit = { _, _ -> },
    onUpdateThirdSlotMeta: (label: String, os: HostOs) -> Unit = { _, _ -> },
    onRefreshBondedDevices: () -> Unit = {},
    hidController: BluetoothHidController,
    modifierMachine: ModifierStateMachine,
    calcController: AccountantCalculatorController
) {
    val palette = KvMeowTheme.getPalette(themeMode)
    var showSettings by remember { mutableStateOf(false) }
    var showImePanel by remember { mutableStateOf(true) }
    var show10KeyModal by remember { mutableStateOf(false) }
    var is10KeyFullScreen by remember { mutableStateOf(false) }
    var imeDraftText by remember { mutableStateOf("") }

    val activeSlot = hosts.firstOrNull { it.id == activeHostId } ?: hosts.firstOrNull() ?: HostSlot(
        id = "win11",
        label = "Win 11",
        os = HostOs.WINDOWS,
        device = null,
        jiggleEnabled = false
    )
    val isMac = activeSlot.os == HostOs.MACOS
    val cmdWinLabel = if (isMac) "Cmd ⌘" else "Win ⊞"
    val altOptLabel = if (isMac) "Opt ⌥" else "Alt"

    CompositionLocalProvider(LocalDeckPalette provides palette) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // ==========================================
            // 1. TOP DECK HEADER: COMPUTER TABS + PER-HOST JIGGLE
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header Bar: Title, Theme Cycler, Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🐾 KVMeowboard",
                                color = palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[Blek-Style Deck]",
                                color = palette.accentPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 3-Theme Cycle Button
                            Button(
                                onClick = {
                                    val next = when (themeMode) {
                                        DeckThemeMode.TRON -> DeckThemeMode.DARK_KAWAII
                                        DeckThemeMode.DARK_KAWAII -> DeckThemeMode.BRIGHT_KAWAII
                                        DeckThemeMode.BRIGHT_KAWAII -> DeckThemeMode.TRON
                                    }
                                    onThemeChange(next)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.buttonBg),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .border(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = when (themeMode) {
                                        DeckThemeMode.TRON -> "⚡ Tron"
                                        DeckThemeMode.DARK_KAWAII -> "🌙 Dark"
                                        DeckThemeMode.BRIGHT_KAWAII -> "🌸 Bright"
                                    },
                                    color = palette.accentPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { showSettings = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = palette.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Adaptive Computer Tabs + Per-Host Jiggle Buttons
                    val activeSlots = if (showThirdSlotInSettings) hosts.take(3) else hosts.take(2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeSlots.forEach { slot ->
                            val isSelected = slot.id == activeHostId
                            Column(modifier = Modifier.weight(1f)) {
                                // Tab Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) palette.buttonSelected else palette.buttonBg)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) palette.accentPrimary else palette.cardBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectHost(slot) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${if (slot.os == HostOs.WINDOWS) "🪟" else if (slot.os == HostOs.MACOS) "🍎" else "🐧"} ${slot.label}",
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Boolean checkbox-style Jiggle toggle button directly underneath
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (slot.jiggleEnabled) palette.statusActive else palette.surface)
                                        .border(
                                            width = if (slot.jiggleEnabled) 1.5.dp else 1.dp,
                                            color = if (slot.jiggleEnabled) palette.textPrimary else palette.cardBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onToggleHostJiggle(slot.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (slot.jiggleEnabled) "☕ Jiggle ON" else "☕ Jiggle OFF",
                                        color = if (slot.jiggleEnabled) palette.bg else palette.textSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // 2. TOUCHPAD / MOUSEPAD SURFACE (weight(1f))
            // ==========================================
            var startY by remember { mutableStateOf(0f) }
            var isTracking3Finger by remember { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.5.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🖱️ Touchpad (3-Finger Swipe = ${if (isMac) "Mission Control" else "Task View"})",
                            color = palette.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "1-Finger Move • 2-Finger Scroll",
                            color = palette.accentPrimary,
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Touchpad interaction area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(palette.surface, RoundedCornerShape(10.dp))
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val count = event.changes.size

                                        if (count == 3) {
                                            val change = event.changes[0]
                                            if (!isTracking3Finger) {
                                                isTracking3Finger = true
                                                startY = change.position.y
                                            } else {
                                                val dy = change.position.y - startY
                                                if (abs(dy) > 120f) {
                                                    if (dy < 0) {
                                                        // Swipe UP
                                                        if (isMac) {
                                                            // macOS Mission Control: Ctrl + Up
                                                            hidController.sendKeyTap(0x01, 0x52)
                                                        } else {
                                                            // Windows Task View: Win + Tab
                                                            hidController.sendKeyTap(0x08, 0x2B)
                                                        }
                                                    } else {
                                                        // Swipe DOWN
                                                        if (isMac) {
                                                            // macOS App Exposé: Ctrl + Down
                                                            hidController.sendKeyTap(0x01, 0x51)
                                                        } else {
                                                            // Windows Show Desktop: Win + D
                                                            hidController.sendKeyTap(0x08, 0x07)
                                                        }
                                                    }
                                                    isTracking3Finger = false
                                                    startY = change.position.y
                                                }
                                            }
                                            event.changes.forEach { it.consume() }
                                        } else {
                                            isTracking3Finger = false
                                            if (count == 1) {
                                                val change = event.changes[0]
                                                if (change.pressed && event.type == PointerEventType.Move) {
                                                    val prev = change.previousPosition
                                                    val curr = change.position
                                                    val dx = (curr.x - prev.x).coerceIn(-127f, 127f).toInt().toByte()
                                                    val dy = (curr.y - prev.y).coerceIn(-127f, 127f).toInt().toByte()
                                                    hidController.sendMouseReport(0, dx, dy, 0)
                                                    change.consume()
                                                }
                                            } else if (count == 2) {
                                                // 2-finger vertical scroll wheel
                                                val change = event.changes[0]
                                                if (change.pressed && event.type == PointerEventType.Move) {
                                                    val prev = change.previousPosition
                                                    val curr = change.position
                                                    val wheel = ((prev.y - curr.y) / 8f).coerceIn(-127f, 127f).toInt().toByte()
                                                    if (wheel != 0.toByte()) {
                                                        hidController.sendMouseReport(0, 0, 0, wheel)
                                                    }
                                                    change.consume()
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🐾 Touchpad Surface\nSwipe Up: 🚀 ${if (isMac) "Mission Control" else "Task View"} | Down: ${if (isMac) "Exposé" else "Desktop"}",
                            color = palette.textSecondary.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Left & Right Click Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                hidController.sendMouseReport(1, 0, 0, 0)
                                Thread.sleep(25)
                                hidController.sendMouseReport(0, 0, 0, 0)
                            },
                            modifier = Modifier.weight(2f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.buttonBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Left Click", color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                hidController.sendMouseReport(4, 0, 0, 0)
                                Thread.sleep(25)
                                hidController.sendMouseReport(0, 0, 0, 0)
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.buttonBg.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Wheel", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                hidController.sendMouseReport(2, 0, 0, 0)
                                Thread.sleep(25)
                                hidController.sendMouseReport(0, 0, 0, 0)
                            },
                            modifier = Modifier.weight(2f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.buttonBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Right Click", color = palette.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // 3. POP-UNDER IME PANEL
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⌨️ IME Input Bar",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Quick Mission Control / Task View Button
                            Button(
                                onClick = {
                                    if (isMac) hidController.sendKeyTap(0x01, 0x52)
                                    else hidController.sendKeyTap(0x08, 0x2B)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.buttonBg),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(
                                    text = if (isMac) "🚀 Mission Ctrl" else "🚀 Task View",
                                    color = palette.statusActive,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // ^ 10-Key Pop-Out Button
                            Button(
                                onClick = { show10KeyModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accentPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("^ 10-KEY", color = palette.bg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Text Field + Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = imeDraftText,
                            onValueChange = { imeDraftText = it },
                            placeholder = {
                                Text("Type with Gboard / IME here...", color = palette.textSecondary.copy(alpha = 0.6f), fontSize = 11.sp)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(palette.surface, RoundedCornerShape(8.dp)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary,
                                focusedBorderColor = palette.accentPrimary,
                                unfocusedBorderColor = palette.cardBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (imeDraftText.isNotEmpty()) {
                                    val textToSend = imeDraftText
                                    imeDraftText = ""
                                    Thread {
                                        AsciiHidKeymap.typeAsciiString(
                                            textToSend,
                                            modifierMachine.currentMask(),
                                            hidController
                                        )
                                        modifierMachine.consumeOneShots()
                                    }.start()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accentSecondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("Send", color = palette.bg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick Emoji Strip (dispatches via UnicodeHexEncoder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("😀", "😂", "👍", "🔥", "🚀", "🎉", "❤️", "🐱", "🐾", "✨", "💯", "💼").forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.buttonBg)
                                    .border(1.dp, palette.cardBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        Thread {
                                            UnicodeHexEncoder.sendEmoji(emoji, activeSlot.os, hidController)
                                        }.start()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // 4. STICKY MODIFIER HOLD STRIP + ARROW CLUSTER
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Modifier Hold Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModifierKeyButton(
                            label = "Ctrl",
                            state = modifierMachine.ctrl,
                            palette = palette,
                            modifier = Modifier.weight(1f),
                            onClick = { modifierMachine.toggleCtrl() }
                        )

                        ModifierKeyButton(
                            label = altOptLabel,
                            state = modifierMachine.altOpt,
                            palette = palette,
                            modifier = Modifier.weight(1f),
                            onClick = { modifierMachine.toggleAltOpt() }
                        )

                        ModifierKeyButton(
                            label = cmdWinLabel,
                            state = modifierMachine.cmdWin,
                            palette = palette,
                            modifier = Modifier.weight(1f),
                            onClick = { modifierMachine.toggleCmdWin() }
                        )

                        ModifierKeyButton(
                            label = "Shift ⇧",
                            state = modifierMachine.shift,
                            palette = palette,
                            modifier = Modifier.weight(1f),
                            onClick = { modifierMachine.toggleShift() }
                        )

                        // Global Release (Rel) button
                        Button(
                            onClick = { modifierMachine.releaseAll() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (modifierMachine.hasAnyActive()) palette.accentSecondary else palette.buttonBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                "Rel",
                                color = if (modifierMachine.hasAnyActive()) palette.bg else palette.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Macro Bar: Copy, Paste, Undo, Select All, Lock Host
                    QuickMacroBar(
                        activeOs = activeSlot.os,
                        modifierMachine = modifierMachine,
                        hidController = hidController
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Arrow Key Cluster directly beneath IME with quick Tab and Esc keys
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickKeyCap(label = "Esc", palette = palette, modifier = Modifier.weight(1f)) {
                            hidController.sendKeyTap(modifierMachine.currentMask(), AsciiHidKeymap.SCANCODE_ESCAPE)
                            modifierMachine.consumeOneShots()
                        }
                        QuickKeyCap(label = "Tab", palette = palette, modifier = Modifier.weight(1f)) {
                            hidController.sendKeyTap(modifierMachine.currentMask(), AsciiHidKeymap.SCANCODE_TAB)
                            modifierMachine.consumeOneShots()
                        }
                        QuickKeyCap(label = "←", palette = palette, modifier = Modifier.weight(1f)) {
                            hidController.sendKeyTap(modifierMachine.currentMask(), AsciiHidKeymap.SCANCODE_LEFT)
                            modifierMachine.consumeOneShots()
                        }
                        QuickKeyCap(label = "↑", palette = palette, modifier = Modifier.weight(1f)) {
                            hidController.sendKeyTap(modifierMachine.currentMask(), AsciiHidKeymap.SCANCODE_UP)
                            modifierMachine.consumeOneShots()
                        }
                        QuickKeyCap(label = "↓", palette = palette, modifier = Modifier.weight(1f)) {
                            hidController.sendKeyTap(modifierMachine.currentMask(), AsciiHidKeymap.SCANCODE_DOWN)
                            modifierMachine.consumeOneShots()
                        }
                        QuickKeyCap(label = "→", palette = palette, modifier = Modifier.weight(1f)) {
                            hidController.sendKeyTap(modifierMachine.currentMask(), AsciiHidKeymap.SCANCODE_RIGHT)
                            modifierMachine.consumeOneShots()
                        }
                        QuickKeyCap(label = "⏎", palette = palette, modifier = Modifier.weight(1f)) {
                            hidController.sendKeyTap(modifierMachine.currentMask(), AsciiHidKeymap.SCANCODE_ENTER)
                            modifierMachine.consumeOneShots()
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. ^ 10-KEY & FULL-SCREEN ACCOUNTANT CALC MODAL
        // ==========================================
        if (show10KeyModal) {
            Dialog(
                onDismissRequest = { show10KeyModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = !is10KeyFullScreen)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                    modifier = (if (is10KeyFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                        .padding(8.dp)
                        .border(2.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Calc Top Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (is10KeyFullScreen) "🧮 Full Accountant Calculator" else "🧮 10-Key Numpad",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { is10KeyFullScreen = !is10KeyFullScreen },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (is10KeyFullScreen) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                                        contentDescription = "Expand/Collapse",
                                        tint = palette.textSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { show10KeyModal = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = palette.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Full-Screen Tape Display
                        if (is10KeyFullScreen) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = palette.cardBg),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("📜 Calculation Tape", color = palette.accentPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("Clear", color = palette.accentSecondary, fontSize = 10.sp, modifier = Modifier.clickable { calcController.onClearTape() })
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    calcController.tape.forEach { line ->
                                        Text(line, color = palette.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Display Value
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.cardBg)
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = calcController.displayValue,
                                color = palette.textPrimary,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Memory & Special Function Row
                        if (is10KeyFullScreen) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CalcBtn("MC", palette.buttonBg, palette.textSecondary, Modifier.weight(1f)) { calcController.onMemoryClear() }
                                CalcBtn("MR", palette.buttonBg, palette.textSecondary, Modifier.weight(1f)) { calcController.onMemoryRecall() }
                                CalcBtn("M+", palette.buttonBg, palette.textSecondary, Modifier.weight(1f)) { calcController.onMemoryAdd() }
                                CalcBtn("M-", palette.buttonBg, palette.textSecondary, Modifier.weight(1f)) { calcController.onMemorySubtract() }
                                CalcBtn("%", palette.buttonBg, palette.accentPrimary, Modifier.weight(1f)) { calcController.onPercentage() }
                                CalcBtn("+/-", palette.buttonBg, palette.textSecondary, Modifier.weight(1f)) { calcController.onToggleSign() }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Keypad Grid
                        val row1 = listOf("7", "8", "9", "/", "C")
                        val row2 = listOf("4", "5", "6", "*", "⌫")
                        val row3 = listOf("1", "2", "3", "-", "=")
                        val row4 = if (is10KeyFullScreen) listOf("0", "00", "000", ".", "+") else listOf("0", "00", ".", "+", "Send HID")

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                row1.forEach { k ->
                                    CalcBtn(k, palette.buttonBg, palette.textPrimary, Modifier.weight(1f)) {
                                        when (k) {
                                            "C" -> calcController.onClear()
                                            "/" -> calcController.onOperator("/")
                                            else -> calcController.onDigit(k)
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                row2.forEach { k ->
                                    CalcBtn(k, palette.buttonBg, palette.textPrimary, Modifier.weight(1f)) {
                                        when (k) {
                                            "⌫" -> calcController.onBackspace()
                                            "*" -> calcController.onOperator("*")
                                            else -> calcController.onDigit(k)
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                row3.forEach { k ->
                                    CalcBtn(k, palette.buttonBg, palette.textPrimary, Modifier.weight(1f)) {
                                        when (k) {
                                            "=" -> calcController.onEquals()
                                            "-" -> calcController.onOperator("-")
                                            else -> calcController.onDigit(k)
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                row4.forEach { k ->
                                    CalcBtn(k, if (k == "Send HID") palette.accentPrimary else palette.buttonBg, palette.textPrimary, Modifier.weight(if (k == "Send HID") 1.4f else 1f)) {
                                        when (k) {
                                            "00" -> calcController.onDoubleZero()
                                            "000" -> calcController.onTripleZero()
                                            "." -> calcController.onDigit(".")
                                            "+" -> calcController.onOperator("+")
                                            "Send HID" -> calcController.sendResultToHid()
                                            else -> calcController.onDigit(k)
                                        }
                                    }
                                }
                            }
                        }

                        if (is10KeyFullScreen) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { calcController.sendResultToHid() },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accentPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Send Result via Bluetooth HID", color = palette.bg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Settings Dialog
        if (showSettings) {
            DeckSettingsDialog(
                hosts = hosts,
                bondedDevices = bondedDevices,
                showThirdSlot = showThirdSlotInSettings,
                onToggleThirdSlot = onToggleThirdSlotInSettings,
                onAssignSlotMac = onAssignSlotMac,
                onUpdateThirdSlotMeta = onUpdateThirdSlotMeta,
                onRefreshBondedDevices = onRefreshBondedDevices,
                onDismiss = { showSettings = false }
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeckSettingsDialog(
    hosts: List<HostSlot>,
    bondedDevices: List<BluetoothDevice>,
    showThirdSlot: Boolean,
    onToggleThirdSlot: (Boolean) -> Unit,
    onAssignSlotMac: (slotId: String, macAddress: String?) -> Unit,
    onUpdateThirdSlotMeta: (label: String, os: HostOs) -> Unit,
    onRefreshBondedDevices: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalDeckPalette.current
    val context = LocalContext.current
    var expandedSlotId by remember { mutableStateOf<String?>(null) }

    val slot3 = hosts.firstOrNull { it.id == "slot3" }
    var slot3LabelDraft by remember(slot3?.label) { mutableStateOf(slot3?.label ?: "Host 3") }
    var slot3OsDraft by remember(slot3?.os) { mutableStateOf(slot3?.os ?: HostOs.LINUX) }

    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshBondedDevices()
    }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    if (state == BluetoothDevice.BOND_BONDED) {
                        onRefreshBondedDevices()
                    }
                }
            }
        }
        val filter = android.content.IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = {
            Text(
                text = "KVMeowboard Settings",
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pair New Host & Refresh List Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val discoverIntent = Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                putExtra(android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180)
                            }
                            discoverableLauncher.launch(discoverIntent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.buttonSelected,
                            contentColor = palette.textPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1.4f).height(36.dp)
                    ) {
                        Text("Pair New Host (180s)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onRefreshBondedDevices,
                        border = BorderStroke(1.dp, palette.accentPrimary),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Refresh List", color = palette.accentPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = palette.cardBorder)

                Text(
                    text = "Bonded Bluetooth Host Mapping",
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                val configurableSlots = if (showThirdSlot) hosts.take(3) else hosts.take(2)
                configurableSlots.forEach { slot ->
                    val assignedName = slot.device?.let { dev ->
                        val name = dev.name?.takeIf { it.isNotBlank() } ?: "Unknown Device"
                        "$name (${dev.address})"
                    } ?: "Auto / Unassigned"

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${slot.label} (${slot.os.name})",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.cardBg)
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    expandedSlotId = if (expandedSlotId == slot.id) null else slot.id
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = assignedName,
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        DropdownMenu(
                            expanded = expandedSlotId == slot.id,
                            onDismissRequest = { expandedSlotId = null },
                            modifier = Modifier.background(palette.cardBg)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("Auto / Unassigned", color = palette.textSecondary, fontSize = 12.sp)
                                },
                                onClick = {
                                    onAssignSlotMac(slot.id, null)
                                    expandedSlotId = null
                                }
                            )

                            bondedDevices.forEach { dev ->
                                val devLabel = "${dev.name?.takeIf { it.isNotBlank() } ?: "Unnamed"} • ${dev.address}"
                                DropdownMenuItem(
                                    text = {
                                        Text(devLabel, color = palette.textPrimary, fontSize = 12.sp)
                                    },
                                    onClick = {
                                        onAssignSlotMac(slot.id, dev.address)
                                        expandedSlotId = null
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = palette.cardBorder)

                // Optional 3rd Computer Slot Toggle & Label/OS Config
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable 3rd Computer Slot", color = palette.textPrimary, fontSize = 13.sp)
                    Switch(checked = showThirdSlot, onCheckedChange = onToggleThirdSlot)
                }

                if (showThirdSlot) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = slot3LabelDraft,
                            onValueChange = {
                                slot3LabelDraft = it
                                onUpdateThirdSlotMeta(it.ifBlank { "Host 3" }, slot3OsDraft)
                            },
                            singleLine = true,
                            label = { Text("Slot 3 Name", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val nextOs = HostOs.entries[(HostOs.entries.indexOf(slot3OsDraft) + 1) % HostOs.entries.size]
                                slot3OsDraft = nextOs
                                onUpdateThirdSlotMeta(slot3LabelDraft.ifBlank { "Host 3" }, nextOs)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.buttonBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(90.dp).height(48.dp)
                        ) {
                            Text(slot3OsDraft.name, fontSize = 10.sp, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = palette.cardBorder)

                Text(
                    text = "Inspired by Blek (AppGround IO). Support the original developer or open Blek directly:",
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )

                OutlinedButton(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage("io.appground.blek")
                            ?: Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/AppGround-io/bluetooth-keyboard-and-mouse-support")
                            )
                        context.startActivity(launchIntent)
                    },
                    border = BorderStroke(1.dp, palette.accentPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Open / Support Blek (AppGround IO)",
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = palette.accentPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ModifierKeyButton(
    label: String,
    state: ModifierState,
    palette: DeckPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when (state) {
        ModifierState.OFF -> palette.buttonBg
        ModifierState.ONESHOT -> palette.accentPrimary.copy(alpha = 0.85f)
        ModifierState.LOCKED -> palette.accentSecondary
    }
    val textColor = when (state) {
        ModifierState.OFF -> palette.textSecondary
        ModifierState.ONESHOT -> palette.bg
        ModifierState.LOCKED -> palette.bg
    }
    val badge = when (state) {
        ModifierState.OFF -> ""
        ModifierState.ONESHOT -> " •"
        ModifierState.LOCKED -> " 🔒"
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = modifier
            .height(36.dp)
            .border(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = "$label$badge",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun QuickKeyCap(
    label: String,
    palette: DeckPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = palette.buttonBg),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        modifier = modifier
            .height(38.dp)
            .border(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CalcBtn(
    label: String,
    bg: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(2.dp),
        modifier = modifier.height(36.dp)
    ) {
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
