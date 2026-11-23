# 🐾 Harvesting KDE Connect & Bluetooth HID for KVMeow / anymd Extension Bundling

> **Status**: Harvested & Specified  
> **Target Subsystems**: Bluetooth HID, Notification Listener, Clipboard Sync, Virtual Display / VNC 2nd Screen  
> **Format**: anymd markdown & cross-compatible extension specification  

---

## 1. Upstream Harvesting Ledger

| Subsystem | Source Component | Key Protocol / Primitive | KVMeow / anymd Extraction Target |
| :--- | :--- | :--- | :--- |
| **Bluetooth HID** | `BluetoothHidDevice` API + `HidReportDescriptor` | L2CAP SDP + Custom Descriptors (Page 0x07 scancodes, Page 0x01 mouse, Vendor 0xFF00 pipe) | Direct serverless HID daemon; idle keep-alive against sniff mode; multi-host switching |
| **Notifications** | `NotificationListenerService` (`NotificationReceiver.kt`) | `kdeconnect.notification` JSON payload (`id`, `appName`, `title`, `text`, `actions`) | Bidirectional notification reflection, remote dismiss, inline reply, and agent event trigger |
| **Clipboard** | `ClipboardManager` + `ClipboardPlugin.kt` | `kdeconnect.clipboard` & `kdeconnect.clipboard.connect` (`content`, `timestamp`) | Zero-touch clipboard bridge, password filter option, HID keystroke typing fallback for restricted PCs |
| **Virtual Display / 2nd Screen** | `MousePadPlugin` + VNC / Virtual Display Linker (headless Hyprland/WayVNC / Sunshine / RDP) | Absolute/Relative digitizer events + RFB / VNC framebuffer streaming | Android phone/tablet transformed into secondary display, drawing tablet, and external monitor |

---

## 2. Subsystem Capabilities & Architecture

### A. Tightened Bluetooth HID Stack
- **Serverless Host Control**: Target computers require no drivers, companion apps, or agents. Windows, macOS, Linux, Android, and ChromeOS identify KVMeow as a standard physical Bluetooth composite keyboard and mouse.
- **Sniff Mode Mitigation**: Sends non-destructive sub-pixel wake-up pulses (`sendWakeUpPulse`) to maintain active connection intervals without introducing latency.
- **Dedicated 10-Key Numpad & Navigation Scancodes**: Directly dispatches hardware-level scancodes for Numpad keys (`0x53`–`0x67`), Arrow keys (`0x4F`–`0x52`), Enter (`0x28`), and Escape (`0x29`).

### B. Notification Harvesting (`kdeconnect.notification`)
- **Capability**: Listens to Android system notifications via `NotificationListenerService`.
- **Packet Structure**:
```json
{
  "id": 172901234,
  "type": "kdeconnect.notification",
  "body": {
    "id": "com.slack:1024",
    "appName": "Slack",
    "ticker": "New message from Alice",
    "title": "Alice",
    "text": "Can you review the PR?",
    "isClearable": true,
    "actions": ["Reply", "Mark as read"]
  }
}
```
- **Bidirectional Control**: Supports `kdeconnect.notification.action` and `kdeconnect.notification.reply` to trigger quick replies and dismiss notifications from the host PC.

### C. Clipboard Synchronization (`kdeconnect.clipboard`)
- **Capability**: Monitors `ClipboardManager.OnPrimaryClipChangedListener`.
- **Packet Structure**:
```json
{
  "id": 172901235,
  "type": "kdeconnect.clipboard",
  "body": {
    "content": "git commit -m 'feat: tight bluetooth'",
    "timestamp": 1700774700000
  }
}
```
- **Fallback Keystroke Injection**: When a corporate PC blocks network clipboard transfer or third-party background software, the clipboard can be typed character-by-character via Bluetooth HID keyboard emulation.

### D. Virtual Display & Second Screen (VNC / Digitizer)
- **Capability**: Integrates with headless display servers (e.g., `wayvnc`, Sunshine/Moonlight, or `vnc_virtual_display_linker`) to present a low-latency extended monitor on Android.
- **Digitizer Mode**: Emulates pen pressure, stylus tilts, and multi-touch gestures directly into Wayland/X11 or Windows Ink.

---

## 3. Dozen Potential Repurposing Ideas (12 Creative Implementations)

1. **Autonomous AI Coding Desk Companion**: Phone sits under your monitor as a dedicated status deck and agent terminal, running anymd convert/search tools while typing commands into your terminal over HID.
2. **Aggressive Accountant Hot-Deck**: Full-screen 10-key calculator mode with tactile feedback that pipes numbers and formulas directly into Excel or Google Sheets on your PC.
3. **Zero-Trust KVM Switcher**: Switch between corporate locked-down laptops and personal Linux rigs with one tap, using local Bluetooth without network bridging or corporate software installation.
4. **Hardware Air-Mouse & Presentation Wand**: Leverage phone gyroscope/accelerometer to steer conference slide decks, laser pointers, and window switcher UI from across the room.
5. **Headless Server Emergency Console**: Plug an Android phone via Bluetooth/OTG to a headless homelab server or Raspberry Pi and use the phone's touchscreen as an instant monitor, trackpad, and terminal keyboard.
6. **Hardware 2FA / Password Vault Typist**: Pair with Bitwarden/KeePass to "type" complex 64-character master passwords over Bluetooth HID, completely bypassing host-side keyloggers and clipboard monitors.
7. **Second-Screen Chat & Telemetry Monitor**: Stream Slack/Discord or GPU/CPU thermals to the phone screen over virtual VNC display while dedicating 100% of your main desktop monitor to full-screen IDEs.
8. **Drawing Tablet & Signature Capture**: Emulate an absolute digitizer surface for signing PDFs and sketching diagrams directly into GIMP, Photoshop, or Excalidraw without buying an expensive graphics tablet.
9. **Gaming Macro Deck & Flight Sim Button Box**: Customize grid buttons to broadcast complex key combinations (e.g. `Ctrl+Alt+Shift+F12`) to flight simulators, OBS streaming setups, or DAW audio mixers.
10. **Silent Remote Camera & Teleprompter Controller**: Trigger teleprompters, scroll speeches, and trigger photo shutter clicks discreetly from your pocket or palm.
11. **Smart TV & Home Theater Controller**: Control Android TV, Apple TV, or Kodi media boxes via standard Bluetooth HID, with direct text entry for search fields instead of awkward remote D-pad typing.
12. **Cross-Device Clipboard Staging Ring**: Maintain a multi-slot clipboard buffer on the phone where you can collect snippets from your phone, laptop, and tablet, and deploy them one-by-one into target applications.
