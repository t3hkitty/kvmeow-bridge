package net.artkitty.kvmeowboard.hid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * 🐾 KvMeowHidService 🐾
 * Native Bluetooth Classic HID ForegroundService for KVMeowboard.
 * Holds PARTIAL_WAKE_LOCK and runs BluetoothHidController with multi-host zero-net displacement jiggler.
 */
class KvMeowHidService : Service() {

    private lateinit var hidController: BluetoothHidController
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "kvmeowboard_hid_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_JIGGLE = "net.artkitty.kvmeowboard.ACTION_START_JIGGLE"
        const val ACTION_STOP_JIGGLE = "net.artkitty.kvmeowboard.ACTION_STOP_JIGGLE"
        const val ACTION_SWITCH_HOST = "net.artkitty.kvmeowboard.ACTION_SWITCH_HOST"
        const val EXTRA_HOST_MAC = "host_mac"
    }

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()

        hidController = BluetoothHidController(this)
        hidController.registerApp()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("KVMeowboard HID Active", "Connected as Bluetooth Combo Keyboard/Mouse"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_JIGGLE -> {
                hidController.startMultiHostJiggler({
                    // Defaults to currently active host if bonded
                    listOfNotNull(hidController.getActiveHost())
                })
                updateNotification("Jiggler Active", "Emitting zero-drift Report 2 micro-ticks")
            }
            ACTION_STOP_JIGGLE -> {
                hidController.stopJiggler()
                updateNotification("Jiggler Stopped", "Keyboard & Mouse ready")
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KVMeowboard:HidServiceWakeLock").apply {
            setReferenceCounted(false)
            acquire(24 * 60 * 60 * 1000L) // 24 hours
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "KVMeowboard HID Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🐾 $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(title, text))
    }

    override fun onDestroy() {
        hidController.stopJiggler()
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
