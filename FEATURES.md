# ESP Fan Control — Feature Matrix & Verification Checklist

This document catalogs all planned, developed, and verified features for the ESP32-C3 4-Wire Fan Control system and its Android companion app. It functions as both a technical specification and a live verification checklist.

---

## 1. Feature Categories & Specification Matrix

| ID | Feature Name | Description & Technical Scope | Target Layer | Verification Status |
| :--- | :--- | :--- | :--- | :--- |
| **CONN-01** | **Instant Connect (Mi Band Style)** | Sub-300ms connection without manual pairing dialogues. Uses open "Just-Works" security and hardware UUID filtering. | Android & BLE | 🔲 Planned |
| **CONN-02** | **Zero-Lag Command Dispatch** | `WRITE_TYPE_NO_RESPONSE` packet transmission for immediate slider/preset execution with zero round-trip ACK latency. | Android & BLE | 🔲 Planned |
| **CONN-03** | **Auto-Reconnection Engine** | Exponential backoff reconnect logic ($1\text{s}, 2\text{s}, 4\text{s}, 8\text{s}$) upon signal loss or ESP32 reboot. | Android BLE Client | 🔲 Planned |
| **CONN-04** | **Battery-Saver Connection Interval** | Dynamic parameter negotiation: switches from $7.5\text{ms}$ burst interval during connect to $100\text{ms}$ interval + Slave Latency 4 during steady state. | ESP32 NimBLE | 🔲 Planned |
| **FAN-01** | **25 kHz High-Frequency PWM** | Hardware LEDC peripheral on **GPIO 4 (Blue Wire)** configured strictly to 25 kHz to eliminate acoustic coil whine. | ESP32 Firmware | 🔲 Planned |
| **FAN-02** | **Closed-Loop Tachometer RPM** | Hardware debounced interrupt on **GPIO 3 (Yellow Wire)** with `INPUT_PULLUP`. Formula: $\text{RPM} = \frac{\text{Pulses}}{2} \times \frac{60}{\Delta t}$. | ESP32 Firmware | 🔲 Planned |
| **FAN-03** | **Continuous Speed Slider** | Tactile 0–100% duty cycle control with real-time percentage feedback and 10% haptic tick stops. | Android UI | 🔲 Planned |
| **FAN-04** | **Stall Prevention Threshold** | Hardware safety clamp: When active, minimum speed is clamped to 20% to prevent motor stall and thermal damage. | ESP32 Firmware | 🔲 Planned |
| **MODE-01** | **Silent Preset (25%)** | One-tap toggle to quiet acoustic profile for night/study environments. | System-wide | 🔲 Planned |
| **MODE-02** | **Balanced Preset (50%)** | Balanced airflow-to-noise ratio for everyday productivity. | System-wide | 🔲 Planned |
| **MODE-03** | **Boost Preset (100%)** | Full-throttle cooling mode for heavy thermal loads. | System-wide | 🔲 Planned |
| **MODE-04** | **Auto Thermal Mode** | Closed-loop temperature-guided speed scaling. | System-wide | 🔲 Planned |
| **PWR-01** | **Master Power Toggle** | Clean rounded toggle switch to instantly start or stop fan operation. | System-wide | 🔲 Planned |
| **PWR-02** | **Reboot & Power-Cycle Persistence** | Persists fan state (Power, Mode, Speed) into ESP32 NVS flash storage. Automatically restores exact speed after full power interruption. | ESP32 NVS | 🔲 Planned |
| **PWR-03** | **Flash Wear-Leveling Debounce** | Debounces continuous slider writes by 1.5s before committing to flash, safeguarding the $100,000$-cycle flash lifespan. | ESP32 Firmware | 🔲 Planned |
| **NOTIF-01** | **Compact Foreground Notification** | Persistent lockscreen and notification shade banner showing live RPM and speed without launching the app. | Android OS | 🔲 Planned |
| **NOTIF-02** | **Inline Notification Toggles** | Direct action buttons (`[Power]`, `[Silent]`, `[Balanced]`, `[Boost]`) embedded in notification for sub-50ms switching. | Android OS | 🔲 Planned |
| **NOTIF-03** | **Quick Settings Tile (Control Center)** | Custom Android Quick Settings tile (`TileService`) enabling quick toggling from system status bar. | Android OS | 🔲 Planned |
| **UI-01** | **Google M3 Minimalist Styling** | High corner radius (16–28dp pill shapes), subtle low-saturation pastel tonal palettes, and generous negative space. | Android Compose | 🔲 Planned |
| **UI-02** | **Extreme Viewing-Angle Contrast** | Tonal surfaces and high-contrast typography ($\ge 4.5:1$ text, $\ge 3.0:1$ widgets) optimized for glancing from desk or vehicle mounts. | Android Compose | 🔲 Planned |
| **UI-03** | **Light & Dark Theme Engine** | Native seamless switching between soft Light Mode and deep Charcoal Dark Mode (`#121316` surface, not harsh `#000000`). | Android Compose | 🔲 Planned |
| **UI-04** | **Zero-Emoji Compliance** | Strictly prohibited emojis in all UI code; replaced entirely by official Google Material Vector Drawables. | Android Compose | 🔲 Planned |
| **UI-05** | **Concise Microcopy** | Purposeful, ultra-short labels (`"1,850 RPM"`, `"Boost"`, `"Connected"`); zero broad or redundant boilerplate. | Android Compose | 🔲 Planned |

---

## 2. User Testing & Verification Checklist

Use this checklist during hardware-in-the-loop and APK validation. Mark items as `[x]` as they pass testing.

### Bluetooth & Battery Efficiency
- [ ] **Instant Connection**: App connects to ESP32-C3 in under 500ms without OS pairing prompts.
- [ ] **Low Phone Battery Impact**: Phone background current drain remains $< 2\text{ mA}$ while paired.
- [ ] **Auto-Recovery**: Disconnecting the ESP32 power rail triggers automatic reconnection within 3 seconds of power restoration.

### Fan Speed & Tachometer Calibration
- [ ] **25 kHz Silent PWM**: Fan operates across all speeds with zero audible high-pitch coil whine.
- [ ] **True RPM Measurement**: Tachometer on GPIO 3 correctly reports RPM within $\pm 2\%$ of physical strobe/optical tachometer.
- [ ] **Stall Prevention**: Setting speed to any non-zero value below 20% maintains smooth spinning without motor jitter.

### State Persistence & Memory Endurance
- [ ] **Power Cycle Recovery**: Setting fan to 65% in Balanced mode, pulling the power plug, and plugging back in restores 65% speed within 1 second.
- [ ] **Flash Wear Protection**: Rapidly scrubbing the slider does not trigger continuous flash writes.

### Notification Bar & Control Center
- [ ] **Notification Microcopy**: Notification shows live RPM and mode cleanly without text truncation or clutter.
- [ ] **Direct Toggle Response**: Pressing `[Boost]` in the notification triggers 100% fan speed instantly without launching the main activity.
- [ ] **Quick Settings Tile**: Android Control Center tile reflects active connection and fan status.

### Visual & Usability Standards
- [ ] **No Emojis Present**: UI contains only crisp Material vector drawables.
- [ ] **Dark & Light Mode Readability**: Displays clear, readable metrics under direct ambient light and in dark rooms.
- [ ] **Wide-Angle Legibility**: Readouts remain legible when viewed from $45^\circ$ angles.

