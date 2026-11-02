package com.espfancontrol.data.model

import android.bluetooth.BluetoothDevice

enum class FanMode(val index: Int, val label: String) {
    MANUAL(0, "Manual"),
    SILENT(1, "Silent"),
    BALANCED(2, "Balanced"),
    BOOST(3, "Boost"),
    AUTO(4, "Auto");

    companion object {
        fun fromIndex(index: Int): FanMode = entries.firstOrNull { it.index == index } ?: MANUAL
    }
}

enum class BleConnectionState(val label: String) {
    DISCONNECTED("Disconnected"),
    SCANNING("Scanning..."),
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    BLUETOOTH_OFF("Bluetooth Off"),
    LOCATION_OFF("Location Required"),
    NO_DEVICE_FOUND("No Fan Found")
}

data class DiscoveredBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isTarget: Boolean = false,
    val device: BluetoothDevice
)

data class FanUiState(
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val isPowered: Boolean = true,
    val speedPercent: Int = 50,
    val liveRpm: Int = 0,
    val mode: FanMode = FanMode.BALANCED,
    val isAutoMode: Boolean = false,
    val deviceName: String = "ESP32-C3 Fan",
    val discoveredDevices: List<DiscoveredBleDevice> = emptyList()
)
