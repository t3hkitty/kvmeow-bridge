package dev.neko.kvmeow.caffeine

import android.bluetooth.BluetoothDevice
import dev.neko.kvmeow.hid.BluetoothHidManager
import kotlinx.coroutines.*

/**
 * ☕ Multi-Host Caffeine Keep-Alive Engine ☕
 * Broadcasts zero-impact sub-pixel relative mouse nudges (+1px, then -1px)
 * or virtual F15 key events to ALL connected PC hosts simultaneously to prevent sleep/lockout.
 */
class CaffeineJitterEngine(private val hidManager: BluetoothHidManager) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var jitterIntervalMs: Long = 45_000L

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            var step = true
            while (isActive) {
                delay(jitterIntervalMs)
                val hosts = hidManager.getConnectedHosts()
                for (host in hosts) {
                    val nudge: Byte = if (step) 1 else -1
                    hidManager.sendMouseReport(0, nudge, 0, 0, target = host)
                    delay(20)
                    hidManager.sendMouseReport(0, 0, 0, 0, target = host)
                }
                step = !step
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun isActive(): Boolean = job?.isActive == true
}
