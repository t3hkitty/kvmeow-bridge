package dev.neko.kvmeow.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.neko.kvmeow.caffeine.CaffeineJitterEngine
import dev.neko.kvmeow.hid.BluetoothHidManager

/**
 * 🐾 KVMeow Daemon Service 🐾
 * Keeps the Bluetooth HID peripheral profile alive, independent of any keyboard IME lifecycle.
 * Responds to local broadcast intents from IME keyboards, Tasker, or automation tiles.
 */
class KvmeowBridgeService : Service() {

    private lateinit var hidManager: BluetoothHidManager
    private lateinit var caffeineEngine: CaffeineJitterEngine

    companion object {
        const val ACTION_SEND_KEY = "dev.neko.kvmeow.ACTION_SEND_KEY"
        const val ACTION_SEND_MOUSE = "dev.neko.kvmeow.ACTION_SEND_MOUSE"
        const val ACTION_FLIP_HOST = "dev.neko.kvmeow.ACTION_FLIP_HOST"
        const val ACTION_TOGGLE_CAFFEINE = "dev.neko.kvmeow.ACTION_TOGGLE_CAFFEINE"
        const val ACTION_QUERY_STATUS = "dev.neko.kvmeow.ACTION_QUERY_STATUS"

        const val STATUS_UPDATE = "dev.neko.kvmeow.STATUS_UPDATE"

        const val EXTRA_MODIFIER = "modifier"
        const val EXTRA_KEYCODES = "keycodes"
        const val EXTRA_BUTTONS = "buttons"
        const val EXTRA_DX = "dx"
        const val EXTRA_DY = "dy"
        const val EXTRA_WHEEL = "wheel"
        const val EXTRA_HOST_MAC = "host_mac"
        const val EXTRA_CAFFEINE_ACTIVE = "caffeine_active"
        const val EXTRA_CONNECTED_HOSTS = "connected_hosts"
        const val EXTRA_ACTIVE_HOST = "active_host"
    }

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                ACTION_SEND_KEY -> {
                    val mod = intent.getByteExtra(EXTRA_MODIFIER, 0)
                    val keys = intent.getByteArrayExtra(EXTRA_KEYCODES) ?: ByteArray(0)
                    hidManager.sendKeyboardReport(mod, keys)
                }
                ACTION_SEND_MOUSE -> {
                    val buttons = intent.getByteExtra(EXTRA_BUTTONS, 0)
                    val dx = intent.getByteExtra(EXTRA_DX, 0)
                    val dy = intent.getByteExtra(EXTRA_DY, 0)
                    val wheel = intent.getByteExtra(EXTRA_WHEEL, 0)
                    hidManager.sendMouseReport(buttons, dx, dy, wheel)
                }
                ACTION_FLIP_HOST -> {
                    val mac = intent.getStringExtra(EXTRA_HOST_MAC)
                    if (mac != null) {
                        val dev = hidManager.getConnectedHosts().firstOrNull { it.address == mac }
                        if (dev != null) hidManager.selectHost(dev)
                    }
                }
                ACTION_TOGGLE_CAFFEINE -> {
                    if (caffeineEngine.isActive()) {
                        caffeineEngine.stop()
                    } else {
                        caffeineEngine.start()
                    }
                    broadcastStatus()
                }
                ACTION_QUERY_STATUS -> {
                    broadcastStatus()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        hidManager = BluetoothHidManager(this)
        caffeineEngine = CaffeineJitterEngine(hidManager)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        adapter?.getProfileProxy(this, hidManager.serviceListener, BluetoothProfile.HID_DEVICE)

        hidManager.onHostListChangedListener = { broadcastStatus() }
        hidManager.onActiveHostChangedListener = { broadcastStatus() }

        val filter = IntentFilter().apply {
            addAction(ACTION_SEND_KEY)
            addAction(ACTION_SEND_MOUSE)
            addAction(ACTION_FLIP_HOST)
            addAction(ACTION_TOGGLE_CAFFEINE)
            addAction(ACTION_QUERY_STATUS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }

        startForeground(1001, createNotification())
    }

    @SuppressLint("MissingPermission")
    private fun broadcastStatus() {
        val statusIntent = Intent(STATUS_UPDATE).apply {
            putExtra(EXTRA_CAFFEINE_ACTIVE, caffeineEngine.isActive())
            val hostNames = hidManager.getConnectedHosts().map { "${it.name ?: "PC"} (${it.address})" }.toTypedArray()
            val hostMacs = hidManager.getConnectedHosts().map { it.address }.toTypedArray()
            putExtra(EXTRA_CONNECTED_HOSTS, hostNames)
            putExtra("host_macs", hostMacs)
            val active = hidManager.getActiveHost()
            putExtra(EXTRA_ACTIVE_HOST, active?.address)
        }
        sendBroadcast(statusIntent)
    }

    private fun createNotification(): Notification {
        val channelId = "kvmeow_bridge_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "KVMeow Bluetooth Bridge",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🐾 KVMeow Bridge Active")
            .setContentText("Listening for HID keyboard/mouse and keep-alive jitter commands")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        unregisterReceiver(commandReceiver)
        caffeineEngine.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
