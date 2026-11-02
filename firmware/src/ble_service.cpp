#include "ble_service.h"

BleFanService::BleFanService()
    : pServer_(nullptr),
      pService_(nullptr),
      pCharSpeed_(nullptr),
      pCharRpm_(nullptr),
      pCharMode_(nullptr),
      clientConnected_(false),
      onSpeedChangeCb_(nullptr),
      onModeChangeCb_(nullptr) {}

void BleFanService::begin(SpeedChangeCallback onSpeedChange, ModeChangeCallback onModeChange) {
    onSpeedChangeCb_ = onSpeedChange;
    onModeChangeCb_ = onModeChange;

    // Initialize NimBLE Device
    NimBLEDevice::init(BLE_DEVICE_NAME);
    NimBLEDevice::setPower(ESP_PWR_LVL_P9); // Maximum TX power for stable connection
    NimBLEDevice::setSecurityAuth(false, false, false); // Just-works pairing

    pServer_ = NimBLEDevice::createServer();
    pServer_->setCallbacks(this);

    // Create GATT Service
    pService_ = pServer_->createService(BLE_SERVICE_UUID);

    // Speed Characteristic (Read, Write, Write No Response, Notify)
    pCharSpeed_ = pService_->createCharacteristic(
        BLE_CHAR_FAN_SPEED_UUID,
        NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_NR | NIMBLE_PROPERTY::NOTIFY
    );
    uint8_t defaultSpeed = 0;
    pCharSpeed_->setValue(&defaultSpeed, 1);
    pCharSpeed_->setCallbacks(this);

    // RPM Characteristic (Read, Notify)
    pCharRpm_ = pService_->createCharacteristic(
        BLE_CHAR_FAN_RPM_UUID,
        NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY
    );
    uint16_t defaultRpm = 0;
    pCharRpm_->setValue(reinterpret_cast<uint8_t*>(&defaultRpm), 2);

    // Mode Characteristic (Read, Write, Write No Response, Notify)
    pCharMode_ = pService_->createCharacteristic(
        BLE_CHAR_FAN_MODE_UUID,
        NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_NR | NIMBLE_PROPERTY::NOTIFY
    );
    uint8_t defaultMode = 0; // Manual
    pCharMode_->setValue(&defaultMode, 1);
    pCharMode_->setCallbacks(this);

    pService_->start();

    // Configure Advertising
    NimBLEAdvertising* pAdvertising = NimBLEDevice::getAdvertising();
    
    // Explicit primary advertising packet: General Discoverable + Complete Local Name
    // This guarantees "ESP32-C3 Fan" is immediately visible in ALL Bluetooth scanners & Android menus
    NimBLEAdvertisementData advData;
    advData.setFlags(0x06); // General Discoverable (0x02) + BR/EDR Not Supported (0x04)
    advData.setName(BLE_DEVICE_NAME);
    pAdvertising->setAdvertisementData(advData);

    // Scan Response packet: Full 128-bit Fan Service UUID
    NimBLEAdvertisementData scanData;
    scanData.setCompleteServices(NimBLEUUID(BLE_SERVICE_UUID));
    pAdvertising->setScanResponseData(scanData);

    // High-frequency advertising interval (20ms to 40ms) for sub-second discovery
    pAdvertising->setMinInterval(32); // 32 * 0.625ms = 20ms
    pAdvertising->setMaxInterval(64); // 64 * 0.625ms = 40ms
    pAdvertising->setScanResponse(true);
    pAdvertising->start();

    Serial.println("[BLE] Fan GATT Service active: Broadcasting 'ESP32-C3 Fan' at 20-40ms interval.");
}

void BleFanService::onConnect(NimBLEServer* pServer) {
    clientConnected_ = true;
    Serial.println("[BLE] Android client connected.");
}

void BleFanService::onDisconnect(NimBLEServer* pServer) {
    clientConnected_ = false;
    Serial.println("[BLE] Client disconnected. Restarting advertisement...");
    NimBLEDevice::startAdvertising();
}

void BleFanService::onWrite(NimBLECharacteristic* pCharacteristic) {
    std::string uuid = pCharacteristic->getUUID().toString();
    std::string val = pCharacteristic->getValue();

    if (val.empty()) return;

    if (pCharacteristic == pCharSpeed_) {
        uint8_t speed = static_cast<uint8_t>(val[0]);
        Serial.printf("[BLE] Speed write command received: %u%%\n", speed);
        if (onSpeedChangeCb_) {
            onSpeedChangeCb_(speed);
        }
    } else if (pCharacteristic == pCharMode_) {
        uint8_t mode = static_cast<uint8_t>(val[0]);
        Serial.printf("[BLE] Mode write command received: %u\n", mode);
        if (onModeChangeCb_) {
            onModeChangeCb_(mode);
        }
    }
}

void BleFanService::notifyRpm(uint16_t rpm) {
    if (clientConnected_ && pCharRpm_) {
        pCharRpm_->setValue(reinterpret_cast<uint8_t*>(&rpm), sizeof(rpm));
        pCharRpm_->notify();
    }
}

void BleFanService::notifySpeed(uint8_t speedPercent) {
    if (pCharSpeed_) {
        pCharSpeed_->setValue(&speedPercent, 1);
        if (clientConnected_) {
            pCharSpeed_->notify();
        }
    }
}

void BleFanService::notifyMode(uint8_t modeIndex) {
    if (pCharMode_) {
        pCharMode_->setValue(&modeIndex, 1);
        if (clientConnected_) {
            pCharMode_->notify();
        }
    }
}

