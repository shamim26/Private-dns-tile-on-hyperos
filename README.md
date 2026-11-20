# Private DNS Tile for HyperOS & Android

A lightweight, standalone Quick Settings (Control Center) tile to toggle Private DNS on and off with a single tap on Xiaomi HyperOS, MIUI, and standard Android devices.

---

## ✨ Features

- **Dynamic DNS Provider:** Automatically toggles whatever Private DNS provider is configured in your Android Settings (e.g. `dns.adguard-dns.com`, `one.one.one.one`, `dns.google`, NextDNS, etc.). You can change the DNS hostname in Settings anytime without reconfiguring the tile.
- **Hidden from Launcher:** No unnecessary app drawer icon. The app runs cleanly in the background as a system utility tile.
- **Long-Press Configuration:** Long-pressing the Quick Settings tile opens the dashboard to check your current DNS status and provider.
- **Root Fallback:** Seamlessly toggles via root (`su`) if `WRITE_SECURE_SETTINGS` hasn't been granted yet.
- **HyperOS Compatible:** Built specifically to solve the missing Private DNS switch in Xiaomi HyperOS / MIUI Control Center.
- **Super Lightweight:** Native Android implementation (~15 KB APK size) with zero third-party tracking or bloat.

---

## 📲 Installation & Setup

### 1. Install the APK
Download [PrivateDnsTile.apk](PrivateDnsTile.apk) from this repository and install it on your device.

---

### 2. Grant Permissions & Add the Tile

#### Method A: Root (KernelSU / Magisk / APatch) — *Recommended*
1. Open your root manager (**KernelSU**, **ResuKisu**, or **Magisk**).
2. Under the Superuser list, grant root to **Private DNS**.
3. Open **Termux** or **MT Manager** and run this single command to inject the tile into your HyperOS Control Center:
   ```bash
   su -c "settings put secure sysui_qs_tiles \"$(settings get secure sysui_qs_tiles),custom(com.shamim.privatedns/.PrivateDnsTileService)\" && pm grant com.shamim.privatedns android.permission.WRITE_SECURE_SETTINGS"
   ```

#### Method B: Via Computer (ADB)
If you prefer not using root:
1. Enable **USB Debugging** and **USB Debugging (Security settings)** in Developer Options.
2. Connect your phone to your PC and run:
   ```bash
   adb shell pm grant com.shamim.privatedns android.permission.WRITE_SECURE_SETTINGS
   ```
3. Add the tile to your active Control Center:
   ```bash
   adb shell "settings put secure sysui_qs_tiles \"\$(settings get secure sysui_qs_tiles),custom(com.shamim.privatedns/.PrivateDnsTileService)\""
   ```

---

## 🛠️ Usage

- **Tap the tile:** Toggles Private DNS between **Active** (uses your configured DNS) and **Off**.
- **Check Status:** When active, the tile subtitle shows your active DNS hostname (e.g., `dns.adguard-dns.com`).
- **Open Preferences:** Tap and hold (long-press) the tile in Control Center to open the app dashboard or jump straight to Android's network settings.

---

## 🔨 Building from Source

To build the APK locally from source on Windows:

### Requirements:
- Java JDK 17+
- Android SDK Build-Tools 33+ (with `aapt2`, `d8`, `zipalign`, and `apksigner`)
- Android Platform SDK (`android-34` or higher)

### Build Command:
```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```
The output APK will be generated at `./PrivateDnsTile.apk`.

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).
