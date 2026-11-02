package com.espfancontrol.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.espfancontrol.data.ble.BleManager
import com.espfancontrol.data.model.BleConnectionState
import com.espfancontrol.data.model.DiscoveredBleDevice
import com.espfancontrol.data.model.FanMode
import com.espfancontrol.data.model.FanUiState
import com.espfancontrol.data.usb.Esp32Flasher
import com.espfancontrol.data.usb.FlashingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FanViewModel(application: Application) : AndroidViewModel(application) {

    val bleManager = BleManager(application.applicationContext)
    val flasher = Esp32Flasher(application.applicationContext)
    val flashingState: StateFlow<FlashingState> = flasher.flashingState

    private val _isPowered = MutableStateFlow(true)
    private val _lastActiveSpeed = MutableStateFlow(50)

    private val _uiState = MutableStateFlow(FanUiState())
    val uiState: StateFlow<FanUiState> = _uiState.asStateFlow()

    init {
        // Auto-scan on launch
        bleManager.startScanAndConnect()

        viewModelScope.launch {
            combine(
                combine(bleManager.connectionState, bleManager.discoveredDevices) { conn, devs -> Pair(conn, devs) },
                bleManager.liveRpm,
                bleManager.currentSpeed,
                bleManager.currentMode,
                _isPowered
            ) { (connection, devices), rpm, speed, mode, powered ->
                FanUiState(
                    connectionState = connection,
                    liveRpm = if (powered && connection == BleConnectionState.CONNECTED) rpm else 0,
                    speedPercent = if (powered) speed else 0,
                    mode = mode,
                    isPowered = powered,
                    isAutoMode = (mode == FanMode.AUTO),
                    discoveredDevices = devices
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun toggleConnection() {
        if (bleManager.connectionState.value == BleConnectionState.CONNECTED ||
            bleManager.connectionState.value == BleConnectionState.CONNECTING) {
            bleManager.disconnect()
        } else {
            bleManager.startScanAndConnect()
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        bleManager.connectToDevice(device)
    }

    fun setSpeed(percent: Int) {
        if (!_isPowered.value) {
            _isPowered.value = true
        }
        _lastActiveSpeed.value = percent
        bleManager.sendSpeedCommand(percent)
    }

    fun setMode(mode: FanMode) {
        if (!_isPowered.value) {
            _isPowered.value = true
        }
        bleManager.sendModeCommand(mode)
    }

    fun setPower(enabled: Boolean) {
        _isPowered.value = enabled
        if (enabled) {
            bleManager.sendSpeedCommand(_lastActiveSpeed.value)
        } else {
            bleManager.sendSpeedCommand(0)
        }
    }

    fun flashEsp32() {
        viewModelScope.launch {
            flasher.flashFirmware()
        }
    }

    fun checkUsbDeviceAttached() {
        flasher.findConnectedEspDevice()
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
    }
}
