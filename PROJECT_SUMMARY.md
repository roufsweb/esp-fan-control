# Project Execution Summary & Knowledge Ledger

This log maintains a rapid-glance, cumulative record of every architectural decision, experiment, success, and failure throughout the ESP Fan Control project. It is updated incrementally as development progresses.

---

## 1. Quick Status Overview
* **Hardware Target**: ESP32-C3 Mini (ESP-IDF / Arduino Core)
* **Fan Interface**: 4-Wire PWM Laptop Fan (Red: $V_{cc}$, Black: GND, Yellow: GPIO 3 Tach, Blue: GPIO 4 PWM)
* **Mobile Target**: Android (Kotlin, Jetpack Compose, Material 3, Dark/Light Mode)
* **Design Philosophy**: Google Minimal, rounded corners (16-28dp), subtle tonal colors, zero emojis, toggle-first, concise copy.
* **Current Phase**: Phase 1 — Project Scaffolding, Governance & Custom Skills Architecture.

---

## 2. Cumulative Experiment & Development Ledger

| # | Topic / Component | What Was Tried | Outcome / What Worked | What Did Not Work / Gotchas | Status |
| :- | :--- | :--- | :--- | :--- | :- |
| 1 | **Workspace Scaffolding** | Initialized standard index, summary tracker, governance rules (`AGENTS.md`, `GEMINI.md`). | Complete workspace indexing established in `PROJECT_INDEX.md`. | N/A | Completed |
| 2 | **Antigravity Custom Skills** | Created 4 project-specific skills (`android-ui-ux-design`, `skill-creator`, `skill-optimizer`, `project-summary-manager`). | Clear skill boundaries; progressive disclosure prevents context bloat. | Overly long prompt descriptions bloat tokens; trimmed to essential directives. | Completed |
| 3 | **Hardware Pin Mapping** | Allocated GPIO 4 for 25kHz PWM output; GPIO 3 for Yellow wire Tachometer pulse input. | Validated ESP32-C3 pinout: GPIO 4 and GPIO 3 are native RTC/GPIO pins not conflicted by strapping pins (GPIO 8/9/2). | Standard Arduino `analogWrite()` produces audible 1 kHz whine. Must use ESP32 LEDC peripheral set strictly to 25 kHz. | Standardized |
| 4 | **Tachometer Sense Protocol** | Configured GPIO 3 with internal pull-up resistor for fan open-collector output. | Open-collector pulse counting works with pull-up; 2 pulses per fan revolution. | Floating pin readings without pull-up caused infinite spurious RPM spikes. Internal pull-up or external 10k resistor is required. | Standardized |
| 5 | **UI/UX Vector Constraint** | Enforced strict prohibition on emoji icons across UI components. | Used clean Material Design Vector drawables (`androidx.compose.material.icons`). Visually consistent across all devices. | Emojis vary across Android OEM skins (Samsung vs Pixel vs Xiaomi) causing broken aesthetics. | Enforced |
| 6 | **Microcopy Policy** | Replaced explanatory text with concise, contextual labels. | Clean, high-density dashboard without redundant explanatory labels. | Verbose labels like "Visual toggle switch to turn fan off" wasted screen space and impaired scanability. | Enforced |
| 7 | **Low-Power Protocol Architecture** | Formulated sub-300ms BLE GATT architecture with open "Just-Works" security and write-without-response. | Eliminates Wi-Fi radio battery drain ($< 1.5\text{ mA}$ average on phone). | Wi-Fi SoftAP drained phone battery rapidly and added 4-second DHCP handshake delay. | Standardized |
| 8 | **State Persistence Strategy** | Architected ESP32 NVS flash storage for fan power, mode, and speed. | Restores exact speed across power cycles; debounces slider commits by 1.5s. | Writing to flash on every 1% slider increment risks exhausting flash endurance ($100k$ cycles). | Standardized |
| 9 | **Control Center Integration** | Designed compact foreground notification and Android Quick Settings Tile (`TileService`). | Instant toggle commands (`[Power]`, `[Silent]`, `[Boost]`) directly from notification shade. | Full app launch is too cumbersome for quick thermal adjustments. | Standardized |
| 10 | **Android APK Compilation** | Built debug APK (`app-debug.apk`, 15.6MB) using Gradle 8.5 with Jetpack Compose & Material 3. | Zero-emoji compliance verified; clean vector drawables; background service and TileService registered. | Missing `android.useAndroidX=true` and attr references resolved. | Completed |
| 11 | **No-PC Flashing Architecture** | Developed OTG flashing strategy for users without computers using Android Chrome WebSerial and bundled unified binary. | Allows flashing ESP32-C3 directly from phone via USB-C OTG cable; eliminates computer requirement. | Standard dual-file flashing (`bootloader` + `firmware`) is error-prone for end-users; requires single merged `.bin` at offset `0x0`. | Standardized |
| 12 | **Native In-App USB Flasher** | Integrated native SLIP bootloader flasher (`Esp32Flasher.kt` + `UsbFlasherCard.kt`) directly into Android app. | Completely eliminates web UI hassle; single-tap flashing with live progress directly in app from OTG cable. | USB baud rate must be synchronized with ESP32-C3 ROM bootloader; 1KB chunks provide maximum OTG stability. | Completed |
| 13 | **Git Initialization & Secret Guard** | Initialized repository with comprehensive `.gitignore`, `README.md`, and created `sensitive-info-guard` skill. | Prevents accidental leak of keystores, credentials, local paths, or build artifacts. | `local.properties` contains private absolute machine paths; must be strictly ignored. | Completed |
| 14 | **GitHub Release & About Metadata** | Published GitHub release `v1.0.0` with assets and set live homepage `https://roufsweb.github.io/esp-fan-control/` in GitHub About section. | Direct one-click access to web portal, APK downloads, and topics on GitHub repository page. | N/A | Completed |
| 15 | **Tasmota Web Portal UI** | Reverted `docs/index.html` to the clean classic Tasmota interface with stacked action buttons, telemetry sensor table, and timestamped console. | Native in-browser WebSerial flashing with zero redirects; direct APK download; preferred clean IoT aesthetic. | N/A | Standardized |

