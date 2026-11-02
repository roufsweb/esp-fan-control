#include "pwm_controller.h"
#include <esp_arduino_version.h>

PwmController::PwmController()
    : currentPercent_(0),
      currentDuty_(0),
      isEnabled_(true) {}

void PwmController::begin() {
    pinMode(PIN_FAN_PWM, OUTPUT);

#if defined(ESP_ARDUINO_VERSION_MAJOR) && (ESP_ARDUINO_VERSION_MAJOR >= 3)
    // ESP32 Arduino Core 3.x syntax
    ledcAttach(PIN_FAN_PWM, FAN_PWM_FREQ, FAN_PWM_RESOLUTION);
    ledcWrite(PIN_FAN_PWM, 0);
#else
    // ESP32 Arduino Core 2.x syntax
    ledcSetup(FAN_PWM_CHANNEL, FAN_PWM_FREQ, FAN_PWM_RESOLUTION);
    ledcAttachPin(PIN_FAN_PWM, FAN_PWM_CHANNEL);
    ledcWrite(FAN_PWM_CHANNEL, 0);
#endif

    currentPercent_ = 0;
    currentDuty_ = 0;
}

uint32_t PwmController::percentToDuty(uint8_t percent) {
    if (percent == 0) return 0;
    
    // Fail-safe: clamp to minimum duty cycle to avoid motor stall
    if (percent < FAN_MIN_DUTY_PERCENT) {
        percent = FAN_MIN_DUTY_PERCENT;
    }
    if (percent > FAN_MAX_DUTY_PERCENT) {
        percent = FAN_MAX_DUTY_PERCENT;
    }
    
    // 8-bit scale: 0 to 255
    return (static_cast<uint32_t>(percent) * 255) / 100;
}

void PwmController::setSpeedPercent(uint8_t percent) {
    if (!isEnabled_) {
        currentPercent_ = percent;
        return;
    }

    if (percent == 0) {
        currentPercent_ = 0;
        currentDuty_ = 0;
    } else {
        if (percent < FAN_MIN_DUTY_PERCENT) {
            percent = FAN_MIN_DUTY_PERCENT;
        } else if (percent > FAN_MAX_DUTY_PERCENT) {
            percent = FAN_MAX_DUTY_PERCENT;
        }
        currentPercent_ = percent;
        currentDuty_ = percentToDuty(percent);
    }

#if defined(ESP_ARDUINO_VERSION_MAJOR) && (ESP_ARDUINO_VERSION_MAJOR >= 3)
    ledcWrite(PIN_FAN_PWM, currentDuty_);
#else
    ledcWrite(FAN_PWM_CHANNEL, currentDuty_);
#endif
}

void PwmController::setEnabled(bool enabled) {
    isEnabled_ = enabled;
    if (!isEnabled_) {
#if defined(ESP_ARDUINO_VERSION_MAJOR) && (ESP_ARDUINO_VERSION_MAJOR >= 3)
        ledcWrite(PIN_FAN_PWM, 0);
#else
        ledcWrite(FAN_PWM_CHANNEL, 0);
#endif
    } else {
        setSpeedPercent(currentPercent_);
    }
}

