package net.artkitty.kvmeowboard.hid

import android.content.Context
import android.net.Uri
import net.artkitty.kvmeowboard.ui.DeckThemeMode
import net.artkitty.kvmeowboard.ui.HostOs
import net.artkitty.kvmeowboard.ui.QuickMacroId
import org.json.JSONArray
import org.json.JSONObject

object KvMeowBackupManager {

    private const val BACKUP_SCHEMA_VERSION = 1

    fun exportToJsonString(context: Context): String {
        val prefs = context.getSharedPreferences("kvmeowboard_prefs", Context.MODE_PRIVATE)

        val root = JSONObject().apply {
            put("app", "KVMeowboard")
            put("schema_version", BACKUP_SCHEMA_VERSION)
            put("exported_at_epoch_ms", System.currentTimeMillis())

            put("theme_mode", prefs.getString("theme_mode", DeckThemeMode.DARK_KAWAII.name))
            put("active_host_id", prefs.getString("active_host_id", "win11"))
            put("show_third_slot", prefs.getBoolean("show_third_slot", false))

            // Bonded host MAC mappings & Slot 3 metadata
            put("hosts", JSONObject().apply {
                put("mac_win11", prefs.getString("mac_win11", "") ?: "")
                put("mac_macbook", prefs.getString("mac_macbook", "") ?: "")
                put("mac_slot3", prefs.getString("mac_slot3", "") ?: "")
                put("label_slot3", prefs.getString("label_slot3", "Host 3") ?: "Host 3")
                put("os_slot3", prefs.getString("os_slot3", HostOs.LINUX.name) ?: HostOs.LINUX.name)
            })

            // Per-host Jiggle toggles
            put("jiggle", JSONObject().apply {
                put("jiggle_win11", prefs.getBoolean("jiggle_win11", false))
                put("jiggle_mac", prefs.getBoolean("jiggle_mac", false))
                put("jiggle_slot3", prefs.getBoolean("jiggle_slot3", false))
            })

            // Built-in Quick-Macro Bar visibility & custom labels
            val enabledMacros = prefs.getStringSet("enabled_macros", null)
                ?: QuickMacroId.entries.map { it.name }.toSet()
            put("enabled_macros", JSONArray(enabledMacros.toList()))

            val macroLabelsObj = JSONObject()
            QuickMacroId.entries.forEach { macro ->
                val customLabel = prefs.getString("macro_label_${macro.name}", null)
                if (!customLabel.isNullOrBlank()) {
                    macroLabelsObj.put(macro.name, customLabel)
                }
            }
            put("custom_macro_labels", macroLabelsObj)

            // User-defined custom HID macros
            val customMacros = CustomHidMacro.fromJsonString(prefs.getString("custom_hid_macros", null))
            put("custom_hid_macros", JSONArray(CustomHidMacro.toJsonString(customMacros)))
        }

        return root.toString(2)
    }

    fun writeBackupToUri(context: Context, uri: Uri): Result<Unit> = runCatching {
        val jsonPayload = exportToJsonString(context)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(jsonPayload.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: error("Unable to open output stream for backup URI")
    }

    fun restoreFromUri(context: Context, uri: Uri): Result<Unit> = runCatching {
        val rawJson = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("Unable to read backup file")

        importFromJsonString(context, rawJson).getOrThrow()
    }

    fun importFromJsonString(context: Context, rawJson: String): Result<Unit> = runCatching {
        val root = JSONObject(rawJson)
        require(root.optString("app") == "KVMeowboard") {
            "Invalid backup file: missing KVMeowboard signature"
        }

        val prefs = context.getSharedPreferences("kvmeowboard_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        root.optString("theme_mode").takeIf { it.isNotBlank() }?.let { rawTheme ->
            if (runCatching { DeckThemeMode.valueOf(rawTheme) }.isSuccess) {
                editor.putString("theme_mode", rawTheme)
            }
        }

        root.optString("active_host_id").takeIf { it.isNotBlank() }?.let {
            editor.putString("active_host_id", it)
        }

        if (root.has("show_third_slot")) {
            editor.putBoolean("show_third_slot", root.getBoolean("show_third_slot"))
        }

        root.optJSONObject("hosts")?.let { hosts ->
            editor.putString("mac_win11", hosts.optString("mac_win11").ifBlank { null })
            editor.putString("mac_macbook", hosts.optString("mac_macbook").ifBlank { null })
            editor.putString("mac_slot3", hosts.optString("mac_slot3").ifBlank { null })
            hosts.optString("label_slot3").takeIf { it.isNotBlank() }?.let {
                editor.putString("label_slot3", it)
            }
            hosts.optString("os_slot3").takeIf { it.isNotBlank() }?.let { rawOs ->
                if (runCatching { HostOs.valueOf(rawOs) }.isSuccess) {
                    editor.putString("os_slot3", rawOs)
                }
            }
        }

        root.optJSONObject("jiggle")?.let { jiggle ->
            if (jiggle.has("jiggle_win11")) editor.putBoolean("jiggle_win11", jiggle.getBoolean("jiggle_win11"))
            if (jiggle.has("jiggle_mac")) editor.putBoolean("jiggle_mac", jiggle.getBoolean("jiggle_mac"))
            if (jiggle.has("jiggle_slot3")) editor.putBoolean("jiggle_slot3", jiggle.getBoolean("jiggle_slot3"))
        }

        root.optJSONArray("enabled_macros")?.let { arr ->
            val validNames = buildSet {
                for (i in 0 until arr.length()) {
                    val name = arr.optString(i)
                    if (runCatching { QuickMacroId.valueOf(name) }.isSuccess) add(name)
                }
            }
            editor.putStringSet("enabled_macros", validNames)
        }

        root.optJSONObject("custom_macro_labels")?.let { labelsObj ->
            QuickMacroId.entries.forEach { macro ->
                val custom = labelsObj.optString(macro.name)
                if (custom.isNotBlank()) {
                    editor.putString("macro_label_${macro.name}", custom)
                } else {
                    editor.remove("macro_label_${macro.name}")
                }
            }
        }

        root.optJSONArray("custom_hid_macros")?.let { customArr ->
            val validated = CustomHidMacro.fromJsonString(customArr.toString())
            editor.putString("custom_hid_macros", CustomHidMacro.toJsonString(validated))
        }

        editor.apply()
    }
}
