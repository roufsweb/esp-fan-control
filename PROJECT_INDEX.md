# ESP Fan Control — Project File Index & Directory Architecture

This index serves as the single source of truth for repository structure, tracking the location, purpose, and contents of all files in this project.

---

## 1. Directory Tree Overview

```
esp-fan-controll/
│
├── .agents/                               # Antigravity agent configuration & skills
│   ├── rules/
│   │   └── ui-and-hardware-rules.md       # Contextual project rules
│   └── skills/                            # Specialized on-demand project skills
│       ├── android-ui-ux-design/
│       │   └── SKILL.md                   # Google M3, rounded, subtle colors, zero emojis, toggle-first
│       ├── sensitive-info-guard/
│       │   └── SKILL.md                   # Secret scanning, credential redaction, and leak prevention
│       ├── skill-creator/
│       │   └── SKILL.md                   # Dynamic generation of new project skills
│       ├── skill-optimizer/
│       │   └── SKILL.md                   # Skill audit, deduplication, and optimization
│       └── project-summary-manager/
│           └── SKILL.md                   # Summary tracking, changelog, and experiment ledger
│
├── firmware/                              # ESP32-C3 Mini Embedded Firmware
│   ├── platformio.ini                     # PlatformIO environment config for ESP32-C3
│   ├── firmware_merged.bin                # Unified single flashable binary (offset 0x0) for mobile OTG flashing
│   ├── include/
│   │   ├── fan_config.h                   # GPIO mappings (GPIO 3 Tach, GPIO 4 PWM), 25kHz constants
│   │   ├── pwm_controller.h               # LEDC PWM timer and channel abstraction
│   │   ├── tachometer.h                   # Pulse counter / interrupt RPM measurement
│   │   └── ble_service.h                  # Bluetooth Low Energy GATT service definitions
│   └── src/
│       ├── main.cpp                       # Application entry point, setup, and loop
│       ├── pwm_controller.cpp             # 25kHz duty cycle adjustment implementation
│       ├── tachometer.cpp                 # Debounced interrupt pulse accumulation & RPM math
│       └── ble_service.cpp                # BLE server, characteristics, and notifications
│
├── android/                               # Android Native Application (Jetpack Compose)
│   ├── build.gradle.kts                   # Project-level build configuration
│   ├── app/
│   │   ├── build.gradle.kts               # App-level dependencies (Compose, Material3, BLE)
│   │   └── src/main/
│   │       ├── AndroidManifest.xml        # Permissions (Bluetooth, Wi-Fi, Internet)
│   │       ├── assets/
│   │       │   └── firmware_merged.bin    # Bundled firmware image for in-app or offline extraction
│   │       ├── res/                       # Drawables (Vector XML only, NO emojis), strings, themes
│   │       │   ├── drawable/              # Clean Material Vector icons
│   │       │   ├── values/                # Strings (concise, zero broad copy), themes
│   │       │   └── values-night/          # Night theme overrides
│   │       └── java/com/espfancontrol/
│   │           ├── MainActivity.kt        # Edge-to-edge Compose activity
│   │           ├── ui/
│   │           │   ├── theme/
│   │           │   │   ├── Color.kt       # Subtle pastel tonal palettes (high contrast ratio)
│   │           │   │   ├── Theme.kt       # Light/Dark mode MaterialTheme setup
│   │           │   │   ├── Type.kt        # Clean Google Sans / Roboto typography
│   │           │   │   └── Shape.kt       # Rounded corner definitions (16dp-28dp pill shapes)
│   │           │   ├── components/
│   │           │   │   ├── RpmGauge.kt    # Minimalist circular / arc tachometer readout
│   │           │   │   ├── SpeedSlider.kt # Rounded tactile PWM slider
│   │           │   │   ├── PresetToggles.kt # Segmented button / chips for Quick Modes
│   │           │   │   ├── StatusPill.kt  # Clean connection indicator pill
│   │           │   │   ├── DeviceConnectionCard.kt # BLE device discovery, diagnostic tips & connect card
│   │           │   │   └── UsbFlasherCard.kt # In-app USB-OTG firmware flasher card with progress
│   │           │   └── screens/
│   │           │       └── DashboardScreen.kt # Main fan control dashboard screen
│   │           ├── data/
│   │           │   ├── ble/               # Android BLE Scanner & GATT client
│   │           │   │   └── BleManager.kt
│   │           │   ├── usb/               # Direct on-device USB-OTG ESP32 ROM flasher
│   │           │   │   └── Esp32Flasher.kt
│   │           │   └── model/             # Fan telemetry and state models
│   │           │       └── FanState.kt
│   │           └── viewmodel/
│   │               └── FanViewModel.kt    # UI State management & command dispatcher
│
├── docs/                                  # GitHub Pages Web Portal & Deployment
│   ├── index.html                         # Material 3 landing page with zero-redirect in-browser WebSerial flasher
│   ├── esptool-bundle.js                  # Standalone in-browser WebSerial ESP32 ROM flasher engine
│   ├── firmware_merged.bin                # Unified single-binary firmware image for web flashing
│   └── app-debug.apk                      # Direct downloadable Android companion app APK
│
├── .gitignore                             # Git exclusion list (build caches, keystores, local properties)
├── README.md                              # Main project overview, wiring diagram, quickstart & index
├── AGENTS.md                              # Agent roles, responsibilities, and pair-programming protocol
├── GEMINI.md                              # Core workspace instructions, hardware pinouts, UI constraints
├── FEATURES.md                            # Comprehensive feature matrix, checklist, and verification status
├── PROJECT_INDEX.md                       # This comprehensive file index
└── PROJECT_SUMMARY.md                     # Cumulative summary of attempts, successes, and failures
```

