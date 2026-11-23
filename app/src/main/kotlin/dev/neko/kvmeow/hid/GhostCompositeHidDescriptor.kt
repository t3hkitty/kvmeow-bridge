package dev.neko.kvmeow.hid

/**
 * 🐾 FOSS Composite 4-Report HID Descriptor 🐾
 *
 * Harvested and adapted from:
 * 1. ShubhamJ010/ghost-hid (MIT License) - Native Android Bluetooth Classic HID with 4-Report Descriptor:
 *    - Report ID 1: Standard 8-byte Keyboard (Modifier + Reserved + 6 Key Array)
 *    - Report ID 2: 4-byte Relative Mouse (Buttons + dx + dy + Wheel)
 *    - Report ID 3: 1-byte System Control (Power Down, Sleep, Wake Up)
 *    - Report ID 4: 2-byte Consumer Control (Play/Pause, Scan Next/Prev, Mute, Volume Up/Down)
 *
 * 2. ToxicOrca/ghostboard-android-bluetooth-mouse-keyboard (MIT License):
 *    - Background-threaded BluetoothHidManager dispatch
 *    - Idle wake-up pulse to bypass Bluetooth sniff-mode latency
 *
 * Both upstream repositories are licensed under the MIT License and fully attributed in README.md.
 */
object GhostCompositeHidDescriptor {
    val COMPOSITE_4_REPORT_DESCRIPTOR = byteArrayOf(
        // -------------------------------------------------------------
        // Report ID 1: Keyboard (8 bytes: Modifier, Reserved, 6 Keys)
        // -------------------------------------------------------------
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x01.toByte(), //   Report ID (1)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Key Codes)
        0x19.toByte(), 0xE0.toByte(), //   Usage Minimum (224 - Left Ctrl)
        0x29.toByte(), 0xE7.toByte(), //   Usage Maximum (231 - Right GUI)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x08.toByte(), //   Report Count (8)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute) - Modifier byte
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x81.toByte(), 0x01.toByte(), //   Input (Constant) - Reserved byte
        0x95.toByte(), 0x05.toByte(), //   Report Count (5)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x05.toByte(), 0x08.toByte(), //   Usage Page (LEDs)
        0x19.toByte(), 0x01.toByte(), //   Usage Minimum (Num Lock)
        0x29.toByte(), 0x05.toByte(), //   Usage Maximum (Kana)
        0x91.toByte(), 0x02.toByte(), //   Output (Data, Variable, Absolute) - LED report
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x75.toByte(), 0x03.toByte(), //   Report Size (3)
        0x91.toByte(), 0x01.toByte(), //   Output (Constant) - LED padding
        0x95.toByte(), 0x06.toByte(), //   Report Count (6)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(), //   Logical Maximum (101)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Key Codes)
        0x19.toByte(), 0x00.toByte(), //   Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(), //   Usage Maximum (101)
        0x81.toByte(), 0x00.toByte(), //   Input (Data, Array) - Key array (6 keys)
        0xC0.toByte(),               // End Collection

        // -------------------------------------------------------------
        // Report ID 2: Mouse (4 bytes: Buttons, dx, dy, Wheel)
        // -------------------------------------------------------------
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x02.toByte(), //   Report ID (2)
        0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
        0x05.toByte(), 0x09.toByte(), //     Usage Page (Buttons)
        0x19.toByte(), 0x01.toByte(), //     Usage Minimum (Button 1)
        0x29.toByte(), 0x03.toByte(), //     Usage Maximum (Button 3)
        0x15.toByte(), 0x00.toByte(), //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //     Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //     Report Size (1)
        0x95.toByte(), 0x03.toByte(), //     Report Count (3)
        0x81.toByte(), 0x02.toByte(), //     Input (Data, Variable, Absolute) - 3 Buttons
        0x75.toByte(), 0x05.toByte(), //     Report Size (5)
        0x95.toByte(), 0x01.toByte(), //     Report Count (1)
        0x81.toByte(), 0x01.toByte(), //     Input (Constant) - Button padding
        0x05.toByte(), 0x01.toByte(), //     Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //     Usage (X)
        0x09.toByte(), 0x31.toByte(), //     Usage (Y)
        0x09.toByte(), 0x38.toByte(), //     Usage (Wheel)
        0x15.toByte(), 0x81.toByte(), //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(), //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(), //     Report Size (8)
        0x95.toByte(), 0x03.toByte(), //     Report Count (3)
        0x81.toByte(), 0x06.toByte(), //     Input (Data, Variable, Relative) - X, Y, Wheel
        0xC0.toByte(),               //   End Collection
        0xC0.toByte(),               // End Collection

        // -------------------------------------------------------------
        // Report ID 3: System Control (1 byte: Power, Sleep, Wake)
        // -------------------------------------------------------------
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x80.toByte(), // Usage (System Control)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x03.toByte(), //   Report ID (3)
        0x15.toByte(), 0x01.toByte(), //   Logical Minimum (1)
        0x25.toByte(), 0x03.toByte(), //   Logical Maximum (3)
        0x09.toByte(), 0x81.toByte(), //   Usage (System Power Down)
        0x09.toByte(), 0x82.toByte(), //   Usage (System Sleep)
        0x09.toByte(), 0x83.toByte(), //   Usage (System Wake Up)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x00.toByte(), //   Input (Data, Array)
        0xC0.toByte(),               // End Collection

        // -------------------------------------------------------------
        // Report ID 4: Consumer Control (2 bytes: Media, Volume)
        // -------------------------------------------------------------
        0x05.toByte(), 0x0C.toByte(), // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(), // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x04.toByte(), //   Report ID (4)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // Logical Maximum (1023)
        0x19.toByte(), 0x00.toByte(), //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // Usage Maximum (1023)
        0x75.toByte(), 0x10.toByte(), //   Report Size (16)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x00.toByte(), //   Input (Data, Array)
        0xC0.toByte()                // End Collection
    )
}
