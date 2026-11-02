---
name: android-ui-ux-design
description: >-
  Enforces Google Material 3 design standards for the Android app. Mandates rounded
  corners, subtle tonal palettes, high-contrast viewing angles, seamless dark/light modes,
  toggle-first controls, strict prohibition of emojis as icons, and concise microcopy.
---

# Android UI/UX Design System Specification

Use this skill when designing, reviewing, or implementing any Android UI layout, composable, theme, or component in this project.

---

## 1. Core Visual Principles

1. **Google Minimalist Aesthetic**:
   * Clean, uncrowded layouts with generous negative space.
   * Soft elevation using tonal surface color tinting instead of heavy drop shadows.
   * Fluid transitions and tactile feedback.

2. **Rounded Corners & Geometry**:
   * **Cards & Containers**: `16dp` to `24dp` corner radius (`RoundedCornerShape(20.dp)`).
   * **Buttons, Badges, & Chips**: Full pill shape (`RoundedCornerShape(percent = 50)` or `28.dp`).
   * **Sliders & Gauges**: Rounded track ends and pill thumb shapes.

3. **Subtle Color Science & Viewing Angle Optimization**:
   * Avoid oversaturated or neon colors. Use soft, desaturated pastel accents inspired by Material You.
   * Optimize for extreme viewing angles (e.g. mobile mounted on a desk or car cradle) by maintaining high luminance contrast between surface containers and text/indicators.
   * Comply with WCAG AA standards: Minimum $4.5:1$ contrast for standard text, $3.0:1$ for graphical widgets.

---

## 2. Theme Architecture (Light & Dark Mode)

Every UI element must dynamically resolve from `MaterialTheme.colorScheme`. Hardcoded color hexes in composables are strictly prohibited.

```kotlin
// Subtle Tonal Color System Definition
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2B5B84),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD2E4F5),
    onPrimaryContainer = Color(0xFF0F3254),
    surface = Color(0xFFF9F9FB),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA0C9F8),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF1B4870),
    onPrimaryContainer = Color(0xFFD2E4F5),
    surface = Color(0xFF121316),       // Soft dark surface, avoiding harsh pure #000000
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF262A30),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099)
)
```

---

## 3. Strict Iconography Rules: ZERO EMOJIS

> [!CAUTION]
> **DO NOT USE EMOJIS AS ICONS**: Emojis (e.g. 🌀, ❄️, ⚡, 🔇, 📴) are rendered inconsistently across device vendors and break the clean Google design standard.

* Use **ONLY** official Google Material Icons or scalable Vector Drawables:
  * Fan / Cooling: `Icons.Outlined.Air`, `Icons.Filled.ModeFan` (or custom SVG vector fan drawable)
  * Speed / Boost: `Icons.Outlined.Speed`, `Icons.Outlined.Bolt`
  * Silent / Quiet: `Icons.Outlined.VolumeMute`, `Icons.Outlined.Bedtime`
  * Auto / Temperature: `Icons.Outlined.Thermostat`, `Icons.Outlined.Tune`
  * Power: `Icons.Outlined.PowerSettingsNew`
  * Bluetooth: `Icons.Outlined.Bluetooth`, `Icons.Outlined.BluetoothConnected`

---

## 4. Toggle-First Control Paradigm

Prioritize toggle controls over deeply nested menus or popups:
* **Speed Presets**: Use `SegmentedButton` or `FilterChip` rows for instantaneous switching:
  * `[ Silent ]` (25%)
  * `[ Balanced ]` (50%)
  * `[ Boost ]` (100%)
  * `[ Auto ]` (Closed-loop temperature curve)
* **Master Fan State**: High-contrast rounded `Switch` with tactile state indicators.
* **Continuous Adjustment**: Smooth, rounded `Slider` with haptic feedback ticks at 10% intervals.

---

## 5. Microcopy & Labeling Rules

> [!IMPORTANT]
> **Eliminate Broad & Redundant Text**:
> Never add descriptive filler that merely states the obvious (e.g., "Toggle switch to turn on fan", "Visual button for speed increase", "Fan control interface").

### Do's & Don'ts
* ❌ **Don't**: *"This is the tachometer reading that shows your current RPM in real time"*
* ✅ **Do**: `"1,850 RPM"` with a subtle `"Live"` badge.
* ❌ **Don't**: *"Press here to engage turbo boost fan cooling mode"*
* ✅ **Do**: `"Boost"`
* ❌ **Don't**: *"Connection status: Bluetooth is currently searching for ESP32 device"*
* ✅ **Do**: `"Connecting..."` / `"Connected"`

