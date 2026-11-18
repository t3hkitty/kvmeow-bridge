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

## ⚖️ Permissive Open-Source Foundations
* Modeled on [DeskHop](https://github.com/DeskHop) & [LanMouse](https://github.com/feschber/lanmouse) Bluetooth HID loops (MIT / Permissive FOSS).
* Zero host drivers required — works natively on Windows, macOS, Linux, ChromeOS, and Android.
