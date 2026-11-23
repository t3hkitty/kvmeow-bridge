package net.artkitty.kvmeowboard.ui

import android.bluetooth.BluetoothDevice

enum class HostOs {
    WINDOWS,
    MACOS,
    LINUX
}

data class HostSlot(
    val id: String,
    val label: String,
    val os: HostOs,
    val device: BluetoothDevice?,
    val jiggleEnabled: Boolean
)
