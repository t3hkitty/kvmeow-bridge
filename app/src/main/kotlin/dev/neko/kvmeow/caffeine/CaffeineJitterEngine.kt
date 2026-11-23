package dev.neko.kvmeow.caffeine

import android.content.Context
import android.os.PowerManager
import android.util.Log
import dev.neko.kvmeow.hid.BluetoothHidManager
import kotlinx.coroutines.*

/**
 * ☕ Multi-Host Caffeine Keep-Alive Engine (Zero Non-Focus Lockouts) ☕
 *
 * Runs inside ForegroundService backed by a PARTIAL_WAKE_LOCK and drift-proof scheduler.
 * Broadcasts zero-net-displacement micro-jitter reports (+1px followed immediately by -1px)
 * or 0px relative HID mouse movement packets to ALL connected PC hosts simultaneously.
 *
 * Configurable interval (30s-60s) guarantees host PCs stay awake when phone screen is off.
 */
class CaffeineJitterEngine(
    private val context: Context,
    private val hidManager: BluetoothHidManager
) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    var jitterIntervalMs: Long = 45_000L
    private var isPaused = false
    private val enabledHostMacs = mutableSetOf<String>()

    fun setHostJiggleEnabled(mac: String, enabled: Boolean) {
        if (enabled) {
            enabledHostMacs.add(mac)
            if (!isActive() && !isPaused) {
                start()
            }
        } else {
            enabledHostMacs.remove(mac)
            if (enabledHostMacs.isEmpty()) {
                stop()
            }
        }
    }

    fun isHostJiggleEnabled(mac: String): Boolean = enabledHostMacs.contains(mac)

    fun start() {
        if (job?.isActive == true) {
            isPaused = false
            return
        }
        isPaused = false

        acquireWakeLock()

        job = scope.launch {
            var step = true
            while (isActive) {
                delay(jitterIntervalMs)
                if (isPaused) continue

                val connectedHosts = hidManager.getConnectedHosts()
                // Target hosts: if enabledHostMacs is non-empty, filter connected hosts by MAC, otherwise target all
                val targetHosts = if (enabledHostMacs.isNotEmpty()) {
                    connectedHosts.filter { enabledHostMacs.contains(it.address) }
                } else {
                    connectedHosts
                }

                if (targetHosts.isNotEmpty()) {
                    for (host in targetHosts) {
                        try {
                            // Zero-net-displacement micro-jitter (+1px, then immediately -1px)
                            val nudge: Byte = if (step) 1 else -1
                            hidManager.sendMouseReport(0, nudge, 0, 0, target = host)
                            delay(25)
                            hidManager.sendMouseReport(0, (-nudge).toByte(), 0, 0, target = host)
                            delay(10)
                            // Release zero packet
                            hidManager.sendMouseReport(0, 0, 0, 0, target = host)
                        } catch (e: Exception) {
                            Log.e("CaffeineJitterEngine", "Error sending keep-alive to host", e)
                        }
                    }
                    step = !step
                }
            }
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
        if (job?.isActive != true) {
            start()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        isPaused = false
        releaseWakeLock()
    }

    fun isActive(): Boolean = job?.isActive == true && !isPaused
    fun isPaused(): Boolean = isPaused

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KVMeow:CaffeineKeepAliveWakeLock").apply {
                setReferenceCounted(false)
            }
        }
        try {
            wakeLock?.acquire(24 * 60 * 60 * 1000L /* 24 hours */)
            Log.d("CaffeineJitterEngine", "Acquired PARTIAL_WAKE_LOCK for jitter scheduler")
        } catch (e: Exception) {
            Log.e("CaffeineJitterEngine", "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("CaffeineJitterEngine", "Released PARTIAL_WAKE_LOCK")
            }
        } catch (_: Exception) {}
    }
}
