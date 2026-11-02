#pragma once

#include <Arduino.h>

// ============================================================================
// HARDWARE PIN DEFINITIONS
// ============================================================================
// Blue Wire: PWM Speed Control (Intel 4-Wire Standard)
constexpr uint8_t PIN_FAN_PWM = 4;

// Yellow Wire: Tachometer Speed Sense (Open-Collector, 2 Pulses / Revolution)
constexpr uint8_t PIN_FAN_TACH = 3;

// On-board Status LED (GPIO 8 on DevKitM-1 RGB / SuperMini Blue LED)
constexpr uint8_t PIN_STATUS_LED = 8;

// ============================================================================
// PWM CONFIGURATION (Intel 4-Wire Spec: 21 kHz - 28 kHz, Target: 25 kHz)
// ============================================================================
constexpr uint32_t FAN_PWM_FREQ = 25000;    // 25 kHz to eliminate audible whine
constexpr uint8_t  FAN_PWM_CHANNEL = 0;     // ESP32 LEDC Channel 0
constexpr uint8_t  FAN_PWM_RESOLUTION = 8;  // 8-bit: 0 to 255
constexpr uint8_t  FAN_MIN_DUTY_PERCENT = 20; // 20% minimum stall-prevention threshold
constexpr uint8_t  FAN_MAX_DUTY_PERCENT = 100;

// ============================================================================
// TACHOMETER CONFIGURATION
// ============================================================================
constexpr uint8_t  TACH_PULSES_PER_REV = 2; // Standard PC/laptop fan specification
constexpr uint32_t TACH_DEBOUNCE_US = 500;  // 500us debounce filters high-freq RF noise
constexpr uint32_t TACH_CALC_INTERVAL_MS = 1000; // 1-second rolling calculation window
constexpr uint32_t TACH_TIMEOUT_MS = 2500;  // Reset RPM to 0 if no pulses within 2.5s

// ============================================================================
// BLE GATT SERVICE & CHARACTERISTIC UUIDs
// ============================================================================
#define BLE_DEVICE_NAME                "ESP32-C3 Fan"
#define BLE_SERVICE_UUID               "19B10000-E8F2-537E-4F6C-D104768A1214"
#define BLE_CHAR_FAN_SPEED_UUID        "19B10001-E8F2-537E-4F6C-D104768A1214" // Read/Write/Notify (uint8_t: 0-100)
#define BLE_CHAR_FAN_RPM_UUID          "19B10002-E8F2-537E-4F6C-D104768A1214" // Read/Notify (uint16_t: Live RPM)
#define BLE_CHAR_FAN_MODE_UUID         "19B10003-E8F2-537E-4F6C-D104768A1214" // Read/Write (uint8_t: 0=Manual, 1=Silent, 2=Balanced, 3=Boost, 4=Auto)

// Presets (Speed Percentage)
constexpr uint8_t PRESET_SILENT_PERCENT   = 25;
constexpr uint8_t PRESET_BALANCED_PERCENT = 50;
constexpr uint8_t PRESET_BOOST_PERCENT    = 100;

