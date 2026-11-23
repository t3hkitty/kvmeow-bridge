package net.artkitty.kvmeowboard

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import net.artkitty.kvmeowboard.calc.AccountantCalculatorController
import net.artkitty.kvmeowboard.hid.*
import net.artkitty.kvmeowboard.ui.DeckThemeMode
import net.artkitty.kvmeowboard.ui.HostOs
import net.artkitty.kvmeowboard.ui.HostSlot
import net.artkitty.kvmeowboard.ui.KvMeowDeckScreen

class MainActivity : ComponentActivity() {

    private val prefs by lazy {
        getSharedPreferences("kvmeowboard_prefs", Context.MODE_PRIVATE)
    }

    private val modifierMachine = ModifierStateMachine()
    private val calcController = AccountantCalculatorController { total ->
        hidService?.hidController?.let { hid ->
            AsciiHidKeymap.typeAsciiString(total, modifierMachine.currentMask(), hid)
            modifierMachine.consumeOneShots()
        }
    }

    private var hidService: KvMeowHidService? = null
    private var serviceBound by mutableStateOf(false)

    private var themeState = mutableStateOf(loadSavedTheme())
    private var activeHostIdState = mutableStateOf(prefs.getString("active_host_id", "win11") ?: "win11")
    private var showThirdSlotState = mutableStateOf(prefs.getBoolean("show_third_slot", false))
    private var jiggleWin11State = mutableStateOf(prefs.getBoolean("jiggle_win11", false))
    private var jiggleMacState = mutableStateOf(prefs.getBoolean("jiggle_mac", false))
    private var jiggleSlot3State = mutableStateOf(prefs.getBoolean("jiggle_slot3", false))

    private var macWin11State = mutableStateOf(prefs.getString("mac_win11", null))
    private var macMacBookState = mutableStateOf(prefs.getString("mac_macbook", null))
    private var macSlot3State = mutableStateOf(prefs.getString("mac_slot3", null))
    private var labelSlot3State = mutableStateOf(prefs.getString("label_slot3", "Host 3") ?: "Host 3")
    private var osSlot3State = mutableStateOf(
        runCatching { HostOs.valueOf(prefs.getString("os_slot3", HostOs.LINUX.name)!!) }
            .getOrDefault(HostOs.LINUX)
    )

    private var bondedDevicesState = mutableStateOf<List<BluetoothDevice>>(emptyList())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? KvMeowHidService.LocalBinder ?: return
            hidService = localBinder.getService()
            serviceBound = true
            syncJigglerTargetsToService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            serviceBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshBondedDevices()
        startAndBindHidService()
        requestIgnoreBatteryOptimizationsIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by themeState
            val activeHostId by activeHostIdState
            val showThirdSlot by showThirdSlotState
            val jiggleWin11 by jiggleWin11State
            val jiggleMac by jiggleMacState
            val jiggleSlot3 by jiggleSlot3State
            val macWin11 by macWin11State
            val macMacBook by macMacBookState
            val macSlot3 by macSlot3State
            val labelSlot3 by labelSlot3State
            val osSlot3 by osSlot3State
            val bonded = bondedDevicesState.value
            val currentHid = hidService?.hidController

            val hosts = remember(
                bonded,
                jiggleWin11,
                jiggleMac,
                jiggleSlot3,
                macWin11,
                macMacBook,
                macSlot3,
                labelSlot3,
                osSlot3
            ) {
                buildHostSlots(
                    bonded = bonded,
                    jiggleWin11 = jiggleWin11,
                    jiggleMac = jiggleMac,
                    jiggleSlot3 = jiggleSlot3
                )
            }

