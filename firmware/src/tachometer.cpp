#include "tachometer.h"

volatile uint32_t Tachometer::pulseCount_ = 0;
volatile uint32_t Tachometer::lastPulseMicros_ = 0;

Tachometer::Tachometer()
    : currentRpm_(0),
      lastCalcTime_(0) {}

void IRAM_ATTR Tachometer::handlePulse() {
    uint32_t nowUs = micros();
    // Hardware debouncing: Reject edges faster than TACH_DEBOUNCE_US (e.g. 500us ~ max 60,000 RPM)
    if (nowUs - lastPulseMicros_ >= TACH_DEBOUNCE_US) {
        pulseCount_++;
        lastPulseMicros_ = nowUs;
    }
}

void Tachometer::begin() {
    // Open-collector output requires internal pull-up enabled
    pinMode(PIN_FAN_TACH, INPUT_PULLUP);
    attachInterrupt(digitalPinToInterrupt(PIN_FAN_TACH), handlePulse, FALLING);
    lastCalcTime_ = millis();
    currentRpm_ = 0;
}

bool Tachometer::update() {
    uint32_t nowMs = millis();
    uint32_t elapsedMs = nowMs - lastCalcTime_;

    if (elapsedMs >= TACH_CALC_INTERVAL_MS) {
        // Atomic read and reset of pulse count
        noInterrupts();
        uint32_t pulses = pulseCount_;
        pulseCount_ = 0;
        interrupts();

        if (pulses > 0) {
            // Formula: RPM = (Pulses / 2) * (60,000 / dt_ms)
            uint32_t rpm = (pulses * 60000UL) / (TACH_PULSES_PER_REV * elapsedMs);
            currentRpm_ = static_cast<uint16_t>(rpm);
        } else {
            currentRpm_ = 0;
        }

        lastCalcTime_ = nowMs;
        return true;
    }

    // Safety timeout: If fan completely stops, decay to 0 RPM
    if (nowMs - lastPulseMicros_ / 1000UL > TACH_TIMEOUT_MS && currentRpm_ > 0) {
        currentRpm_ = 0;
    }

    return false;
}

