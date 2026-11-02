# Workspace Directives & Design Constraints

This document defines global rules and constraints that apply unconditionally across this repository.

---

## 1. Hardware Architecture & Wiring

* **Microcontroller**: ESP32-C3 Mini (RISC-V single-core).
* **Fan**: 4-Wire Standard Laptop Fan (Red, Black, Yellow, Blue).
* **Pin Mapping**:
  * **GPIO 4**: Connected to **Blue Wire** (PWM Control). Must use ESP32 LEDC peripheral operating at **25 kHz** (standard Intel 4-wire PWM spec; range 21–28 kHz). Do not use default 1 kHz PWM.
  * **GPIO 3**: Connected to **Yellow Wire** (Tachometer / Speed Sense). Fan provides open-collector pulses (2 pulses per revolution). Internal pull-up (`INPUT_PULLUP`) must be enabled on GPIO 3.
  * **Red Wire**: $V_{cc}$ (+5V or +12V power rail, depending on fan rating; never power 12V fan directly from ESP32 3.3V rail).
  * **Black Wire**: Ground (GND). Must share common ground with ESP32-C3.

---

## 2. Android UI/UX Standards

* **Aesthetic**: Google Material 3 / Pixel minimalist design.
* **Shapes**: Clean, high corner radius (cards: 16–24dp, buttons/chips: full pill or 28dp).
* **Color Palette**:
  * Subtle, low-saturation pastel tones (Material You dynamic tonal palettes).
  * Dark Mode and Light Mode supported out of the box.
  * Optimized for high contrast and extreme viewing angles without eye fatigue.
* **Control Paradigm**:
  * Toggle-first design: Use `Switch`, `SegmentedButton`, and `FilterChip` for controlling modes (e.g., Silent, Normal, Turbo, Auto).
  * Tactile sliders with clear percentage readouts.
* **Iconography Rule**:
  * **ZERO EMOJIS**: Never use emojis as icons or visual markers in UI components, buttons, or indicators.
  * Use strictly Google Material Icons / Vector Drawables (`androidx.compose.material.icons`).
* **Microcopy Rule**:
  * **Concise & Purposeful Copy**: Strictly avoid broad, conversational, or redundant descriptions that state the obvious (e.g. do not write "Click this button to control the fan" or "Visual toggle switch").
  * Use brief, clear labels: e.g., `"Speed"`, `"Silent"`, `"Turbo"`, `"Auto"`, `"1,450 RPM"`, `"Connected"`.

---

## 3. Project Documentation Rules

* Every code addition or structural change must be reflected in `PROJECT_INDEX.md`.
* Every experiment, working test, or failed attempt must be documented immediately in `PROJECT_SUMMARY.md`.
* New skills must follow the standard Antigravity skill structure and be placed in `.agents/skills/<name>/SKILL.md`.

