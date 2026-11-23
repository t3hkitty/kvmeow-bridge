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
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import dev.neko.kvmeow.hid.HidScancodes
import dev.neko.kvmeow.service.KvmeowBridgeService

/**
 * 🐾 KVMeow Bridge Control Console 🐾
 * High-Density Rounded Plumpitude UI for 1-tap Recent Host switching,
 * direct live keyboard typing, and instant Bluetooth permission handling.
 */
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshBluetoothState()
    }

    private val connectedHosts = mutableStateListOf<String>()
    private val hostMacs = mutableStateListOf<String>()
    private val bondedHosts = mutableStateListOf<Pair<String, String>>() // name to mac
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

        setContent {
            KvmeowConsoleScreen(
                hasPermissions = hasPermissions,
                onRequestPermissions = { requestPermissions() },
                connectedHosts = connectedHosts,
                hostMacs = hostMacs,
                bondedHosts = bondedHosts,
                activeHostMac = activeHostMac,
                isCaffeineActive = isCaffeineActive,
                onSwitchHost = { mac -> switchHost(mac) },
                onSendDirectText = { text -> sendDirectText(text) },
                onSendRawKeycode = { scancode, mod -> sendRawKeycode(scancode, mod) },
                onSendMouseReport = { buttons, dx, dy, wheel -> sendMouseReport(buttons, dx, dy, wheel) },
                onToggleCaffeine = { toggleCaffeine() },
                onScanRefresh = { refreshBluetoothState() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        refreshBluetoothState()
        queryServiceStatus()
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

    private fun toggleCaffeine() {
        val intent = Intent(KvmeowBridgeService.ACTION_TOGGLE_CAFFEINE)
        sendBroadcast(intent)
    }

    private fun queryServiceStatus() {
        val intent = Intent(KvmeowBridgeService.ACTION_QUERY_STATUS)
        sendBroadcast(intent)
    }
}

// 🎨 Kawaiianism v3.1-Plump Palette Tokens
private val DeepIndigo = Color(0xFF2E1A47)
private val SoftLavender = Color(0xFFE6E6FA)
private val DarkSlate = Color(0xFF1E1E2E)
private val AmberGold = Color(0xFFFFFBEB)
private val CardPurple = Color(0xFF3B225C)
private val AccentLilac = Color(0xFFA78BFA)
private val AccentGreen = Color(0xFF98FB98)
private val KawaiiPink = Color(0xFFFFAFCC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KvmeowConsoleScreen(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    connectedHosts: List<String>,
    hostMacs: List<String>,
    bondedHosts: List<Pair<String, String>>,
    activeHostMac: String?,
    isCaffeineActive: Boolean,
    onSwitchHost: (String) -> Unit,
    onSendDirectText: (String) -> Unit,
    onSendRawKeycode: (Byte, Byte) -> Unit,
    onSendMouseReport: (Byte, Byte, Byte, Byte) -> Unit,
    onToggleCaffeine: () -> Unit,
    onScanRefresh: () -> Unit
) {
    var directInputText by remember { mutableStateOf("") }
    var showNumpadModal by remember { mutableStateOf(false) }
    var isNumpadFullScreen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlate)
            .statusBarsPadding()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 🌸 Console Header Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CardPurple, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🐾 KVMeow Bridge",
                        color = AmberGold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isCaffeineActive) "☕ JITTER ON" else "☕ JITTER OFF",
                            color = if (isCaffeineActive) AccentGreen else SoftLavender,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(CardPurple, RoundedCornerShape(12.dp))
                                .clickable { onToggleCaffeine() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Text(
                    text = "✨ [status: ready] • Bluetooth HID Peripheral Daemon",
                    color = SoftLavender,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ⚠️ Permission Alert Banner (if missing)
        if (!hasPermissions) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KawaiiPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestPermissions() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚨", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bluetooth Permissions Required",
                            color = DarkSlate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Tap to grant BLUETOOTH_CONNECT and SCAN",
                            color = DarkSlate,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant", color = AmberGold, fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 🚀 1-Tap Recent Device Switcher Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ HOST SWITCHER",
                color = AccentLilac,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            IconButton(
                onClick = onScanRefresh,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Scan Refresh",
                    tint = SoftLavender
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HostChip(
                label = "📱 Local Android",
                subLabel = "Local IME",
                isSelected = activeHostMac == null,
                onClick = { onSwitchHost("") }
            )

            for ((name, mac) in bondedHosts) {
                val isSelected = activeHostMac == mac
                val isConnected = hostMacs.contains(mac)
                val icon = when {
                    name.contains("Mac", ignoreCase = true) -> "🍎"
                    name.contains("Win", ignoreCase = true) || name.contains("PC", ignoreCase = true) -> "🪟"
                    name.contains("Linux", ignoreCase = true) -> "🐧"
                    else -> "💻"
                }
                HostChip(
                    label = "$icon $name",
                    subLabel = if (isConnected) "Connected ✨" else mac.takeLast(5),
                    isSelected = isSelected,
                    onClick = { onSwitchHost(mac) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🖱️ AppGround-Style Touchpad Area
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CardPurple, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🖱️ Touchpad (Swipe cursor • Tap L-Click)",
                        color = AmberGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "2-Finger: R-Click",
                        color = AccentLilac,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Gesture Touchpad Surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(DarkSlate, RoundedCornerShape(14.dp))
                        .border(1.dp, CardPurple, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x.coerceIn(-127f, 127f).toInt().toByte()
                                val dy = dragAmount.y.coerceIn(-127f, 127f).toInt().toByte()
                                onSendMouseReport(0, dx, dy, 0)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    onSendMouseReport(1, 0, 0, 0) // Left click
                                },
                                onDoubleTap = {
                                    onSendMouseReport(1, 0, 0, 0)
                                    onSendMouseReport(1, 0, 0, 0)
                                },
                                onLongPress = {
                                    onSendMouseReport(2, 0, 0, 0) // Right click
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🐾 Drag to steer mouse\nTap to click • Long-press right click",
                        color = SoftLavender.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mouse Click Bar (Left, Middle/Scroll, Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSendMouseReport(1, 0, 0, 0) },
                        modifier = Modifier.weight(2f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Left Click", color = SoftLavender, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onSendMouseReport(4, 0, 0, 0) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardPurple.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Wheel", color = SoftLavender, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onSendMouseReport(2, 0, 0, 0) },
                        modifier = Modifier.weight(2f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Right Click", color = SoftLavender, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ⌨️ IME Pops Up Under Touchpad Pad
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CardPurple, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⌨️ Live Keyboard / IME Input",
                        color = AmberGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    // 10-Key Pop-Out Button ^
                    Button(
                        onClick = { showNumpadModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = KawaiiPink),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "^ 10-KEY",
                            color = DarkSlate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = directInputText,
                    onValueChange = { newText ->
                        if (newText.length > directInputText.length) {
                            val added = newText.substring(directInputText.length)
                            onSendDirectText(added)
                        } else if (newText.length < directInputText.length) {
                            onSendRawKeycode(HidScancodes.KEY_BACKSPACE, HidScancodes.MODIFIER_NONE)
                        }
                        directInputText = newText
                        if (directInputText.length > 60) {
                            directInputText = ""
                        }
                    },
                    placeholder = {
                        Text("Tap here to pop up IME keyboard...", color = AccentLilac, fontSize = 12.sp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardPurple, RoundedCornerShape(12.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AmberGold,
                        unfocusedTextColor = SoftLavender,
                        focusedBorderColor = KawaiiPink,
                        unfocusedBorderColor = CardPurple
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 🎯 Arrow Keys Strip Right Under IME
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickKeyButton(
                        label = "Esc",
                        modifier = Modifier.weight(1f),
                        onClick = { onSendRawKeycode(HidScancodes.KEY_ESCAPE, HidScancodes.MODIFIER_NONE) }
                    )
                    QuickKeyButton(
                        label = "Tab",
                        modifier = Modifier.weight(1f),
                        onClick = { onSendRawKeycode(HidScancodes.KEY_TAB, HidScancodes.MODIFIER_NONE) }
                    )
                    QuickKeyButton(
                        label = "◀",
                        modifier = Modifier.weight(1f),
                        onClick = { onSendRawKeycode(HidScancodes.KEY_LEFT, HidScancodes.MODIFIER_NONE) }
                    )
                    QuickKeyButton(
                        label = "▲",
                        modifier = Modifier.weight(1f),
                        onClick = { onSendRawKeycode(HidScancodes.KEY_UP, HidScancodes.MODIFIER_NONE) }
                    )
                    QuickKeyButton(
                        label = "▼",
                        modifier = Modifier.weight(1f),
                        onClick = { onSendRawKeycode(HidScancodes.KEY_DOWN, HidScancodes.MODIFIER_NONE) }
                    )
                    QuickKeyButton(
                        label = "▶",
                        modifier = Modifier.weight(1f),
                        onClick = { onSendRawKeycode(HidScancodes.KEY_RIGHT, HidScancodes.MODIFIER_NONE) }
                    )
                    QuickKeyButton(
                        label = "⏎",
                        modifier = Modifier.weight(1f),
                        onClick = { onSendRawKeycode(HidScancodes.KEY_ENTER, HidScancodes.MODIFIER_NONE) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 💜 Status & Host Ledger Info
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardPurple),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "💜 Host Ledger & Telemetry",
                    color = SoftLavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Active Target: ${activeHostMac ?: "No active target (Local)"}",
                    color = AmberGold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Connected Hosts: ${if (connectedHosts.isEmpty()) "None (idle/advertising)" else connectedHosts.joinToString(", ")}",
                    color = SoftLavender,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    // 🧮 Aggressive 10-Key Pop-Out Accountant Calculator Dialog
    if (showNumpadModal) {
        Dialog(
            onDismissRequest = {
                showNumpadModal = false
                isNumpadFullScreen = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = !isNumpadFullScreen
            )
        ) {
            Card(
                shape = RoundedCornerShape(if (isNumpadFullScreen) 0.dp else 24.dp),
                colors = CardDefaults.cardColors(containerColor = DeepIndigo),
                modifier = (if (isNumpadFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .border(2.dp, CardPurple, RoundedCornerShape(if (isNumpadFullScreen) 0.dp else 24.dp))
            ) {
                AccountantTenKeyContent(
                    isFullScreen = isNumpadFullScreen,
                    onToggleFullScreen = { isNumpadFullScreen = !isNumpadFullScreen },
                    onClose = {
                        showNumpadModal = false
                        isNumpadFullScreen = false
                    },
                    onSendScancode = { code -> onSendRawKeycode(code, HidScancodes.MODIFIER_NONE) },
                    onSendChar = { ch -> onSendDirectText(ch.toString()) }
                )
            }
        }
    }
}

@Composable
fun AccountantTenKeyContent(
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onClose: () -> Unit,
    onSendScancode: (Byte) -> Unit,
    onSendChar: (Char) -> Unit
) {
    var calcDisplay by remember { mutableStateOf("0") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Modal Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🧮", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFullScreen) "Full Accountant Calculator" else "Aggressive 10-Key Pad",
                    color = AmberGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Row {
                IconButton(onClick = onToggleFullScreen) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Fullscreen",
                        tint = SoftLavender
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SoftLavender
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Calculator Tape / Display Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSlate, RoundedCornerShape(12.dp))
                .border(1.dp, CardPurple, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = calcDisplay,
                color = AmberGold,
                fontSize = if (isFullScreen) 32.sp else 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 10-Key Matrix with aggressive numpad HID scancodes
        val keyRows = listOf(
            listOf(
                Triple("Num", HidScancodes.KEY_NUM_LOCK, CardPurple),
                Triple("/", HidScancodes.KEY_KP_SLASH, CardPurple),
                Triple("*", HidScancodes.KEY_KP_ASTERISK, CardPurple),
                Triple("-", HidScancodes.KEY_KP_MINUS, CardPurple)
            ),
            listOf(
                Triple("7", HidScancodes.KEY_KP_7, DeepIndigo),
                Triple("8", HidScancodes.KEY_KP_8, DeepIndigo),
                Triple("9", HidScancodes.KEY_KP_9, DeepIndigo),
                Triple("+", HidScancodes.KEY_KP_PLUS, CardPurple)
            ),
            listOf(
                Triple("4", HidScancodes.KEY_KP_4, DeepIndigo),
                Triple("5", HidScancodes.KEY_KP_5, DeepIndigo),
                Triple("6", HidScancodes.KEY_KP_6, DeepIndigo),
                Triple("BS", HidScancodes.KEY_BACKSPACE, KawaiiPink.copy(alpha = 0.6f))
            ),
            listOf(
                Triple("1", HidScancodes.KEY_KP_1, DeepIndigo),
                Triple("2", HidScancodes.KEY_KP_2, DeepIndigo),
                Triple("3", HidScancodes.KEY_KP_3, DeepIndigo),
                Triple("⏎", HidScancodes.KEY_KP_ENTER, AccentLilac)
            ),
            listOf(
                Triple("0", HidScancodes.KEY_KP_0, DeepIndigo),
                Triple("00", HidScancodes.KEY_NONE, DeepIndigo),
                Triple(".", HidScancodes.KEY_KP_DOT, DeepIndigo),
                Triple("CLR", HidScancodes.KEY_ESCAPE, KawaiiPink)
            )
        )

        for (row in keyRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for ((label, scancode, color) in row) {
                    Button(
                        onClick = {
                            if (label == "CLR") {
                                calcDisplay = "0"
                                onSendScancode(scancode)
                            } else if (label == "00") {
                                calcDisplay = if (calcDisplay == "0") "0" else calcDisplay + "00"
                                onSendScancode(HidScancodes.KEY_KP_0)
                                onSendScancode(HidScancodes.KEY_KP_0)
                            } else {
                                if (scancode != HidScancodes.KEY_NONE) {
                                    onSendScancode(scancode)
                                }
                                if (label in "0123456789.") {
                                    calcDisplay = if (calcDisplay == "0") label else calcDisplay + label
                                } else if (label in listOf("+", "-", "*", "/")) {
                                    calcDisplay += " $label "
                                } else if (label == "BS") {
                                    calcDisplay = if (calcDisplay.length > 1) calcDisplay.dropLast(1) else "0"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isFullScreen) 68.dp else 52.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (color == KawaiiPink) DarkSlate else AmberGold,
                            fontSize = if (isFullScreen) 20.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HostChip(
    label: String,
    subLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DeepIndigo else CardPurple
        ),
        modifier = Modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) KawaiiPink else CardPurple,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = if (isSelected) AmberGold else SoftLavender,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = subLabel,
                color = if (isSelected) KawaiiPink else AccentLilac,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun QuickKeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = CardPurple),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = SoftLavender,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
