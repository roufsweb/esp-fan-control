---
name: project-summary-manager
description: >-
  Manages and updates PROJECT_SUMMARY.md. Appends concise records of experiments,
  successes, failures, hardware calibrations, and technical decisions as the project evolves.
---

# Project Summary Manager Specification

Use this skill whenever completing a feature, testing hardware, attempting an experiment, or debugging an issue. It ensures the team maintains a cumulative, rapid-glance ledger of technical learnings.

---

## 1. Operating Rules for Summaries

1. **Be Rapidly Scannable**:
   * Use structured markdown tables and short bullet points.
   * Do not write lengthy essays. Keep entries strictly under 2–3 sentences.

2. **Categorize Every Entry**:
   * **Component / Topic**: e.g. `PWM Generator`, `Tachometer ISR`, `BLE GATT`, `M3 Theme`.
   * **What Was Tried**: Concrete action or architecture tested.
   * **What Worked**: The exact method or parameters that produced the desired result.
   * **What Did Not Work**: The specific failure, bug, or acoustic/electrical problem encountered.
   * **Status**: `Completed`, `Iterating`, `Blocked`, or `Standardized`.

3. **Cumulative Evolution**:
   * Never delete previous experiment records unless they were based on completely invalid assumptions.
   * Append new entries to the `Cumulative Experiment & Development Ledger` in `PROJECT_SUMMARY.md`.

---

## 2. Standard Entry Template

When recording a new experiment or milestone, format as a table row in `PROJECT_SUMMARY.md`:

```markdown
| <ID> | **<Topic>** | <What was tried> | <What worked & verified parameters> | <What failed & root cause> | <Status> |
```

### Example Entry:
```markdown
| 7 | **BLE Reconnection** | Auto-reconnect with exponential backoff on Android client. | Tested 1s, 2s, 4s retry backoff; recovered connection after ESP power cycle in under 3.2s. | Immediate unthrottled reconnect loops caused Android Bluetooth stack GATT error 133. | Completed |
```

---

## 3. Trigger Points for Updating `PROJECT_SUMMARY.md`
* After verifying a hardware circuit or pin reading (e.g., tachometer pulse counting on GPIO 3).
* After testing a PWM frequency or duty cycle on GPIO 4.
* After adding or refining an Android UI screen, theme, or toggle component.
* After creating or optimizing an Antigravity agent skill.
* Whenever an error, crash, or unexpected behavior is diagnosed and resolved.

