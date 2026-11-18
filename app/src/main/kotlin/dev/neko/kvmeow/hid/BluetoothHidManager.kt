package dev.neko.kvmeow.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log

/**
 * 🐾 Bluetooth HID Manager for KVMeow Daemon 🐾
 * Registers as standard Bluetooth HID Device and manages paired computer endpoints.
 */
class BluetoothHidManager(private val context: Context) {
    private var hidDevice: BluetoothHidDevice? = null
    private val connectedDevices = mutableSetOf<BluetoothDevice>()
    private var activeDevice: BluetoothDevice? = null

    var onHostListChangedListener: (() -> Unit)? = null
    var onActiveHostChangedListener: ((BluetoothDevice?) -> Unit)? = null

    val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as? BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        val sdp = android.bluetooth.BluetoothHidDeviceAppSdpSettings(
            "KVMeow Bridge",
            "Kawaiian Bluetooth HID & Host Switcher",
            "t3hkitty",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidReportDescriptor.COMBO_REPORT_DESCRIPTOR
        )

        val qos = android.bluetooth.BluetoothHidDeviceAppQosSettings(
            android.bluetooth.BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800, 9, 0, 11250, android.bluetooth.BluetoothHidDeviceAppQosSettings.MAX
        )

        hidDevice?.registerApp(sdp, qos, null, Runnable::run, object : BluetoothHidDevice.Callback() {
            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                super.onConnectionStateChanged(device, state)
                device ?: return
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedDevices.add(device)
                        if (activeDevice == null) {
                            activeDevice = device
                            onActiveHostChangedListener?.invoke(device)
                        }
                        onHostListChangedListener?.invoke()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectedDevices.remove(device)
                        if (activeDevice == device) {
                            activeDevice = connectedDevices.firstOrNull()
                            onActiveHostChangedListener?.invoke(activeDevice)
                        }
                        onHostListChangedListener?.invoke()
                    }
                }
            }
        })
    }

    fun getConnectedHosts(): List<BluetoothDevice> = connectedDevices.toList()

    fun getActiveHost(): BluetoothDevice? = activeDevice

    fun selectHost(device: BluetoothDevice) {
        activeDevice = device
        if (!connectedDevices.contains(device)) {
            connectHost(device)
        }
        onActiveHostChangedListener?.invoke(device)
    }

    @SuppressLint("MissingPermission")
    fun connectHost(device: BluetoothDevice): Boolean {
        return try {
            hidDevice?.connect(device) ?: false
        } catch (e: SecurityException) {
            Log.e("BluetoothHidManager", "SecurityException connecting host", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectHost(device: BluetoothDevice): Boolean {
        return try {
            hidDevice?.disconnect(device) ?: false
        } catch (e: SecurityException) {
            Log.e("BluetoothHidManager", "SecurityException disconnecting host", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun sendKeyboardReport(modifier: Byte, keyCodes: ByteArray, target: BluetoothDevice? = activeDevice): Boolean {
        val dev = target ?: return false
        val report = ByteArray(8)
        report[0] = modifier
        report[1] = 0 // Reserved
        for (i in 0 until minOf(keyCodes.size, 6)) {
            report[2 + i] = keyCodes[i]
        }
        val sent = hidDevice?.sendReport(dev, 1, report) ?: false
        if (modifier != 0.toByte() || keyCodes.any { it != 0.toByte() }) {
            // Auto-send release report after delay so keys don't stick
            try {
                Thread.sleep(15)
                hidDevice?.sendReport(dev, 1, ByteArray(8))
            } catch (_: Exception) {}
        }
        return sent
    }

    @SuppressLint("MissingPermission")
    fun sendKeyStroke(char: Char, target: BluetoothDevice? = activeDevice): Boolean {
        val stroke = HidScancodes.getScancodeForAscii(char)
        if (stroke.scancode == HidScancodes.KEY_NONE) return false
        return sendKeyboardReport(stroke.modifier, byteArrayOf(stroke.scancode), target)
    }

    @SuppressLint("MissingPermission")
    fun sendTextDirect(text: String, target: BluetoothDevice? = activeDevice) {
        for (ch in text) {
            sendKeyStroke(ch, target)
            try {
                Thread.sleep(10)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun sendMouseReport(buttons: Byte, dx: Byte, dy: Byte, wheel: Byte, target: BluetoothDevice? = activeDevice): Boolean {
        val dev = target ?: return false
        val report = byteArrayOf(buttons, dx, dy, wheel)
        val sent = hidDevice?.sendReport(dev, 2, report) ?: false
        if (buttons != 0.toByte()) {
            try {
                Thread.sleep(20)
                hidDevice?.sendReport(dev, 2, byteArrayOf(0, 0, 0, 0))
            } catch (_: Exception) {}
        }
        return sent
    }
}
