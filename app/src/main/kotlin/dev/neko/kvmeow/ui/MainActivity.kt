package dev.neko.kvmeow.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import dev.neko.kvmeow.data.DeckPreferences
import dev.neko.kvmeow.hid.HidScancodes
import dev.neko.kvmeow.ime.PerAppImeAndGestureManager
import dev.neko.kvmeow.service.KvmeowBridgeService
import dev.neko.kvmeow.unicode.UnicodeInputEngine
import kotlin.math.abs

/**
 * 🐾 KVMeow Bridge Control Console (appground-io Style Deck) 🐾
 *
 * Vertical Hierarchy:
 * a) Main appground-io deck workspace (Two button tabs for Win 11 / MacBook + optional Slot 3, status)
 * b) Touchpad surface with 3-finger macOS swipe gesture detection & screenshot suppression
 * c) IME panel directly under touchpad (single-tap live input, emoji/sticker bridge toggle)
 * d) Sticky modifier hold strip (Ctrl, Alt/Opt, Cmd/Win, Shift with 3 states) + Arrow key cluster
 * e) Pop-out ^ 10-key numeric pad & expandable full-screen accountant calculator
 */
class MainActivity : ComponentActivity() {

    private lateinit var deckPrefs: DeckPreferences
    private lateinit var imeManager: PerAppImeAndGestureManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshBluetoothState()
    }

    private val connectedHosts = mutableStateListOf<String>()
    private val hostMacs = mutableStateListOf<String>()
    private val bondedHosts = mutableStateListOf<Pair<String, String>>()
    private var activeHostMac by mutableStateOf<String?>(null)
    private var isCaffeineActive by mutableStateOf(false)
    private var hasPermissions by mutableStateOf(false)

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            if (intent.action == KvmeowBridgeService.STATUS_UPDATE) {
                isCaffeineActive = intent.getBooleanExtra(KvmeowBridgeService.EXTRA_CAFFEINE_ACTIVE, false)
                val names = intent.getStringArrayExtra(KvmeowBridgeService.EXTRA_CONNECTED_HOSTS) ?: emptyArray()
                val macs = intent.getStringArrayExtra("host_macs") ?: emptyArray()
                connectedHosts.clear()
                connectedHosts.addAll(names)
                hostMacs.clear()
                hostMacs.addAll(macs)
                activeHostMac = intent.getStringExtra(KvmeowBridgeService.EXTRA_ACTIVE_HOST)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deckPrefs = DeckPreferences(this)
        imeManager = PerAppImeAndGestureManager(this)

        val serviceIntent = Intent(this, KvmeowBridgeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val filter = IntentFilter(KvmeowBridgeService.STATUS_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, filter)
        }

        checkPermissions()
        refreshBluetoothState()
        queryServiceStatus()
        checkBatteryOptimization()

        // Exclude entire screen window from system gesture back/screenshot triggers
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.post {
                val rect = Rect(0, 0, window.decorView.width, window.decorView.height)
                window.decorView.systemGestureExclusionRects = listOf(rect)
            }
        }

        setContent {
            KvmeowDeckScreen(
                deckPrefs = deckPrefs,
                hasPermissions = hasPermissions,
                onRequestPermissions = { requestPermissions() },
                connectedHosts = connectedHosts,
                hostMacs = hostMacs,
                bondedHosts = bondedHosts,
                activeHostMac = activeHostMac,
                isCaffeineActive = isCaffeineActive,
                onSwitchHost = { mac -> switchHost(mac) },
                onSetHostJiggle = { mac, enabled -> setHostJiggle(mac, enabled) },
                onSendDirectText = { text -> sendDirectText(text) },
                onSendUnicodeHex = { text, targetOs -> sendUnicodeHex(text, targetOs) },
                onStageMediaPaste = { uri, mime, fallback, targetOs -> stageMediaPaste(uri, mime, fallback, targetOs) },
                onSendRawKeycode = { scancode, mod -> sendRawKeycode(scancode, mod) },
                onSendMouseReport = { buttons, dx, dy, wheel -> sendMouseReport(buttons, dx, dy, wheel) },
                onMissionControl = { sendMissionControl() },
                onAppExpose = { sendAppExpose() },
                onToggleCaffeine = { toggleCaffeine() },
                onScanRefresh = { refreshBluetoothState() },
                onShowImePicker = { imeManager.showImePicker() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        refreshBluetoothState()
        queryServiceStatus()
        imeManager.onAppForeground(deckPrefs.perAppImeId.ifEmpty { null })
    }

    override fun onPause() {
        imeManager.onAppBackground()
        super.onPause()
    }

    override fun onDestroy() {
        unregisterReceiver(statusReceiver)
        super.onDestroy()
    }

    private fun checkPermissions() {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        hasPermissions = required.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        permissionLauncher.launch(perms)
    }

    @SuppressLint("MissingPermission")
    private fun refreshBluetoothState() {
        checkPermissions()
        if (!hasPermissions) return
        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
        val bonded = adapter?.bondedDevices ?: emptySet()
        bondedHosts.clear()
        for (dev in bonded) {
            val name = try { dev.name ?: "Unknown Host" } catch (_: SecurityException) { "Unknown Host" }
            bondedHosts.add(name to dev.address)
        }
    }

    private fun switchHost(mac: String) {
        val intent = Intent(KvmeowBridgeService.ACTION_FLIP_HOST).apply {
            putExtra(KvmeowBridgeService.EXTRA_HOST_MAC, mac)
        }
        sendBroadcast(intent)
        activeHostMac = mac
    }

    private fun sendDirectText(text: String) {
        val intent = Intent(KvmeowBridgeService.ACTION_SEND_TEXT).apply {
            putExtra(KvmeowBridgeService.EXTRA_TEXT, text)
        }
        sendBroadcast(intent)
    }

    private fun sendUnicodeHex(text: String, targetOs: UnicodeInputEngine.TargetOs) {
        val intent = Intent(KvmeowBridgeService.ACTION_SEND_UNICODE_HEX).apply {
            putExtra(KvmeowBridgeService.EXTRA_TEXT, text)
            putExtra(KvmeowBridgeService.EXTRA_TARGET_OS, targetOs.name)
        }
        sendBroadcast(intent)
    }

    private fun stageMediaPaste(uri: String?, mimeType: String, fallbackText: String, targetOs: UnicodeInputEngine.TargetOs) {
        val intent = Intent(KvmeowBridgeService.ACTION_STAGE_MEDIA_PASTE).apply {
            putExtra(KvmeowBridgeService.EXTRA_MEDIA_URI, uri)
            putExtra(KvmeowBridgeService.EXTRA_MIME_TYPE, mimeType)
            putExtra(KvmeowBridgeService.EXTRA_FALLBACK_TEXT, fallbackText)
            putExtra(KvmeowBridgeService.EXTRA_TARGET_OS, targetOs.name)
        }
        sendBroadcast(intent)
    }

    private fun sendRawKeycode(scancode: Byte, modifier: Byte = 0) {
        val intent = Intent(KvmeowBridgeService.ACTION_SEND_RAW_KEYCODE).apply {
            putExtra(KvmeowBridgeService.EXTRA_SCANCODE, scancode)
            putExtra(KvmeowBridgeService.EXTRA_MODIFIER, modifier)
        }
        sendBroadcast(intent)
    }

    private fun sendMouseReport(buttons: Byte, dx: Byte, dy: Byte, wheel: Byte = 0) {
        val intent = Intent(KvmeowBridgeService.ACTION_SEND_MOUSE).apply {
            putExtra(KvmeowBridgeService.EXTRA_BUTTONS, buttons)
            putExtra(KvmeowBridgeService.EXTRA_DX, dx)
            putExtra(KvmeowBridgeService.EXTRA_DY, dy)
            putExtra(KvmeowBridgeService.EXTRA_WHEEL, wheel)
        }
        sendBroadcast(intent)
    }

    private fun sendMissionControl() {
        sendBroadcast(Intent(KvmeowBridgeService.ACTION_MISSION_CONTROL))
    }

    private fun sendAppExpose() {
        sendBroadcast(Intent(KvmeowBridgeService.ACTION_APP_EXPOSE))
    }

    private fun setHostJiggle(mac: String, enabled: Boolean) {
        val intent = Intent(KvmeowBridgeService.ACTION_SET_HOST_JIGGLE).apply {
            putExtra(KvmeowBridgeService.EXTRA_HOST_MAC, mac)
            putExtra(KvmeowBridgeService.EXTRA_ENABLED, enabled)
        }
        sendBroadcast(intent)
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    private fun toggleCaffeine() {
        val intent = Intent(KvmeowBridgeService.ACTION_TOGGLE_CAFFEINE)
        sendBroadcast(intent)
    }

    private fun queryServiceStatus() {
        val intent = Intent(KvmeowBridgeService.ACTION_QUERY_STATUS)
        sendBroadcast(intent)
    }
}

// 🎨 Kawaiianism v3.1 Palette Tokens
private val DeepIndigo = Color(0xFF2E1A47)
private val SoftLavender = Color(0xFFE6E6FA)
private val DarkSlate = Color(0xFF1E1E2E)
private val AmberGold = Color(0xFFFFFBEB)
private val CardPurple = Color(0xFF3B225C)
private val AccentLilac = Color(0xFFA78BFA)
private val AccentGreen = Color(0xFF98FB98)
private val KawaiiPink = Color(0xFFFFAFCC)
private val ActiveBlue = Color(0xFF64B5F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KvmeowDeckScreen(
    deckPrefs: DeckPreferences,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    connectedHosts: List<String>,
    hostMacs: List<String>,
    bondedHosts: List<Pair<String, String>>,
    activeHostMac: String?,
    isCaffeineActive: Boolean,
    onSwitchHost: (String) -> Unit,
    onSetHostJiggle: (String, Boolean) -> Unit,
    onSendDirectText: (String) -> Unit,
    onSendUnicodeHex: (String, UnicodeInputEngine.TargetOs) -> Unit,
    onStageMediaPaste: (String?, String, String, UnicodeInputEngine.TargetOs) -> Unit,
    onSendRawKeycode: (Byte, Byte) -> Unit,
    onSendMouseReport: (Byte, Byte, Byte, Byte) -> Unit,
    onMissionControl: () -> Unit,
    onAppExpose: () -> Unit,
    onToggleCaffeine: () -> Unit,
    onScanRefresh: () -> Unit,
    onShowImePicker: () -> Unit
) {
    var currentThemeEnum by remember {
        mutableStateOf(
            try {
                AppTheme.valueOf(deckPrefs.appTheme)
            } catch (_: Exception) {
                AppTheme.DARK_KAWAII
            }
        )
    }
    val theme = ThemeManager.getPalette(currentThemeEnum)

    var directInputText by remember { mutableStateOf("") }
    var stickyModifiers by remember { mutableStateOf(StickyModifiers()) }
    var showNumpadModal by remember { mutableStateOf(deckPrefs.isNumpadOpen) }
    var isNumpadFullScreen by remember { mutableStateOf(deckPrefs.isNumpadFullscreen) }
    var showEmojiSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var slot1Jiggle by remember { mutableStateOf(deckPrefs.slot1JiggleEnabled) }
    var slot2Jiggle by remember { mutableStateOf(deckPrefs.slot2JiggleEnabled) }
    var slot3Jiggle by remember { mutableStateOf(deckPrefs.slot3JiggleEnabled) }

    // Active target OS derived from active tab
    val currentTargetOs = when (deckPrefs.targetOs) {
        "WINDOWS" -> UnicodeInputEngine.TargetOs.WINDOWS
        "LINUX" -> UnicodeInputEngine.TargetOs.LINUX
        else -> UnicodeInputEngine.TargetOs.MACOS
    }

    // OS-aware modifier key labels
    val isMac = currentTargetOs == UnicodeInputEngine.TargetOs.MACOS
    val cmdWinLabel = if (isMac) "Cmd ⌘" else "Win ⊞"
    val altOptLabel = if (isMac) "Opt ⌥" else "Alt"

    // Helper to send a key with sticky modifier hold applied
    fun sendKeyWithModifiers(scancode: Byte) {
        val modMask = stickyModifiers.toBitmask()
        onSendRawKeycode(scancode, modMask)
        stickyModifiers = stickyModifiers.consumeLatches()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ==========================================
        // 1. MAIN APPGROUND-IO WORKSPACE HEADER
        // ==========================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, theme.cardBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🐾 KVMeow",
                            color = theme.primaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[appground]",
                            color = theme.accentPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 3-Theme Switcher Button (Tron -> Dark Kawaii -> Bright Kawaii)
                        Button(
                            onClick = {
                                val nextTheme = when (currentThemeEnum) {
                                    AppTheme.TRON -> AppTheme.DARK_KAWAII
                                    AppTheme.DARK_KAWAII -> AppTheme.BRIGHT_KAWAII
                                    AppTheme.BRIGHT_KAWAII -> AppTheme.TRON
                                }
                                currentThemeEnum = nextTheme
                                deckPrefs.appTheme = nextTheme.name
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.buttonBackground),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).border(1.dp, theme.cardBorder, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = when (currentThemeEnum) {
                                    AppTheme.TRON -> "⚡ Tron"
                                    AppTheme.DARK_KAWAII -> "🌙 Dark"
                                    AppTheme.BRIGHT_KAWAII -> "🌸 Bright"
                                },
                                color = theme.accentPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Global Caffeine Jitter indicator
                        Text(
                            text = if (isCaffeineActive) "☕ JITTER" else "☕ IDLE",
                            color = if (isCaffeineActive) theme.statusActive else theme.secondaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(theme.buttonBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, theme.cardBorder, RoundedCornerShape(8.dp))
                                .clickable { onToggleCaffeine() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                        IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = theme.secondaryText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ADAPTIVE COMPUTER BUTTON TABS + INDEPENDENT PER-HOST JIGGLE TOGGLE BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Win 11 Tab Column
                    Column(modifier = Modifier.weight(1f)) {
                        ComputerTabButton(
                            label = "🪟 ${deckPrefs.slot1Name}",
                            isSelected = deckPrefs.activeSlot == 1,
                            theme = theme,
                            onClick = {
                                deckPrefs.activeSlot = 1
                                deckPrefs.targetOs = "WINDOWS"
                                stickyModifiers = stickyModifiers.releaseAll()
                                if (deckPrefs.slot1Mac.isNotEmpty()) onSwitchHost(deckPrefs.slot1Mac)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        PerHostJiggleButton(
                            isActive = slot1Jiggle,
                            theme = theme,
                            onClick = {
                                slot1Jiggle = !slot1Jiggle
                                deckPrefs.slot1JiggleEnabled = slot1Jiggle
                                if (deckPrefs.slot1Mac.isNotEmpty()) {
                                    onSetHostJiggle(deckPrefs.slot1Mac, slot1Jiggle)
                                } else {
                                    onToggleCaffeine()
                                }
                            }
                        )
                    }

                    // MacBook Tab Column
                    Column(modifier = Modifier.weight(1f)) {
                        ComputerTabButton(
                            label = "🍎 ${deckPrefs.slot2Name}",
                            isSelected = deckPrefs.activeSlot == 2,
                            theme = theme,
                            onClick = {
                                deckPrefs.activeSlot = 2
                                deckPrefs.targetOs = "MACOS"
                                stickyModifiers = stickyModifiers.releaseAll()
                                if (deckPrefs.slot2Mac.isNotEmpty()) onSwitchHost(deckPrefs.slot2Mac)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        PerHostJiggleButton(
                            isActive = slot2Jiggle,
                            theme = theme,
                            onClick = {
                                slot2Jiggle = !slot2Jiggle
                                deckPrefs.slot2JiggleEnabled = slot2Jiggle
                                if (deckPrefs.slot2Mac.isNotEmpty()) {
                                    onSetHostJiggle(deckPrefs.slot2Mac, slot2Jiggle)
                                } else {
                                    onToggleCaffeine()
                                }
                            }
                        )
                    }

                    // Optional Slot 3 Tab Column
                    if (deckPrefs.slot3Enabled) {
                        Column(modifier = Modifier.weight(1f)) {
                            ComputerTabButton(
                                label = "🐧 ${deckPrefs.slot3Name}",
                                isSelected = deckPrefs.activeSlot == 3,
                                theme = theme,
                                onClick = {
                                    deckPrefs.activeSlot = 3
                                    deckPrefs.targetOs = "LINUX"
                                    stickyModifiers = stickyModifiers.releaseAll()
                                    if (deckPrefs.slot3Mac.isNotEmpty()) onSwitchHost(deckPrefs.slot3Mac)
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            PerHostJiggleButton(
                                isActive = slot3Jiggle,
                                theme = theme,
                                onClick = {
                                    slot3Jiggle = !slot3Jiggle
                                    deckPrefs.slot3JiggleEnabled = slot3Jiggle
                                    if (deckPrefs.slot3Mac.isNotEmpty()) {
                                        onSetHostJiggle(deckPrefs.slot3Mac, slot3Jiggle)
                                    } else {
                                        onToggleCaffeine()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==========================================
        // 2. TOUCHPAD / MOUSEPAD SURFACE
        // ==========================================
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, theme.cardBorder, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🖱️ Touchpad (3-Finger Swipe = Mission Control)",
                        color = theme.primaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap L • Hold R",
                        color = theme.accentPrimary,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Multi-Touch Surface tracking 3-finger swipes
                var startY by remember { mutableStateOf(0f) }
                var isTracking3Finger by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(theme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, theme.cardBorder, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val count = event.changes.size

                                    if (count == 3) {
                                        // 3-Finger Gesture Interception for macOS Mission Control
                                        val change = event.changes[0]
                                        if (!isTracking3Finger) {
                                             isTracking3Finger = true
                                             startY = change.position.y
                                        } else {
                                             val dy = change.position.y - startY
                                             if (abs(dy) > 120f) {
                                                 if (dy < 0) {
                                                     onMissionControl() // Swipe Up = Mission Control
                                                 } else {
                                                     onAppExpose() // Swipe Down = App Exposé
                                                 }
                                                 isTracking3Finger = false
                                                 startY = change.position.y
                                             }
                                        }
                                        event.changes.forEach { it.consume() }
                                    } else {
                                        isTracking3Finger = false
                                        // Standard cursor steering
                                        if (count == 1) {
                                            val change = event.changes[0]
                                            if (change.pressed && event.type == PointerEventType.Move) {
                                                val prev = change.previousPosition
                                                val curr = change.position
                                                val dx = (curr.x - prev.x).coerceIn(-127f, 127f).toInt().toByte()
                                                val dy = (curr.y - prev.y).coerceIn(-127f, 127f).toInt().toByte()
                                                onSendMouseReport(0, dx, dy, 0)
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🐾 Swipe to steer cursor\n3-Finger Swipe Up: 🚀 Mission Control | Down: Exposé",
                            color = theme.secondaryText.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Mouse Buttons (Left, Wheel, Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onSendMouseReport(1, 0, 0, 0) },
                        modifier = Modifier.weight(2f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.buttonBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Left Click", color = theme.primaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onSendMouseReport(4, 0, 0, 0) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.buttonBackground.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Wheel", color = theme.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onSendMouseReport(2, 0, 0, 0) },
                        modifier = Modifier.weight(2f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.buttonBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Right Click", color = theme.primaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==========================================
        // 3. IME PANEL (Pops up directly under Touchpad)
        // ==========================================
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, theme.cardBorder, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⌨️ IME Input Bar",
                        color = theme.primaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Emoji / Sticker Bridge Button
                        Button(
                            onClick = { showEmojiSheet = !showEmojiSheet },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.buttonBackground),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("😀 Stickers", color = theme.accentSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Instant Mission Control fallback button
                        Button(
                            onClick = { onMissionControl() },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.buttonBackground),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("🚀 MC", color = theme.statusActive, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // ^ 10-Key Pop-Out Button
                        Button(
                            onClick = {
                                showNumpadModal = true
                                deckPrefs.isNumpadOpen = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("^ 10-KEY", color = theme.primaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = directInputText,
                    onValueChange = { newText ->
                        if (newText.length > directInputText.length) {
                            val added = newText.substring(directInputText.length)
                            if (deckPrefs.emojiMode == "HEX_CHORD") {
                                onSendUnicodeHex(added, currentTargetOs)
                            } else {
                                onSendDirectText(added)
                            }
                        } else if (newText.length < directInputText.length) {
                            sendKeyWithModifiers(HidScancodes.KEY_BACKSPACE)
                        }
                        directInputText = newText
                        if (directInputText.length > 50) directInputText = ""
                    },
                    placeholder = {
                        Text("Tap here to pop up IME under touchpad...", color = theme.secondaryText.copy(alpha = 0.7f), fontSize = 12.sp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.surface, RoundedCornerShape(10.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.primaryText,
                        unfocusedTextColor = theme.primaryText,
                        focusedBorderColor = theme.accentPrimary,
                        unfocusedBorderColor = theme.cardBorder
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                if (showEmojiSheet) {
                    Spacer(modifier = Modifier.height(8.dp))
                    EmojiStickerPicker(
                        onSendEmoji = { emoji ->
                            if (deckPrefs.emojiMode == "HEX_CHORD") {
                                onSendUnicodeHex(emoji, currentTargetOs)
                            } else {
                                onSendDirectText(emoji)
                            }
                        },
                        onSendStickerMedia = { uri, mime, fallback ->
                            onStageMediaPaste(uri, mime, fallback, currentTargetOs)
                        },
                        onClose = { showEmojiSheet = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==========================================
        // 4. STICKY MODIFIERS HOLD STRIP & ARROW KEY CLUSTER
        // ==========================================
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, theme.cardBorder, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Modifier Hold Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StickyModifierKey(
                        label = "Ctrl",
                        state = stickyModifiers.ctrl,
                        theme = theme,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            stickyModifiers = stickyModifiers.copy(ctrl = cycleModifierState(stickyModifiers.ctrl))
                        }
                    )
                    StickyModifierKey(
                        label = altOptLabel,
                        state = stickyModifiers.altOpt,
                        theme = theme,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            stickyModifiers = stickyModifiers.copy(altOpt = cycleModifierState(stickyModifiers.altOpt))
                        }
                    )
                    StickyModifierKey(
                        label = cmdWinLabel,
                        state = stickyModifiers.cmdWin,
                        theme = theme,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            stickyModifiers = stickyModifiers.copy(cmdWin = cycleModifierState(stickyModifiers.cmdWin))
                        }
                    )
                    StickyModifierKey(
                        label = "Shift ⇧",
                        state = stickyModifiers.shift,
                        theme = theme,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            stickyModifiers = stickyModifiers.copy(shift = cycleModifierState(stickyModifiers.shift))
                        }
                    )
                    // Global Release (Rel) button
                    Button(
                        onClick = { stickyModifiers = stickyModifiers.releaseAll() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (stickyModifiers.hasAnyActive()) theme.accentSecondary else theme.buttonBackground),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Rel", color = theme.primaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Arrow Key Cluster directly beneath IME with quick Tab and Esc keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickKeyButton(label = "Esc", theme = theme, modifier = Modifier.weight(1f)) {
                        sendKeyWithModifiers(HidScancodes.KEY_ESCAPE)
                    }
                    QuickKeyButton(label = "Tab", theme = theme, modifier = Modifier.weight(1f)) {
                        sendKeyWithModifiers(HidScancodes.KEY_TAB)
                    }
                    QuickKeyButton(label = "←", theme = theme, modifier = Modifier.weight(1f)) {
                        sendKeyWithModifiers(HidScancodes.KEY_LEFT)
                    }
                    QuickKeyButton(label = "↑", theme = theme, modifier = Modifier.weight(1f)) {
                        sendKeyWithModifiers(HidScancodes.KEY_UP)
                    }
                    QuickKeyButton(label = "↓", theme = theme, modifier = Modifier.weight(1f)) {
                        sendKeyWithModifiers(HidScancodes.KEY_DOWN)
                    }
                    QuickKeyButton(label = "→", theme = theme, modifier = Modifier.weight(1f)) {
                        sendKeyWithModifiers(HidScancodes.KEY_RIGHT)
                    }
                    QuickKeyButton(label = "⏎", theme = theme, modifier = Modifier.weight(1f)) {
                        sendKeyWithModifiers(HidScancodes.KEY_ENTER)
                    }
                }
            }
        }
    }

    // ==========================================
    // 5. ^ 10-KEY / FULL-SCREEN ACCOUNTANT CALC MODAL
    // ==========================================
    if (showNumpadModal) {
        Dialog(
            onDismissRequest = {
                showNumpadModal = false
                deckPrefs.isNumpadOpen = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = !isNumpadFullScreen)
        ) {
            FullAccountantCalculatorView(
                isFullScreen = isNumpadFullScreen,
                onToggleFullScreen = {
                    isNumpadFullScreen = !isNumpadFullScreen
                    deckPrefs.isNumpadFullscreen = isNumpadFullScreen
                },
                onClose = {
                    showNumpadModal = false
                    deckPrefs.isNumpadOpen = false
                },
                onSendScancode = { code -> sendKeyWithModifiers(code) },
                onSendText = { text -> onSendDirectText(text) },
                savedTape = deckPrefs.calcTape,
                onSaveTape = { tape -> deckPrefs.calcTape = tape },
                savedMemory = deckPrefs.calcMemory,
                onSaveMemory = { mem -> deckPrefs.calcMemory = mem }
            )
        }
    }

    // ==========================================
    // SETTINGS & CONFIGURATION DIALOG
    // ==========================================
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                modifier = Modifier.fillMaxWidth().padding(10.dp).border(2.dp, theme.cardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚙️ Deck Preferences", color = theme.primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Theme Selector:", color = theme.secondaryText, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AppTheme.values().forEach { t ->
                                val isSelected = currentThemeEnum == t
                                Button(
                                    onClick = {
                                        currentThemeEnum = t
                                        deckPrefs.appTheme = t.name
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) theme.accentPrimary else theme.buttonBackground
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = when(t) {
                                            AppTheme.TRON -> "Tron"
                                            AppTheme.DARK_KAWAII -> "Dark"
                                            AppTheme.BRIGHT_KAWAII -> "Bright"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) theme.primaryText else theme.secondaryText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3rd Computer Slot (Linux/Work):", color = theme.secondaryText, fontSize = 12.sp)
                        Switch(
                            checked = deckPrefs.slot3Enabled,
                            onCheckedChange = { deckPrefs.slot3Enabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Emoji Encoding Mode:", color = theme.secondaryText, fontSize = 12.sp)
                        Button(
                            onClick = {
                                deckPrefs.emojiMode = if (deckPrefs.emojiMode == "HEX_CHORD") "CLIPBOARD" else "HEX_CHORD"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.buttonBackground)
                        ) {
                            Text(deckPrefs.emojiMode, color = theme.accentPrimary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onShowImePicker() },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.buttonBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select System / Per-App IME", color = theme.primaryText, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showSettingsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = theme.primaryText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun cycleModifierState(current: ModifierLockState): ModifierLockState {
    return when (current) {
        ModifierLockState.OFF -> ModifierLockState.LATCHED
        ModifierLockState.LATCHED -> ModifierLockState.LOCKED
        ModifierLockState.LOCKED -> ModifierLockState.OFF
    }
}

@Composable
fun StickyModifierKey(
    label: String,
    state: ModifierLockState,
    theme: ThemePalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when (state) {
        ModifierLockState.OFF -> theme.buttonBackground
        ModifierLockState.LATCHED -> theme.accentPrimary.copy(alpha = 0.8f)
        ModifierLockState.LOCKED -> theme.accentSecondary
    }
    val textColor = when (state) {
        ModifierLockState.OFF -> theme.secondaryText
        ModifierLockState.LATCHED -> theme.primaryText
        ModifierLockState.LOCKED -> theme.primaryText
    }
    val badge = when (state) {
        ModifierLockState.OFF -> ""
        ModifierLockState.LATCHED -> " •"
        ModifierLockState.LOCKED -> " 🔒"
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = modifier.height(36.dp).border(1.dp, theme.cardBorder, RoundedCornerShape(8.dp))
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
fun ComputerTabButton(
    label: String,
    isSelected: Boolean,
    theme: ThemePalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) theme.buttonSelected else theme.buttonBackground),
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) theme.accentPrimary else theme.cardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = theme.primaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PerHostJiggleButton(
    isActive: Boolean,
    theme: ThemePalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) theme.statusActive else theme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) theme.primaryText else theme.cardBorder,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Text(
            text = if (isActive) "☕ Jiggle ON" else "☕ Jiggle OFF",
            color = if (isActive) theme.background else theme.secondaryText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun QuickKeyButton(
    label: String,
    theme: ThemePalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = theme.buttonBackground),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        modifier = modifier.height(38.dp).border(1.dp, theme.cardBorder, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = label,
            color = theme.primaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
