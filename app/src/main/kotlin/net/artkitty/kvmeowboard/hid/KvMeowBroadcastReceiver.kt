package net.artkitty.kvmeowboard.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import net.artkitty.kvmeowboard.ui.HostOs

class KvMeowBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SEND_HID = "net.artkitty.kvmeowboard.ACTION_SEND_HID"

        const val EXTRA_COMMAND = "cmd" // "text" | "key" | "chord" | "mouse" | "emoji" | "switch_host" | "release_all"
        const val EXTRA_TEXT = "text"
        const val EXTRA_MODIFIER = "mod" // Hex string ("0x01", "0x08") or int byte
        const val EXTRA_KEY = "key" // Hex string ("0x28", "0x52") or int byte
        const val EXTRA_CHORD_KEYS = "keys" // Comma-separated hex/int keycodes, e.g. "0x04,0x06"
        const val EXTRA_DX = "dx"
        const val EXTRA_DY = "dy"
        const val EXTRA_WHEEL = "wheel"
        const val EXTRA_BUTTONS = "buttons"
        const val EXTRA_OS = "os" // "windows" | "macos" | "linux"
        const val EXTRA_MAC = "mac" // Target Bluetooth device MAC address for "switch_host"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        Thread {
            try {
                val hid = BluetoothHidController(context.applicationContext).apply { registerApp() }
                Thread.sleep(150L) // Brief window for profile proxy attachment when receiver wakes cold

                val uri = intent.data
                val cmd = intent.getStringExtra(EXTRA_COMMAND)
                    ?: uri?.host
                    ?: return@Thread

                val os = parseOs(intent.getStringExtra(EXTRA_OS) ?: uri?.getQueryParameter("os"))

                when (cmd.lowercase()) {
                    "text", "type" -> {
                        val payload = intent.getStringExtra(EXTRA_TEXT)
                            ?: uri?.getQueryParameter("text")
                            ?: return@Thread
                        val mod = parseByte(intent.getStringExtra(EXTRA_MODIFIER) ?: uri?.getQueryParameter("mod"))
                        AsciiHidKeymap.typeAsciiString(payload, mod, hid)
                    }

                    "key" -> {
                        val mod = parseByte(intent.getStringExtra(EXTRA_MODIFIER) ?: uri?.getQueryParameter("mod"))
                        val key = parseByte(intent.getStringExtra(EXTRA_KEY) ?: uri?.getQueryParameter("key"))
                        hid.sendKeyTap(mod, key)
                    }

                    "chord" -> {
                        val mod = parseByte(intent.getStringExtra(EXTRA_MODIFIER) ?: uri?.getQueryParameter("mod"))
                        val rawKeys = intent.getStringExtra(EXTRA_CHORD_KEYS)
                            ?: uri?.getQueryParameter("keys")
                            ?: ""
                        val keyList = rawKeys.split(",")
                            .mapNotNull { token ->
                                val trimmed = token.trim()
                                if (trimmed.isEmpty()) null else parseByte(trimmed)
                            }
                        if (keyList.isNotEmpty()) {
                            hid.sendKeyChord(mod, keyList)
                        }
                    }

                    "mouse" -> {
                        val buttons = parseByte(intent.getStringExtra(EXTRA_BUTTONS) ?: uri?.getQueryParameter("buttons"))
                        val dx = parseByte(intent.getStringExtra(EXTRA_DX) ?: uri?.getQueryParameter("dx"))
                        val dy = parseByte(intent.getStringExtra(EXTRA_DY) ?: uri?.getQueryParameter("dy"))
                        val wheel = parseByte(intent.getStringExtra(EXTRA_WHEEL) ?: uri?.getQueryParameter("wheel"))
                        hid.sendMouseReport(buttons, dx, dy, wheel)
                    }

                    "emoji" -> {
                        val emojiText = intent.getStringExtra(EXTRA_TEXT)
                            ?: uri?.getQueryParameter("text")
                            ?: return@Thread
                        UnicodeHexEncoder.sendUnicodeText(emojiText, os, hid)
                    }

                    "switch_host" -> {
                        val targetMac = intent.getStringExtra(EXTRA_MAC)
                            ?: uri?.getQueryParameter("mac")
                            ?: return@Thread
                        switchToBondedMac(context.applicationContext, hid, targetMac)
                    }

                    "release_all" -> {
                        hid.releaseAllModifiers()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun switchToBondedMac(ctx: Context, hid: BluetoothHidController, mac: String) {
        val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        val target = adapter.bondedDevices?.firstOrNull {
            it.address.equals(mac.trim(), ignoreCase = true)
        } ?: return
        hid.switchHost(target)
    }

    private fun parseByte(raw: String?): Byte {
        if (raw.isNullOrBlank()) return 0x00
        val clean = raw.trim()
        return try {
            if (clean.startsWith("0x", ignoreCase = true)) {
                clean.substring(2).toInt(16).toByte()
            } else {
                clean.toInt().toByte()
            }
        } catch (_: NumberFormatException) {
            0x00
        }
    }

    private fun parseOs(raw: String?): HostOs = when (raw?.lowercase()) {
        "mac", "macos", "osx" -> HostOs.MACOS
        "win", "windows", "win11" -> HostOs.WINDOWS
        "linux" -> HostOs.LINUX
        else -> HostOs.WINDOWS
    }
}
