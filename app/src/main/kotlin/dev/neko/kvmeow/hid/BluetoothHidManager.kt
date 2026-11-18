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
        if (connectedDevices.contains(device)) {
            activeDevice = device
            onActiveHostChangedListener?.invoke(device)
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
        return hidDevice?.sendReport(dev, 1, report) ?: false
    }

    @SuppressLint("MissingPermission")
    fun sendMouseReport(buttons: Byte, dx: Byte, dy: Byte, wheel: Byte, target: BluetoothDevice? = activeDevice): Boolean {
        val dev = target ?: return false
        val report = byteArrayOf(buttons, dx, dy, wheel)
        return hidDevice?.sendReport(dev, 2, report) ?: false
    }
}