            if (currentHid != null) {
                KvMeowDeckScreen(
                    themeMode = themeMode,
                    onThemeChange = { nextTheme ->
                        themeState.value = nextTheme
                        prefs.edit().putString("theme_mode", nextTheme.name).apply()
                    },
                    hosts = hosts,
                    bondedDevices = bonded,
                    activeHostId = activeHostId,
                    onSelectHost = { slot ->
                        activeHostIdState.value = slot.id
                        prefs.edit().putString("active_host_id", slot.id).apply()
                        slot.device?.let { device ->
                            currentHid.switchHost(device)
                        }
                    },
                    onToggleHostJiggle = { hostId ->
                        when (hostId) {
                            "win11" -> {
                                val next = !jiggleWin11State.value
                                jiggleWin11State.value = next
                                prefs.edit().putBoolean("jiggle_win11", next).apply()
                            }
                            "macbook" -> {
                                val next = !jiggleMacState.value
                                jiggleMacState.value = next
                                prefs.edit().putBoolean("jiggle_mac", next).apply()
                            }
                            "slot3" -> {
                                val next = !jiggleSlot3State.value
                                jiggleSlot3State.value = next
                                prefs.edit().putBoolean("jiggle_slot3", next).apply()
                            }
                        }
                        syncJigglerTargetsToService()
                        TileService.requestListeningState(
                            this,
                            ComponentName(this, KvMeowJiggleTileService::class.java)
                        )
                    },
                    showThirdSlotInSettings = showThirdSlot,
                    onToggleThirdSlotInSettings = { enabled ->
                        showThirdSlotState.value = enabled
                        prefs.edit().putBoolean("show_third_slot", enabled).apply()
                        syncJigglerTargetsToService()
                    },
                    onAssignSlotMac = { slotId, mac ->
                        val prefKey = when (slotId) {
                            "win11" -> {
                                macWin11State.value = mac
                                "mac_win11"
                            }
                            "macbook" -> {
                                macMacBookState.value = mac
                                "mac_macbook"
                            }
                            else -> {
                                macSlot3State.value = mac
                                "mac_slot3"
                            }
                        }
                        prefs.edit().putString(prefKey, mac).apply()
                        syncJigglerTargetsToService()
                    },
                    onUpdateThirdSlotMeta = { label, os ->
                        labelSlot3State.value = label
                        osSlot3State.value = os
                        prefs.edit()
                            .putString("label_slot3", label)
                            .putString("os_slot3", os.name)
                            .apply()
                    },
                    onRefreshBondedDevices = { refreshBondedDevices() },
                    hidController = currentHid,
                    modifierMachine = modifierMachine,
                    calcController = calcController
                )
            }
        }

        ensureRuntimePermissions()
    }

    override fun onResume() {
        super.onResume()
        jiggleWin11State.value = prefs.getBoolean("jiggle_win11", false)
        jiggleMacState.value = prefs.getBoolean("jiggle_mac", false)
        jiggleSlot3State.value = prefs.getBoolean("jiggle_slot3", false)
        refreshBondedDevices()
        GestureSuppressionHelper.suppressThreeFingerScreenshotIfRooted()
        if (serviceBound) {
            syncJigglerTargetsToService()
        }
    }

    override fun onPause() {
        modifierMachine.releaseAll()
        GestureSuppressionHelper.restoreSystemGestures()
        super.onPause()
    }

    override fun onDestroy() {
        GestureSuppressionHelper.restoreSystemGestures()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        super.onDestroy()
    }

    private fun ensureRuntimePermissions() {
        val required = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val missing = required.filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            refreshBondedDevices()
            startAndBindHidService()
            requestIgnoreBatteryOptimizationsIfNeeded()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startAndBindHidService() {
        val intent = Intent(this, KvMeowHidService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun requestIgnoreBatteryOptimizationsIfNeeded() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshBondedDevices() {
        val hasConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        if (!hasConnect) return

        val btAdapter = getSystemService(BluetoothManager::class.java)?.adapter ?: return
        bondedDevicesState.value = btAdapter.bondedDevices?.toList().orEmpty()
    }

    private fun buildHostSlots(
        bonded: List<BluetoothDevice>,
        jiggleWin11: Boolean,
        jiggleMac: Boolean,
        jiggleSlot3: Boolean
    ): List<HostSlot> {
        val winMac = macWin11State.value
        val macMac = macMacBookState.value
        val slot3Mac = macSlot3State.value

        val winDevice = bonded.firstOrNull { it.address == winMac } ?: bonded.getOrNull(0)
        val macDevice = bonded.firstOrNull { it.address == macMac } ?: bonded.getOrNull(1)
        val slot3Device = bonded.firstOrNull { it.address == slot3Mac } ?: bonded.getOrNull(2)

        return listOf(
            HostSlot(
                id = "win11",
                label = "Win 11",
                os = HostOs.WINDOWS,
                device = winDevice,
                jiggleEnabled = jiggleWin11
            ),
            HostSlot(
                id = "macbook",
                label = "MacBook",
                os = HostOs.MACOS,
                device = macDevice,
                jiggleEnabled = jiggleMac
            ),
            HostSlot(
                id = "slot3",
                label = labelSlot3State.value,
                os = osSlot3State.value,
                device = slot3Device,
                jiggleEnabled = jiggleSlot3
            )
        )
    }

    private fun syncJigglerTargetsToService() {
        val svc = hidService ?: return
        val slots = buildHostSlots(
            bonded = bondedDevicesState.value,
            jiggleWin11 = jiggleWin11State.value,
            jiggleMac = jiggleMacState.value,
            jiggleSlot3 = jiggleSlot3State.value
        )
        val activeSlots = if (showThirdSlotState.value) slots.take(3) else slots.take(2)
        val jiggleDevices = activeSlots.filter { it.jiggleEnabled }.mapNotNull { it.device }
        svc.refreshJiggler(hostsProvider = { jiggleDevices }, intervalSeconds = 35L)
    }

    private fun loadSavedTheme(): DeckThemeMode {
        val raw = prefs.getString("theme_mode", DeckThemeMode.DARK_KAWAII.name)
        return runCatching { DeckThemeMode.valueOf(raw ?: DeckThemeMode.DARK_KAWAII.name) }
            .getOrDefault(DeckThemeMode.DARK_KAWAII)
    }
}
