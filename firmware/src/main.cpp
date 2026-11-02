#include "ble_service.h"
#include "fan_config.h"
#include "pwm_controller.h"
#include "tachometer.h"
#include <Arduino.h>
#include <Preferences.h>

// Global instances
static PwmController fanPwm;
static Tachometer fanTach;
static BleFanService bleService;
static Preferences prefs;

// State variables
static uint8_t currentMode = 2; // Default Balanced
static uint8_t currentSpeed = PRESET_BALANCED_PERCENT;
static bool currentPower = true;

// Flash commit debounce variables
static bool pendingFlashWrite = false;
static uint32_t pendingFlashTime = 0;
constexpr uint32_t FLASH_DEBOUNCE_MS = 1500;

static uint32_t lastSerialLogTime = 0;

// Save state to NVS
void commitStateToFlash() {
  prefs.begin("fan_state", false);
  prefs.putUChar("pwr", currentPower ? 1 : 0);
  prefs.putUChar("spd", currentSpeed);
  prefs.putUChar("mod", currentMode);
  prefs.end();
  pendingFlashWrite = false;
  Serial.printf("[NVS] State saved: Pwr=%u, Spd=%u%%, Mod=%u\n", currentPower,
                currentSpeed, currentMode);
}

// Callbacks from BLE
void onBleSpeedReceived(uint8_t speedPercent) {
  currentMode = 0; // Manual
  currentSpeed = speedPercent;
  currentPower = (speedPercent > 0);

  fanPwm.setEnabled(currentPower);
  fanPwm.setSpeedPercent(speedPercent);

  bleService.notifySpeed(fanPwm.getSpeedPercent());
  bleService.notifyMode(currentMode);

  // Debounce flash write to avoid wear during slider dragging
  pendingFlashWrite = true;
  pendingFlashTime = millis();
}

void onBleModeReceived(uint8_t modeIndex) {
  currentMode = modeIndex;
  currentPower = true;
  fanPwm.setEnabled(true);

  switch (modeIndex) {
  case 1: // Silent
    currentSpeed = PRESET_SILENT_PERCENT;
    break;
  case 2: // Balanced
    currentSpeed = PRESET_BALANCED_PERCENT;
    break;
  case 3: // Boost
    currentSpeed = PRESET_BOOST_PERCENT;
    break;
  case 4: // Auto
    currentSpeed = PRESET_BALANCED_PERCENT;
    break;
  default: // Manual
    break;
  }

  fanPwm.setSpeedPercent(currentSpeed);
  bleService.notifySpeed(fanPwm.getSpeedPercent());
  bleService.notifyMode(currentMode);

  // Immediate commit for discrete mode toggles
  commitStateToFlash();
}

void setup() {
  Serial.begin(115200);
  delay(1000); // Allow USB CDC enumeration on ESP32-C3

  Serial.println("==================================================");
  Serial.println("   ESP32-C3 4-Wire PWM Fan Controller (v1.0)     ");
  Serial.println("==================================================");
  Serial.printf("[INIT] Blue Wire (PWM):    GPIO %u (LEDC 25 kHz)\n",
                PIN_FAN_PWM);
  Serial.printf("[INIT] Yellow Wire (Tach): GPIO %u (INPUT_PULLUP)\n",
                PIN_FAN_TACH);

  // Load persisted state from NVS
  prefs.begin("fan_state", true); // Read-only mode
  currentPower = prefs.getUChar("pwr", 1) != 0;
  currentSpeed = prefs.getUChar("spd", PRESET_BALANCED_PERCENT);
  currentMode = prefs.getUChar("mod", 2); // Default Balanced
  prefs.end();

  Serial.printf("[NVS] Restored state: Pwr=%u, Spd=%u%%, Mod=%u\n",
                currentPower, currentSpeed, currentMode);

  // Initialize hardware peripherals
  pinMode(PIN_STATUS_LED, OUTPUT);
  digitalWrite(PIN_STATUS_LED, HIGH); // Immediate power-on visual indicator
  fanPwm.begin();
  fanTach.begin();

  // Apply restored state
  fanPwm.setEnabled(currentPower);
  if (currentPower) {
    fanPwm.setSpeedPercent(currentSpeed);
  } else {
    fanPwm.setSpeedPercent(0);
  }

  // Initialize BLE Stack and GATT Service
  bleService.begin(onBleSpeedReceived, onBleModeReceived);
  bleService.notifySpeed(currentPower ? currentSpeed : 0);
  bleService.notifyMode(currentMode);

  Serial.println("[INIT] Peripherals and BLE ready.");
}

void loop() {
  // Check tachometer pulse accumulation (1000ms window)
  if (fanTach.update()) {
    uint16_t rpm = fanTach.getRpm();
    bleService.notifyRpm(rpm);

    // Periodic telemetry log to USB serial
    uint32_t now = millis();
    if (now - lastSerialLogTime >= 2000) {
      Serial.printf(
          "[STATUS] PWM: %3u%% (Duty %3u/255) | RPM: %4u | Mode: %u | BLE: "
          "%s\n",
          fanPwm.getSpeedPercent(), fanPwm.getRawDuty(), rpm, currentMode,
          bleService.isClientConnected() ? "Connected" : "Advertising");
      lastSerialLogTime = now;
    }
  }

  // Check debounced NVS flash write
  if (pendingFlashWrite && (millis() - pendingFlashTime >= FLASH_DEBOUNCE_MS)) {
    commitStateToFlash();
  }

  // Visual status LED: Solid ON when phone connected, heartbeat blink when advertising
  uint32_t nowMs = millis();
  if (bleService.isClientConnected()) {
    digitalWrite(PIN_STATUS_LED, HIGH);
  } else {
    // 80ms pulse every 1000ms
    bool ledPulse = (nowMs % 1000) < 80;
    digitalWrite(PIN_STATUS_LED, ledPulse ? HIGH : LOW);
  }

  // Yield for FreeRTOS background tasks & BLE stack
  delay(10);
}
