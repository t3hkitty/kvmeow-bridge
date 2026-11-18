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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    onToggleCaffeine: () -> Unit,
    onScanRefresh: () -> Unit
) {
    var directInputText by remember { mutableStateOf("") }

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

        Spacer(modifier = Modifier.height(12.dp))

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
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 🚀 1-Tap Recent Device Switcher Strip
        Text(
            text = "⚡ RECENT DEVICES (1-Tap Switch)",
            color = AccentLilac,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Android Local
            HostChip(
                label = "📱 Local Android",
                subLabel = "Local IME",
                isSelected = activeHostMac == null,
                onClick = { onSwitchHost("") }
            )

            // Paired & Connected Hosts
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

        Spacer(modifier = Modifier.height(16.dp))

        // ⚡ Direct Live Keyboard Typing Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepIndigo),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CardPurple, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚡ Direct Mode (Live Host Typing)",
                        color = AmberGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = onScanRefresh,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Refresh",
                            tint = SoftLavender
                        )
                    }
                }

                Text(
                    text = "Type or dictate here — every keystroke is piped straight to the active computer in real-time!",
                    color = SoftLavender,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                OutlinedTextField(
                    value = directInputText,
                    onValueChange = { newText ->
                        if (newText.length > directInputText.length) {
                            val added = newText.substring(directInputText.length)
                            onSendDirectText(added)
                        }
                        directInputText = newText
                        if (directInputText.length > 50) {
                            directInputText = ""
                        }
                    },
                    placeholder = {
                        Text(
                            "Type directly to computer...",
                            color = AccentLilac,
                            fontSize = 13.sp
                        )
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
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickKeyButton(
                        label = "Enter ⏎",
                        modifier = Modifier.weight(1f),
                        onClick = { onSendDirectText("\n") }
                    )
                    QuickKeyButton(
                        label = "Tab ⇥",
                        modifier = Modifier.weight(1f),
                        onClick = { onSendDirectText("\t") }
                    )
                    QuickKeyButton(
                        label = "Space ␣",
                        modifier = Modifier.weight(1f),
                        onClick = { onSendDirectText(" ") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 💜 Status & Host Ledger Info
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardPurple),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "💜 [#zettelkasten] Host Ledger",
                    color = SoftLavender,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Active Target: ${activeHostMac ?: "No active target (Local)"}",
                    color = AmberGold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
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
