package dev.neko.kvmeow.hid

/**
 * Extended USB HID Report Descriptors including Vendor-Defined bidirectional Data Pipe.
 * - Report 1: Keyboard
 * - Report 2: Mouse
 * - Report 3: Bidirectional Fast Raw Pipe (64-byte chunks for Clipboard & Notifications over BLE/Classic HID!)
 */
object ExtendedHidReportDescriptor {
    val COMBO_AND_PIPE_DESCRIPTOR = byteArrayOf(
        // Keyboard (Report ID 1)
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x06.toByte(),
        0xA1.toByte(), 0x01.toByte(),
        0x85.toByte(), 0x01.toByte(), // Report ID 1
        0x05.toByte(), 0x07.toByte(),
        0x19.toByte(), 0xE0.toByte(),
        0x29.toByte(), 0xE7.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x08.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x81.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x05.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x05.toByte(), 0x08.toByte(),
        0x19.toByte(), 0x01.toByte(),
        0x29.toByte(), 0x05.toByte(),
        0x91.toByte(), 0x02.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x03.toByte(),
        0x91.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x06.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x65.toByte(),
        0x05.toByte(), 0x07.toByte(),
        0x19.toByte(), 0x00.toByte(),
        0x29.toByte(), 0x65.toByte(),
        0x81.toByte(), 0x00.toByte(),
        0xC0.toByte(),

        // Mouse (Report ID 2)
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x02.toByte(),
        0xA1.toByte(), 0x01.toByte(),
        0x85.toByte(), 0x02.toByte(), // Report ID 2
        0x09.toByte(), 0x01.toByte(),
        0xA1.toByte(), 0x00.toByte(),
        0x05.toByte(), 0x09.toByte(),
        0x19.toByte(), 0x01.toByte(),
        0x29.toByte(), 0x03.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x03.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0x75.toByte(), 0x05.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x01.toByte(),
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x30.toByte(),
        0x09.toByte(), 0x31.toByte(),
        0x09.toByte(), 0x38.toByte(),
        0x15.toByte(), 0x81.toByte(),
        0x25.toByte(), 0x7F.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x03.toByte(),
        0x81.toByte(), 0x06.toByte(),
        0xC0.toByte(),
        0xC0.toByte(),

        // 🐾 Vendor-Defined Raw Data Pipe (Report ID 3) 🐾
        // Used for bidirectional Clipboard Sync & Notification Push over Bluetooth!
        0x06.toByte(), 0x00.toByte(), 0xFF.toByte(), // Usage Page (Vendor-Defined 0xFF00)
        0x09.toByte(), 0x01.toByte(),               // Usage (Vendor 1)
        0xA1.toByte(), 0x01.toByte(),               // Collection (Application)
        0x85.toByte(), 0x03.toByte(),               //   Report ID (3)
        0x09.toByte(), 0x02.toByte(),               //   Usage (Data In)
        0x15.toByte(), 0x00.toByte(),               //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), //   Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(),               //   Report Size (8 bits)
        0x95.toByte(), 0x40.toByte(),               //   Report Count (64 bytes)
        0x81.toByte(), 0x02.toByte(),               //   Input (Data, Variable, Absolute)
        0x09.toByte(), 0x03.toByte(),               //   Usage (Data Out)
        0x15.toByte(), 0x00.toByte(),
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(),
        0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x40.toByte(),
        0x91.toByte(), 0x02.toByte(),               //   Output (Data, Variable, Absolute)
        0xC0.toByte()                                // End Collection
    )
}
