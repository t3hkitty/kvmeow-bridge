package dev.neko.kvmeow.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import dev.neko.kvmeow.hid.BluetoothHidManager
import dev.neko.kvmeow.hid.HidScancodes
import dev.neko.kvmeow.unicode.UnicodeInputEngine
import java.security.MessageDigest

/**
 * 🐾 KDE Connect / Host Clipboard Sync, Sticker & GIF Bridge 🐾
 *
 * Handles:
 * 1. Deduplicated bidirectional text clipboard sync with sha256 hash & debounce guards.
 * 2. Image / GIF / Sticker payload transport (e.g. KitStickerStudio / Mixtjis).
 * 3. Automated trigger of Cmd + V (macOS) or Ctrl + V (Windows/Linux) after clipboard staging.
 * 4. Graceful fallback to plain-text / Markdown URL when rich image paste is unsupported.
 */
class HostClipboardSyncBridge(
    private val context: Context,
    private val hidManager: BluetoothHidManager
) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var lastSentHash: String? = null
    private var lastReceivedHash: String? = null
    private var lastSyncTimestamp = 0L

    var onClipboardPacketToSend: ((content: String, timestamp: Long) -> Unit)? = null
    var onRemoteClipboardApplied: ((content: String) -> Unit)? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        handleLocalClipboardChanged()
    }

    fun start() {
        clipboardManager.addPrimaryClipChangedListener(clipListener)
    }

    fun stop() {
        clipboardManager.removePrimaryClipChangedListener(clipListener)
    }

    private fun handleLocalClipboardChanged() {
        val now = System.currentTimeMillis()
        if (now - lastSyncTimestamp < 350) return // Debounce guard

        val clip = clipboardManager.primaryClip ?: return
        if (clip.itemCount == 0) return

        val item = clip.getItemAt(0)
        val text = item.text?.toString() ?: item.coerceToText(context).toString()
        if (text.isEmpty()) return

        val hash = sha256(text)
        if (hash == lastReceivedHash || hash == lastSentHash) {
            return // Echo loop deduplication
        }

        lastSentHash = hash
        lastSyncTimestamp = now
        Log.d("HostClipboardSyncBridge", "Local clipboard changed: ${text.take(30)}... (hash: ${hash.take(8)})")
        onClipboardPacketToSend?.invoke(text, now)
    }

    /**
     * Called when a KDE Connect packet or remote sync packet arrives from the PC.
     */
    fun onRemoteClipboardReceived(content: String, timestamp: Long) {
        val hash = sha256(content)
        if (hash == lastSentHash) return // Deduplicate our own echo

        lastReceivedHash = hash
        lastSyncTimestamp = System.currentTimeMillis()

        try {
            val clip = ClipData.newPlainText("KVMeow Remote Clip", content)
            clipboardManager.setPrimaryClip(clip)
            onRemoteClipboardApplied?.invoke(content)
            Log.d("HostClipboardSyncBridge", "Applied remote clipboard to Android: ${content.take(30)}...")
        } catch (e: Exception) {
            Log.e("HostClipboardSyncBridge", "Failed to apply remote clipboard", e)
        }
    }

    /**
     * Staging an Image / GIF / Sticker onto clipboard & triggering OS paste:
     * Dispatches Cmd + V (Mac) or Ctrl + V (Windows/Linux) via Bluetooth HID!
     */
    fun stageAndPasteMedia(
        uri: Uri?,
        mimeType: String = "image/png",
        fallbackUrlOrText: String,
        targetOs: UnicodeInputEngine.TargetOs
    ) {
        try {
            if (uri != null) {
                val clip = ClipData.newUri(context.contentResolver, "Sticker", uri)
                clipboardManager.setPrimaryClip(clip)
            } else {
                val clip = ClipData.newPlainText("Sticker Fallback", fallbackUrlOrText)
                clipboardManager.setPrimaryClip(clip)
            }
        } catch (e: Exception) {
            Log.w("HostClipboardSyncBridge", "Failed to set URI clip, falling back to text", e)
            val clip = ClipData.newPlainText("Sticker Fallback", fallbackUrlOrText)
            clipboardManager.setPrimaryClip(clip)
        }

        // Trigger OS Paste via Bluetooth HID
        Thread {
            try {
                Thread.sleep(120) // Allow OS clipboard propagation
                triggerRemotePaste(targetOs)
            } catch (_: Exception) {}
        }.start()
    }

    /**
     * Direct text injection via Clipboard + Paste:
     * Useful for complex emojis, multi-line code snippets, or formulas.
     */
    fun pasteTextDirect(text: String, targetOs: UnicodeInputEngine.TargetOs) {
        try {
            val clip = ClipData.newPlainText("KVMeow Direct Paste", text)
            clipboardManager.setPrimaryClip(clip)
        } catch (e: Exception) {
            Log.e("HostClipboardSyncBridge", "Error setting clipboard for paste", e)
        }

        Thread {
            try {
                Thread.sleep(80)
                triggerRemotePaste(targetOs)
            } catch (_: Exception) {}
        }.start()
    }

    /**
     * Dispatches Cmd + V (Mac) or Ctrl + V (Win/Linux)
     */
    fun triggerRemotePaste(targetOs: UnicodeInputEngine.TargetOs) {
        when (targetOs) {
            UnicodeInputEngine.TargetOs.MACOS -> {
                // Cmd + V (Left GUI + KEY_V)
                hidManager.sendRawKeycode(HidScancodes.KEY_V, HidScancodes.MODIFIER_LEFT_GUI)
            }
            UnicodeInputEngine.TargetOs.WINDOWS,
            UnicodeInputEngine.TargetOs.LINUX -> {
                // Ctrl + V (Left Ctrl + KEY_V)
                hidManager.sendRawKeycode(HidScancodes.KEY_V, HidScancodes.MODIFIER_LEFT_CTRL)
            }
        }
    }

    companion object {
        fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
