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
        const val ACTION_SEND_RAW_KEYCODE = "dev.neko.kvmeow.ACTION_SEND_RAW_KEYCODE"
        const val ACTION_SEND_TEXT = "dev.neko.kvmeow.ACTION_SEND_TEXT"
        const val ACTION_SEND_UNICODE_HEX = "dev.neko.kvmeow.ACTION_SEND_UNICODE_HEX"
        const val ACTION_STAGE_MEDIA_PASTE = "dev.neko.kvmeow.ACTION_STAGE_MEDIA_PASTE"
        const val ACTION_MISSION_CONTROL = "dev.neko.kvmeow.ACTION_MISSION_CONTROL"
        const val ACTION_APP_EXPOSE = "dev.neko.kvmeow.ACTION_APP_EXPOSE"
        const val ACTION_SEND_MOUSE = "dev.neko.kvmeow.ACTION_SEND_MOUSE"
        const val ACTION_FLIP_HOST = "dev.neko.kvmeow.ACTION_FLIP_HOST"
        const val ACTION_PAUSE_CAFFEINE = "dev.neko.kvmeow.ACTION_PAUSE_CAFFEINE"
        const val ACTION_RESUME_CAFFEINE = "dev.neko.kvmeow.ACTION_RESUME_CAFFEINE"
        const val ACTION_STOP_CAFFEINE = "dev.neko.kvmeow.ACTION_STOP_CAFFEINE"
        const val ACTION_TOGGLE_CAFFEINE = "dev.neko.kvmeow.ACTION_TOGGLE_CAFFEINE"
        const val ACTION_SET_HOST_JIGGLE = "dev.neko.kvmeow.ACTION_SET_HOST_JIGGLE"
        const val ACTION_QUERY_STATUS = "dev.neko.kvmeow.ACTION_QUERY_STATUS"

        const val STATUS_UPDATE = "dev.neko.kvmeow.STATUS_UPDATE"

        const val EXTRA_MODIFIER = "modifier"
        const val EXTRA_KEYCODES = "keycodes"
        const val EXTRA_SCANCODE = "scancode"
        const val EXTRA_TEXT = "text"
        const val EXTRA_TARGET_OS = "target_os"
        const val EXTRA_MEDIA_URI = "media_uri"
        const val EXTRA_MIME_TYPE = "mime_type"
        const val EXTRA_FALLBACK_TEXT = "fallback_text"
        const val EXTRA_BUTTONS = "buttons"
        const val EXTRA_DX = "dx"
        const val EXTRA_DY = "dy"
        const val EXTRA_WHEEL = "wheel"
        const val EXTRA_HOST_MAC = "host_mac"
        const val EXTRA_ENABLED = "enabled"
        const val EXTRA_CAFFEINE_ACTIVE = "caffeine_active"
        const val EXTRA_CAFFEINE_PAUSED = "caffeine_paused"
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
                ACTION_SEND_RAW_KEYCODE -> {
                    val mod = intent.getByteExtra(EXTRA_MODIFIER, 0)
                    val scancode = intent.getByteExtra(EXTRA_SCANCODE, 0)
                    if (scancode != 0.toByte()) {
                        hidManager.sendRawKeycode(scancode, mod)
                    }
                }
                ACTION_SEND_TEXT -> {
                    val text = intent.getStringExtra(EXTRA_TEXT)
                    if (!text.isNullOrEmpty()) {
                        Thread { hidManager.sendTextDirect(text) }.start()
                    }
                }
                ACTION_SEND_UNICODE_HEX -> {
                    val text = intent.getStringExtra(EXTRA_TEXT)
                    val targetOsStr = intent.getStringExtra(EXTRA_TARGET_OS) ?: "MACOS"
                    val targetOs = try {
                        dev.neko.kvmeow.unicode.UnicodeInputEngine.TargetOs.valueOf(targetOsStr)
                    } catch (_: Exception) {
                        dev.neko.kvmeow.unicode.UnicodeInputEngine.TargetOs.MACOS
                    }
                    if (!text.isNullOrEmpty()) {
                        Thread {
                            dev.neko.kvmeow.unicode.UnicodeInputEngine.sendUnicodeHex(
                                text = text,
                                targetOs = targetOs,
                                hidManager = hidManager,
                                onClipboardFallback = { fallbackStr ->
                                    clipboardBridge.pasteTextDirect(fallbackStr, targetOs)
                                }
                            )
                        }.start()
                    }
                }
                ACTION_STAGE_MEDIA_PASTE -> {
                    val uriString = intent.getStringExtra(EXTRA_MEDIA_URI)
                    val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "image/png"
                    val fallbackText = intent.getStringExtra(EXTRA_FALLBACK_TEXT) ?: ""
                    val targetOsStr = intent.getStringExtra(EXTRA_TARGET_OS) ?: "MACOS"
                    val targetOs = try {
                        dev.neko.kvmeow.unicode.UnicodeInputEngine.TargetOs.valueOf(targetOsStr)
                    } catch (_: Exception) {
                        dev.neko.kvmeow.unicode.UnicodeInputEngine.TargetOs.MACOS
                    }
                    val uri = if (!uriString.isNullOrEmpty()) android.net.Uri.parse(uriString) else null
                    clipboardBridge.stageAndPasteMedia(uri, mimeType, fallbackText, targetOs)
                }
                ACTION_MISSION_CONTROL -> {
                    // macOS Mission Control shortcut: Ctrl + Up Arrow
                    hidManager.sendRawKeycode(dev.neko.kvmeow.hid.HidScancodes.KEY_UP, dev.neko.kvmeow.hid.HidScancodes.MODIFIER_LEFT_CTRL)
                }
                ACTION_APP_EXPOSE -> {
                    // macOS App Exposé shortcut: Ctrl + Down Arrow
                    hidManager.sendRawKeycode(dev.neko.kvmeow.hid.HidScancodes.KEY_DOWN, dev.neko.kvmeow.hid.HidScancodes.MODIFIER_LEFT_CTRL)
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
                    updateNotification()
                    broadcastStatus()
                }
                ACTION_PAUSE_CAFFEINE -> {
                    caffeineEngine.pause()
                    updateNotification()
                    broadcastStatus()
                }
                ACTION_RESUME_CAFFEINE -> {
                    caffeineEngine.resume()
                    updateNotification()
                    broadcastStatus()
                }
                ACTION_STOP_CAFFEINE -> {
                    caffeineEngine.stop()
                    updateNotification()
                    broadcastStatus()
                }
                ACTION_SET_HOST_JIGGLE -> {
                    val mac = intent.getStringExtra(EXTRA_HOST_MAC)
                    val enabled = intent.getBooleanExtra(EXTRA_ENABLED, true)
                    if (mac != null) {
                        caffeineEngine.setHostJiggleEnabled(mac, enabled)
                        updateNotification()
                        broadcastStatus()
                    }
                }
                ACTION_QUERY_STATUS -> {
                    broadcastStatus()
                }
            }
        }
    }

    private lateinit var clipboardBridge: dev.neko.kvmeow.clipboard.HostClipboardSyncBridge

    override fun onCreate() {
        super.onCreate()
        hidManager = BluetoothHidManager(this)
        caffeineEngine = CaffeineJitterEngine(this, hidManager)
        clipboardBridge = dev.neko.kvmeow.clipboard.HostClipboardSyncBridge(this, hidManager)
        clipboardBridge.start()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        adapter?.getProfileProxy(this, hidManager.serviceListener, BluetoothProfile.HID_DEVICE)

        hidManager.onHostListChangedListener = { broadcastStatus() }
        hidManager.onActiveHostChangedListener = { broadcastStatus() }

        val filter = IntentFilter().apply {
            addAction(ACTION_SEND_KEY)
            addAction(ACTION_SEND_RAW_KEYCODE)
            addAction(ACTION_SEND_TEXT)
            addAction(ACTION_SEND_UNICODE_HEX)
            addAction(ACTION_STAGE_MEDIA_PASTE)
            addAction(ACTION_MISSION_CONTROL)
            addAction(ACTION_APP_EXPOSE)
            addAction(ACTION_SEND_MOUSE)
            addAction(ACTION_FLIP_HOST)
            addAction(ACTION_TOGGLE_CAFFEINE)
            addAction(ACTION_PAUSE_CAFFEINE)
            addAction(ACTION_RESUME_CAFFEINE)
            addAction(ACTION_STOP_CAFFEINE)
            addAction(ACTION_SET_HOST_JIGGLE)
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
            putExtra(EXTRA_CAFFEINE_PAUSED, caffeineEngine.isPaused())
            val hostNames = hidManager.getConnectedHosts().map { "${it.name ?: "PC"} (${it.address})" }.toTypedArray()
            val hostMacs = hidManager.getConnectedHosts().map { it.address }.toTypedArray()
            putExtra(EXTRA_CONNECTED_HOSTS, hostNames)
            putExtra("host_macs", hostMacs)
            val active = hidManager.getActiveHost()
            putExtra(EXTRA_ACTIVE_HOST, active?.address)
        }
        sendBroadcast(statusIntent)
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1001, createNotification())
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

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pauseIntent = Intent(ACTION_PAUSE_CAFFEINE)
        val pausePending = android.app.PendingIntent.getBroadcast(this, 1, pauseIntent, flag)

        val resumeIntent = Intent(ACTION_RESUME_CAFFEINE)
        val resumePending = android.app.PendingIntent.getBroadcast(this, 2, resumeIntent, flag)

        val stopIntent = Intent(ACTION_STOP_CAFFEINE)
        val stopPending = android.app.PendingIntent.getBroadcast(this, 3, stopIntent, flag)

        val isJittering = caffeineEngine.isActive()
        val isPaused = caffeineEngine.isPaused()

        val statusText = when {
            isPaused -> "☕ Jitter Paused"
            isJittering -> "☕ Micro-Jitter Active (Zero Drift)"
            else -> "Listening for HID keyboard/mouse commands"
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🐾 KVMeow Bridge Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)

        if (isJittering) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause All", pausePending)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
        } else if (isPaused) {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "Start Jitter", resumePending)
        }

        return builder.build()
    }

    override fun onDestroy() {
        unregisterReceiver(commandReceiver)
        caffeineEngine.stop()
        clipboardBridge.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
