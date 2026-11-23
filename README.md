# 🐾 KVMeow Bridge (Kawaiian v3.1-Plump) 🐾

```text
       /\_/\
      ( o.o )   KVMeow Bridge: Dedicated Bluetooth HID Daemon & Multi-Host Switcher
       > ^ <    Zero-drop Bluetooth stack • Independent of IME keyboard lifecycle
```

## ✨ What KVMeow Bridge Does
* **Permanent Bluetooth Profile**: Runs as a lightweight Android Foreground Service keeping the Bluetooth HID device profile alive. Never flaps or drops when opening, closing, or switching IMEs.
* **1-Pairing Multi-PC Hub**: Your phone pairs to your PCs once. No cluttering the Bluetooth cache with multiple test app IDs.
* **Multi-Host Caffeine Jitter**: Broadcasts zero-impact sub-pixel relative mouse nudges (+1px, then -1px) to **all connected machines simultaneously**, keeping work & home laptops unlocked without installing any company-flagged scripts on target PCs.
* **Open IPC Command Bridge**: Exposes standard Android Intents for IME keyboards (`neko-keyboard`), Tasker, or automation tiles:
  - `dev.neko.kvmeow.ACTION_SEND_KEY` (modifiers + keycodes)
  - `dev.neko.kvmeow.ACTION_SEND_MOUSE` (buttons, relative dx/dy, wheel)
  - `dev.neko.kvmeow.ACTION_FLIP_HOST` (select active computer by MAC)
  - `dev.neko.kvmeow.ACTION_TOGGLE_CAFFEINE` (start/stop background jitter)

## ⚖️ Permissive Open-Source Foundations & Harvested References

`KVMeowboard` (KVMeow Bridge) layers custom multi-host switcher, background jiggler, and audio routing features onto battle-tested FOSS Android HID patterns:

1. **[ShubhamJ010/ghost-hid](https://github.com/ShubhamJ010/ghost-hid)** (`MIT License`):
   - Native Android `BluetoothHidDevice` SDP registration and 4-report composite HID descriptor (Report 1: 8-byte Keyboard, Report 2: 4-byte Relative Mouse, Report 3: 1-byte System Control, Report 4: 2-byte Consumer Control).
   - Driverless bonding on Windows 10/11, Apple macOS, Linux, and ChromeOS without desktop agent software.

2. **[ToxicOrca/ghostboard-android-bluetooth-mouse-keyboard](https://github.com/ToxicOrca/ghostboard-android-bluetooth-mouse-keyboard)** (`MIT License`):
   - Dedicated background-threaded HID dispatch pipeline.
   - Sniff-mode idle wake-up pulse to prevent latency spikes on first key/mouse interaction after sleep.

3. **[AppGround-io/bluetooth-keyboard-and-mouse-support](https://github.com/AppGround-io/bluetooth-keyboard-and-mouse-support)**:
   - UI styling inspiration, high-contrast non-blocking deck workspace, touchpad layout, and connectivity hardening.

4. **[KDE/kdeconnect-android](https://invent.kde.org/network/kdeconnect-android)** (`GPL-2.0-or-later`):
   - Protocol harvest for clipboard synchronization, remote mousepad packets, and notification mirroring (documented in `HARVEST_KDECONNECT_ANYMD.md`).

All custom additions—including the 3-theme engine (`Tron`, `Dark Kawaii`, `Bright Kawaii`), per-host zero-net micro-jitter engine, sticky modifier hold buttons, OxygenOS screenshot gesture suppression, 3-finger macOS swipe mapper, `^` 10-key accountant calculator, and dual-track meeting audio capture (`AudioRecord` + LAN/USB PCM audio bridge)—operate under the permissive open-source license terms of this project.
