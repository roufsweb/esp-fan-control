#pragma once

#include <Arduino.h>
#include "fan_config.h"

class Tachometer {
public:
    Tachometer();
    
    // Initialize GPIO 3 with INPUT_PULLUP and attach ISR
    void begin();
    
    // Call in main loop to calculate rolling RPM window
    // Returns true if a new RPM calculation cycle finished
    bool update();
    
    // Get latest calculated RPM
    uint16_t getRpm() const { return currentRpm_; }
    
    // Static ISR called on falling edge
    static void IRAM_ATTR handlePulse();

private:
    uint16_t currentRpm_;
    uint32_t lastCalcTime_;
    
    static volatile uint32_t pulseCount_;
    static volatile uint32_t lastPulseMicros_;
};

