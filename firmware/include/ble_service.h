#pragma once

#include <Arduino.h>
#include <NimBLEDevice.h>
#include "fan_config.h"

// Callback type for speed change commands received from Android client
using SpeedChangeCallback = void (*)(uint8_t speedPercent);
using ModeChangeCallback = void (*)(uint8_t modeIndex);

class BleFanService : public NimBLEServerCallbacks, public NimBLECharacteristicCallbacks {
public:
    BleFanService();

    // Initialize BLE stack, GATT server, service, characteristics, and start advertising
    void begin(SpeedChangeCallback onSpeedChange, ModeChangeCallback onModeChange);

    // Push live telemetry notifications to Android client
    void notifyRpm(uint16_t rpm);
    void notifySpeed(uint8_t speedPercent);
    void notifyMode(uint8_t modeIndex);

    bool isClientConnected() const { return clientConnected_; }

    // NimBLEServerCallbacks
    void onConnect(NimBLEServer* pServer) override;
    void onDisconnect(NimBLEServer* pServer) override;

    // NimBLECharacteristicCallbacks
    void onWrite(NimBLECharacteristic* pCharacteristic) override;

private:
    NimBLEServer* pServer_;
    NimBLEService* pService_;
    NimBLECharacteristic* pCharSpeed_;
    NimBLECharacteristic* pCharRpm_;
    NimBLECharacteristic* pCharMode_;
    
    bool clientConnected_;
    SpeedChangeCallback onSpeedChangeCb_;
    ModeChangeCallback onModeChangeCb_;
};

