package com.espfancontrol.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.espfancontrol.data.model.BleConnectionState
import com.espfancontrol.data.model.FanMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "BleManager"

        // 128-bit UUIDs matching ESP32 firmware
        val SERVICE_UUID: UUID = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214")
        val CHAR_SPEED_UUID: UUID = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214")
        val CHAR_RPM_UUID: UUID = UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214")
        val CHAR_MODE_UUID: UUID = UUID.fromString("19B10003-E8F2-537E-4F6C-D104768A1214")

        // Standard Client Characteristic Configuration Descriptor (CCCD)
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var bluetoothGatt: BluetoothGatt? = null
    private var charSpeed: BluetoothGattCharacteristic? = null
    private var charRpm: BluetoothGattCharacteristic? = null
    private var charMode: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _liveRpm = MutableStateFlow(0)
    val liveRpm: StateFlow<Int> = _liveRpm.asStateFlow()

    private val _currentSpeed = MutableStateFlow(50)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    private val _currentMode = MutableStateFlow(FanMode.BALANCED)
    val currentMode: StateFlow<FanMode> = _currentMode.asStateFlow()

    private var targetDevice: BluetoothDevice? = null
    private var isAutoReconnectActive = true
    private var reconnectAttempt = 0

    // Hardware Filtered Scanning
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                Log.d(TAG, "Target ESP32 found: ${device.name ?: "Unknown"} [${device.address}]")
                stopScan()
                connectToDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed with code: $errorCode")
            _connectionState.value = BleConnectionState.DISCONNECTED
        }
    }

    fun startScanAndConnect() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth adapter unavailable or disabled")
            _connectionState.value = BleConnectionState.DISCONNECTED
            return
        }

        if (_connectionState.value == BleConnectionState.CONNECTED || _connectionState.value == BleConnectionState.CONNECTING) {
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.w(TAG, "BLE Scanner not available")
            return
        }

        _connectionState.value = BleConnectionState.SCANNING

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)

        // Safety timeout: stop scanning after 15 seconds
        scope.launch {
            delay(15000)
            if (_connectionState.value == BleConnectionState.SCANNING) {
                stopScan()
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        }
    }

    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        targetDevice = device
        _connectionState.value = BleConnectionState.CONNECTING

        bluetoothGatt = device.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    fun disconnect() {
        isAutoReconnectActive = false
        stopScan()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    // GATT Callback Handler
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "GATT connected. Requesting high priority & discovering services...")
                    gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    gatt?.discoverServices()
                    reconnectAttempt = 0
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "GATT disconnected (status $status)")
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    gatt?.close()
                    bluetoothGatt = null

                    if (isAutoReconnectActive) {
                        scheduleReconnect()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    charSpeed = service.getCharacteristic(CHAR_SPEED_UUID)
                    charRpm = service.getCharacteristic(CHAR_RPM_UUID)
                    charMode = service.getCharacteristic(CHAR_MODE_UUID)

                    _connectionState.value = BleConnectionState.CONNECTED
                    Log.i(TAG, "ESP32 Fan Service bound successfully!")

                    // Subscribe to RPM notifications
                    enableRpmNotifications(gatt)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicUpdate(characteristic.uuid, value)
        }

        // Backward compatibility for Android <= 12
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic != null) {
                @Suppress("DEPRECATION")
                handleCharacteristicUpdate(characteristic.uuid, characteristic.value)
            }
        }
    }

    private fun handleCharacteristicUpdate(uuid: UUID, bytes: ByteArray) {
        when (uuid) {
            CHAR_RPM_UUID -> {
                if (bytes.size >= 2) {
                    val rpm = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
                    _liveRpm.value = rpm
                }
            }
            CHAR_SPEED_UUID -> {
                if (bytes.isNotEmpty()) {
                    _currentSpeed.value = bytes[0].toInt() and 0xFF
                }
            }
            CHAR_MODE_UUID -> {
                if (bytes.isNotEmpty()) {
                    _currentMode.value = FanMode.fromIndex(bytes[0].toInt())
                }
            }
        }
    }

    private fun enableRpmNotifications(gatt: BluetoothGatt) {
        val rpmChar = charRpm ?: return
        gatt.setCharacteristicNotification(rpmChar, true)

        val descriptor = rpmChar.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    // Fast Write Without Response
    fun sendSpeedCommand(percent: Int) {
        val gatt = bluetoothGatt ?: return
        val characteristic = charSpeed ?: return
        val clamped = percent.coerceIn(0, 100).toByte()

        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, byteArrayOf(clamped), BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = byteArrayOf(clamped)
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
        _currentSpeed.value = percent
    }

    fun sendModeCommand(mode: FanMode) {
        val gatt = bluetoothGatt ?: return
        val characteristic = charMode ?: return
        val modeByte = mode.index.toByte()

        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, byteArrayOf(modeByte), BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = byteArrayOf(modeByte)
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
        _currentMode.value = mode
    }

    private fun scheduleReconnect() {
        val backoffMs = (1000L * (1L shl reconnectAttempt.coerceAtMost(3))) // 1s, 2s, 4s, 8s
        reconnectAttempt++
        Log.d(TAG, "Reconnecting in ${backoffMs}ms (attempt $reconnectAttempt)...")

        scope.launch {
            delay(backoffMs)
            if (isAutoReconnectActive && _connectionState.value == BleConnectionState.DISCONNECTED) {
                targetDevice?.let { connectToDevice(it) } ?: startScanAndConnect()
            }
        }
    }
}

