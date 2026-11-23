package net.artkitty.kvmeowboard.hid

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object HidDescriptor {
    const val REPORT_ID_KEYBOARD: Int = 1
    const val REPORT_ID_MOUSE: Int = 2
    const val REPORT_ID_SYSTEM: Int = 3
    const val REPORT_ID_CONSUMER: Int = 4

    val COMPOSITE_DESCRIPTOR: ByteArray = byteArrayOf(
        // --- Report ID 1: Standard 8-Byte Boot/NKRO-Compatible Keyboard ---
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x01.toByte(), //   Report ID (1)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Key Codes)
        0x19.toByte(), 0xE0.toByte(), //   Usage Minimum (224 - Left Ctrl)
        0x29.toByte(), 0xE7.toByte(), //   Usage Maximum (231 - Right GUI)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x08.toByte(), //   Report Count (8)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute) - Modifier byte
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x81.toByte(), 0x01.toByte(), //   Input (Constant) - Reserved byte
        0x95.toByte(), 0x06.toByte(), //   Report Count (6)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(), //   Logical Maximum (101)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Key Codes)
        0x19.toByte(), 0x00.toByte(), //   Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(), //   Usage Maximum (101)
        0x81.toByte(), 0x00.toByte(), //   Input (Data, Array) - 6 keycodes
        0xC0.toByte(),                // End Collection

        // --- Report ID 2: 4-Byte Relative Mouse + Vertical Wheel ---
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x02.toByte(), //   Report ID (2)
        0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
        0x05.toByte(), 0x09.toByte(), //     Usage Page (Buttons)
        0x19.toByte(), 0x01.toByte(), //     Usage Minimum (Button 1)
        0x29.toByte(), 0x03.toByte(), //     Usage Maximum (Button 3)
        0x15.toByte(), 0x00.toByte(), //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //     Logical Maximum (1)
        0x95.toByte(), 0x03.toByte(), //     Report Count (3)
        0x75.toByte(), 0x01.toByte(), //     Report Size (1)
        0x81.toByte(), 0x02.toByte(), //     Input (Data, Variable, Absolute) - 3 button bits
        0x95.toByte(), 0x01.toByte(), //     Report Count (1)
        0x75.toByte(), 0x05.toByte(), //     Report Size (5)
        0x81.toByte(), 0x01.toByte(), //     Input (Constant) - 5 padding bits
        0x05.toByte(), 0x01.toByte(), //     Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //     Usage (X)
        0x09.toByte(), 0x31.toByte(), //     Usage (Y)
        0x09.toByte(), 0x38.toByte(), //     Usage (Wheel)
        0x15.toByte(), 0x81.toByte(), //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(), //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(), //     Report Size (8)
        0x95.toByte(), 0x03.toByte(), //     Report Count (3)
        0x81.toByte(), 0x06.toByte(), //     Input (Data, Variable, Relative) - dx, dy, wheel
        0xC0.toByte(),                //   End Collection
        0xC0.toByte(),                // End Collection

        // --- Report ID 3: 1-Byte System Control (Power / Sleep / Wake) ---
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x80.toByte(), // Usage (System Control)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x03.toByte(), //   Report ID (3)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x03.toByte(), //   Report Count (3)
        0x09.toByte(), 0x81.toByte(), //   Usage (System Power Down)
        0x09.toByte(), 0x82.toByte(), //   Usage (System Sleep)
        0x09.toByte(), 0x83.toByte(), //   Usage (System Wake Up)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute)
        0x95.toByte(), 0x05.toByte(), //   Report Count (5)
        0x81.toByte(), 0x01.toByte(), //   Input (Constant)
        0xC0.toByte(),                // End Collection

        // --- Report ID 4: 2-Byte Consumer Control (Media & Brightness) ---
        0x05.toByte(), 0x0C.toByte(), // Usage Page (Consumer Devices)
        0x09.toByte(), 0x01.toByte(), // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x04.toByte(), //   Report ID (4)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // Logical Maximum (1023)
        0x19.toByte(), 0x00.toByte(), //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // Usage Maximum (1023)
        0x75.toByte(), 0x10.toByte(), //   Report Size (16)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x00.toByte(), //   Input (Data, Array)
        0xC0.toByte()                 // End Collection
    )
}