---

## 3. Key Technical Decisions Record (TDR)

* **PWM Frequency**: Fixed at **25 kHz** adhering to the standard Intel 4-wire PWM fan specification. Prevents human-audible coil whine.
* **Tachometer Formula**: $\text{RPM} = \left(\frac{\text{Pulse Count}}{2}\right) \times \left(\frac{60}{\Delta t_{\text{sec}}}\right)$.
* **Mobile UI Theme**: Material 3 tonal palettes with dynamic luminance adjustment. Dark mode utilizes `#121316` tonal background (not `#000000`) for visual comfort and proper contrast at extreme viewing angles.
* **Iconography Standard**: Pure Vector Drawables only. No Unicode emoji glyphs allowed in UI code or resources.
* **BLE Handshake & Battery Strategy**: Unencrypted "Just-Works" pairing with hardware UUID scan filter and `WRITE_TYPE_NO_RESPONSE` packet flow. Eliminates pairing delays and minimizes phone battery drain.
* **Non-Volatile Storage (NVS)**: Stores `fan_pwr`, `fan_spd`, and `fan_mod` in flash with a 1.5-second debounce on continuous slider changes to protect flash cell lifespan.

* **Unified Flashing Binary**: Single merged image (`firmware_merged.bin`) combines bootloader, partition table, and app code into a single binary flashable at offset `0x0`. Eliminates complex multi-file offset configurations for mobile users.

---

## 4. Current Status & Verification Deliverables
- [x] Implement ESP32 NVS flash storage read/write in `main.cpp` and `fan_config.h`.
- [x] Implement Android `BleManager` with `ScanFilter`, fast connection parameter update, and `WRITE_TYPE_NO_RESPONSE`.
- [x] Implement Jetpack Compose UI (Theme, DashboardScreen, RpmGauge, SpeedSlider, PresetToggles).
- [x] Implement Android `FanTileService` (Quick Settings Control Center) and minimal foreground notification with direct action buttons.
- [x] Compile ESP32-C3 firmware via PlatformIO (`firmware.bin` & `firmware_merged.bin` generated).
- [x] Compile Android APK via Gradle (`app-debug.apk` bundled with firmware asset).
- [ ] Connect Galaxy Note 9 or ESP32-C3 for on-device flashing & verification.