---

## 2. Detailed File Descriptions

### Governance & Workspace Configuration
| File Path | Description & Purpose | Status |
| :--- | :--- | :--- |
| `README.md` | Primary entry point: system architecture, wiring diagrams, features, quick start. | Active |
| `AGENTS.md` | Defines agent personas, capabilities, and cross-agent communication protocols. | Active |
| `GEMINI.md` | Persistent workspace guidelines: Pin mapping (GPIO 3, GPIO 4), zero emojis, Google M3 style, concise copy. | Active |
| `FEATURES.md` | Master feature inventory, verification checklist, and functional requirements. | Active |
| `PROJECT_INDEX.md` | Master catalog of every file, its exact path, role, and content structure. | Active |
| `PROJECT_SUMMARY.md` | Concise, expanding log of everything attempted, what succeeded, what failed, and key decisions. | Active |

### Antigravity Agent Skills (`.agents/skills/`)
| Skill Folder | Description & Capabilities |
| :--- | :--- |
| `android-ui-ux-design/` | Standardizes modern Google M3 design: rounded pill corners (16-28dp), subtle tonal colors, high-contrast viewing angles, dark/light modes, toggle-first interfaces, zero emojis, concise microcopy. |
| `skill-creator/` | Enables agents to author new `.agents/skills/<name>/SKILL.md` packages autonomously with standardized metadata and schemas. |
| `skill-optimizer/` | Audits existing skills for bloat, token efficiency, overlap, and updates outdated instructions. |
| `project-summary-manager/` | Orchestrates updates to `PROJECT_SUMMARY.md`, formatting entries into structured, rapid-glance summaries. |

### Firmware Source Files (`firmware/`)
| File Path | Content & Technical Role |
| :--- | :--- |
| `firmware/include/fan_config.h` | Defines GPIO 4 (PWM output) and GPIO 3 (Tachometer input), 25 kHz PWM frequency, LEDC timer config, and pulse-per-revolution constants (2 PPR). |
| `firmware/include/pwm_controller.h` | Header declaring LEDC PWM setup, speed percentage-to-duty-cycle mapping, and thermal floor limits. |
| `firmware/src/pwm_controller.cpp` | Implementation of 25 kHz PWM generation using ESP32-C3 hardware LEDC timers. |
| `firmware/include/tachometer.h` | Header declaring pulse counter ISR, timer window calculations, and RPM conversion formulas. |
| `firmware/src/tachometer.cpp` | GPIO 3 interrupt / PCNT driver with debouncing and formula: $\text{RPM} = \frac{\text{Pulses}}{2} \times \frac{60}{\Delta t}$. |
| `firmware/include/ble_service.h` | BLE GATT UUID definitions for Service, Fan Speed (Read/Write), and Live RPM (Notify). |
| `firmware/src/ble_service.cpp` | NimBLE / ESP-IDF BLE stack implementation handling mobile connections and live telemetry. |
| `firmware/src/main.cpp` | Main loop coordinating PWM commands, RPM reading intervals, and BLE notifications. |

### Android Application Files (`android/`)
| File Path | Content & Technical Role |
| :--- | :--- |
| `android/.../theme/Color.kt` | Material 3 tonal palettes: low-saturation pastels, deep dark mode surfaces, high contrast for viewing angles. |
| `android/.../theme/Shape.kt` | Rounded corner definitions: 16dp for cards, 28dp for buttons and pills. |
| `android/.../theme/Theme.kt` | Dynamic and custom theme switcher for Dark Mode and Light Mode. |
| `android/.../components/RpmGauge.kt` | Sleek, minimal digital/arc readout of live RPM. |
| `android/.../components/SpeedSlider.kt` | Rounded slider for 0–100% duty cycle with haptic feedback. |
| `android/.../components/PresetToggles.kt` | Segmented buttons / filter chips for Silent (25%), Balanced (50%), Boost (100%), and Auto. |
| `android/.../data/ble/BleManager.kt` | Android Bluetooth Low Energy manager for scanning, connecting, and subscribing to ESP32-C3 GATT. |
| `android/.../viewmodel/FanViewModel.kt` | StateFlow-driven ViewModel orchestrating UI events, connection states, and fan speed commands. |

