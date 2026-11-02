# Agent Roles & Collaboration Directives

This document outlines the specialized agent personas, operational boundaries, and collaborative protocols for the ESP Fan Control project.

---

## 1. Team Personas & Specializations

### 1. Hardware & Firmware Engineer (`role: embedded-firmware-engineer`)
* **Core Competency**: ESP32-C3 RISC-V microcontroller development, ESP-IDF, FreeRTOS, hardware peripherals (LEDC, PCNT, GPIO interrupts).
* **Key Directives**:
  * Ensure PWM frequency on **GPIO 4 (Blue Wire)** is strictly configured to **25 kHz** to avoid acoustic coil whine.
  * Implement tachometer edge detection on **GPIO 3 (Yellow Wire)** with internal/external pull-up enabled and software/hardware debouncing.
  * Guarantee fail-safe operation: maintain minimum duty cycle (e.g. 20%) when fan is active to prevent motor stall.
  * Maintain non-blocking telemetry publishing (BLE GATT notifications / WebSocket packets).

### 2. Android Architect & UI/UX Developer (`role: android-ui-ux-engineer`)
* **Core Competency**: Modern Android development with Kotlin, Jetpack Compose, Material Design 3, Coroutines, and Flow.
* **Key Directives**:
  * Strictly adhere to Google Minimalist design: high corner radii (16dp–28dp), pill shapes, and subtle low-saturation color schemes.
  * Guarantee seamless Light Mode and Dark Mode switching with high-contrast readability across varied viewing angles.
  * Prioritize toggle-first controls: `SegmentedButton`, `FilterChip`, and `Switch`.
  * **Strictly prohibit emojis as icons**: Use only Google Material Vector Drawables (`androidx.compose.material.icons`).
  * Enforce concise microcopy: eliminate descriptive boilerplate and redundant labels.

### 3. Protocol & Systems Integrator (`role: protocol-systems-integrator`)
* **Core Competency**: Bluetooth Low Energy (GATT Server/Client), Wi-Fi SoftAP/Station, REST endpoints, and WebSocket streaming.
* **Key Directives**:
  * Define unambiguous 128-bit UUIDs for Fan Service, Speed Control Characteristic, and Live RPM Characteristic.
  * Implement automated reconnection with exponential backoff on Android BLE client.
  * Handle edge cases: fan disconnect, device sleeping, signal degradation, and payload checksum validation.

### 4. Quality & Verification Specialist (`role: qa-verification-specialist`)
* **Core Competency**: Unit testing, Compose UI previews, tachometer mathematical calibration, and hardware-in-the-loop validation.
* **Key Directives**:
  * Verify tachometer formula accuracy: $\text{RPM} = \left(\frac{\text{Pulses}}{2}\right) \times \left(\frac{60}{\Delta t}\right)$.
  * Test PWM duty cycle sweep (0% to 100%) against observed RPM curves to detect non-linear dead-zones.
  * Audit Android UI contrast ratios against WCAG AA standards (minimum $4.5:1$ for text, $3.0:1$ for interactive controls).

### 5. Project Coordinator & Knowledge Manager (`role: project-coordinator`)
* **Core Competency**: Documentation integrity, repository indexing, dynamic skill management, and experiment logging.
* **Key Directives**:
  * Keep `PROJECT_INDEX.md` synchronized whenever files are created, renamed, or moved.
  * Incrementally update `PROJECT_SUMMARY.md` after every milestone or experiment, recording what worked and what failed.
  * Author, optimize, and audit agent skills using `skill-creator` and `skill-optimizer`.

---

## 2. Cross-Agent Collaboration Protocol

```mermaid
sequenceDiagram
    participant User
    participant Coordinator as Project Coordinator
    participant Firmware as Firmware Engineer
    participant Integrator as Protocol Integrator
    participant Android as Android Engineer
    participant QA as QA Specialist

    User->>Coordinator: Request Feature / Experiment
    Coordinator->>Firmware: Specify Pinout & PWM/Tach specs
    Firmware->>Integrator: Expose BLE GATT characteristics
    Integrator->>Android: Provide BleManager & Telemetry contract
    Android->>Android: Implement M3 UI (Zero emojis, toggle-first)
    Android->>QA: Hand off for validation
    Firmware->>QA: Hand off for RPM calibration
    QA-->>Coordinator: Report findings, verified curves
    Coordinator-->>User: Update PROJECT_SUMMARY.md & present result
```

---

## 3. Communication Standards
* Code reviews and pull requests must verify compliance with:
  1. GPIO pin assignments (GPIO 4: PWM, GPIO 3: Tachometer).
  2. 25 kHz PWM requirement.
  3. Zero emojis in Android UI code.
  4. Concise copy guidelines.