@SuppressLint("MissingPermission")
class BluetoothHidController(private val context: Context) {
    private val btManager = context.getSystemService(BluetoothManager::class.java)
    private val btAdapter: BluetoothAdapter? = btManager?.adapter
    private var hidDevice: BluetoothHidDevice? = null
    private var activeHost: BluetoothDevice? = null

    // Dedicated single-thread executor prevents UI jank & orders HID frames strictly
    private val hidExecutor = Executors.newSingleThreadScheduledExecutor()
    private var jiggleTask: ScheduledFuture<*>? = null

    private val appCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered && pluggedDevice != null) {
                hidDevice?.connect(pluggedDevice)
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> activeHost = device
                BluetoothProfile.STATE_DISCONNECTED -> if (activeHost == device) activeHost = null
            }
        }
    }

    fun registerApp() {
        btAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile != BluetoothProfile.HID_DEVICE) return
                hidDevice = proxy as BluetoothHidDevice

                val sdp = BluetoothHidDeviceAppSdpSettings(
                    "KVMeowboard",
                    "Composite Bluetooth HID Deck",
                    "artkitty",
                    BluetoothHidDevice.SUBCLASS1_COMBO, // 0xC0 -> Advertises as Keyboard + Mouse
                    HidDescriptor.COMPOSITE_DESCRIPTOR
                )

                val qosOut = BluetoothHidDeviceAppQosSettings(
                    BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                    800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
                )

                hidDevice?.registerApp(sdp, null, qosOut, hidExecutor, appCallback)
            }

            override fun onServiceDisconnected(profile: Int) {
                hidDevice = null
                activeHost = null
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    fun switchHost(target: BluetoothDevice) {
        hidExecutor.execute {
            sendKeyboardReport(0x00, 0x00) // Release any latched modifiers before switching
            activeHost?.let { current ->
                if (current.address != target.address) hidDevice?.disconnect(current)
            }
            hidDevice?.connect(target)
        }
    }

    fun sendKeyboardReport(modifierMask: Byte, keyCode: Byte) {
        val host = activeHost ?: return
        hidExecutor.execute {
            val payload = byteArrayOf(modifierMask, 0x00, keyCode, 0, 0, 0, 0, 0)
            hidDevice?.sendReport(host, HidDescriptor.REPORT_ID_KEYBOARD, payload)
        }
    }

    fun sendKeyTap(modifierMask: Byte, keyCode: Byte, holdMs: Long = 18L) {
        val host = activeHost ?: return
        hidExecutor.execute {
            hidDevice?.sendReport(host, HidDescriptor.REPORT_ID_KEYBOARD, byteArrayOf(modifierMask, 0, keyCode, 0, 0, 0, 0, 0))
            Thread.sleep(holdMs)
            hidDevice?.sendReport(host, HidDescriptor.REPORT_ID_KEYBOARD, byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0))
        }
    }

    fun sendMouseReport(buttons: Byte, dx: Byte, dy: Byte, wheel: Byte = 0) {
        val host = activeHost ?: return
        hidExecutor.execute {
            hidDevice?.sendReport(host, HidDescriptor.REPORT_ID_MOUSE, byteArrayOf(buttons, dx, dy, wheel))
        }
    }

    /** Zero-net-displacement background jiggler driven inside ForegroundService + PARTIAL_WAKE_LOCK */
    fun startMultiHostJiggler(
        enabledHostsProvider: () -> List<BluetoothDevice>,
        intervalSeconds: Long = 35L
    ) {
        jiggleTask?.cancel(false)
        jiggleTask = hidExecutor.scheduleAtFixedRate({
            val targets = enabledHostsProvider()
            if (targets.isEmpty()) return@scheduleAtFixedRate
            val originalHost = activeHost
            for (target in targets) {
                if (activeHost?.address != target.address) {
                    hidDevice?.connect(target)
                    Thread.sleep(350L)
                }
                hidDevice?.sendReport(target, HidDescriptor.REPORT_ID_MOUSE, byteArrayOf(0, 1, 0, 0))
                Thread.sleep(40L)
                hidDevice?.sendReport(target, HidDescriptor.REPORT_ID_MOUSE, byteArrayOf(0, (-1).toByte(), 0, 0))
            }
            if (originalHost != null && activeHost?.address != originalHost.address && targets.size > 1) {
                hidDevice?.connect(originalHost)
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS)
    }

    fun stopJiggler() {
        jiggleTask?.cancel(false)
        jiggleTask = null
    }

    fun getActiveHost(): BluetoothDevice? = activeHost
}
