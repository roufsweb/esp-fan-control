#pragma once

#include <Arduino.h>
#include "fan_config.h"

class PwmController {
public:
    PwmController();
    
    // Initialize LEDC timer and bind to GPIO 4
    void begin();
    
    // Set target fan speed (0-100%)
    // If percent > 0 and < FAN_MIN_DUTY_PERCENT, enforces stall-prevention threshold
    void setSpeedPercent(uint8_t percent);
    
    // Immediate power toggle
    void setEnabled(bool enabled);
    
    // Getters
    uint8_t getSpeedPercent() const { return currentPercent_; }
    uint32_t getRawDuty() const { return currentDuty_; }
    bool isEnabled() const { return isEnabled_; }

private:
    uint8_t currentPercent_;
    uint32_t currentDuty_;
    bool isEnabled_;
    
    uint32_t percentToDuty(uint8_t percent);
};

