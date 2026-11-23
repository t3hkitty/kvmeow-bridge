package net.artkitty.kvmeowboard.hid

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CustomHidMacro(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val modifierMask: Byte,
    val usageCode: Byte
) {
    fun modifierHex(): String = "0x%02X".format(modifierMask.toInt() and 0xFF)
    fun usageHex(): String = "0x%02X".format(usageCode.toInt() and 0xFF)

    companion object {
        fun parseByteToken(raw: String): Byte? {
            val clean = raw.trim()
            if (clean.isEmpty()) return 0x00
            return try {
                val parsed = if (clean.startsWith("0x", ignoreCase = true)) {
                    clean.substring(2).toInt(16)
                } else {
                    clean.toInt(16)
                }
                if (parsed in 0..255) parsed.toByte() else null
            } catch (_: NumberFormatException) {
                null
            }
        }

        fun toJsonString(macros: List<CustomHidMacro>): String {
            val array = JSONArray()
            macros.forEach { m ->
                array.put(
                    JSONObject().apply {
                        put("id", m.id)
                        put("label", m.label)
                        put("mod", m.modifierMask.toInt() and 0xFF)
                        put("key", m.usageCode.toInt() and 0xFF)
                    }
                )
            }
            return array.toString()
        }

        fun fromJsonString(raw: String?): List<CustomHidMacro> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(
                            CustomHidMacro(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                label = obj.getString("label"),
                                modifierMask = obj.getInt("mod").toByte(),
                                usageCode = obj.getInt("key").toByte()
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}
