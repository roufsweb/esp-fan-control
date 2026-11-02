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
import android.location.LocationManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.espfancontrol.data.model.BleConnectionState
import com.espfancontrol.data.model.DiscoveredBleDevice
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

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private var targetDevice: BluetoothDevice? = null
    private var isAutoReconnectActive = true
    private var reconnectAttempt = 0
    private var scanTimeoutJob: Job? = null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    private fun isLocationServiceEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return true
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Comprehensive scan callback that builds device list and auto-connects to matches
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val record = result.scanRecord
            val name = device.name ?: record?.deviceName ?: ""
            val address = device.address ?: ""
            val rssi = result.rssi

            // Check if this advertisement matches our fan controller
            val isTarget = name.contains("ESP32-C3 Fan", ignoreCase = true) ||
                           name.contains("ESP32", ignoreCase = true) ||
                           name.contains("Fan", ignoreCase = true) ||
                           (record?.serviceUuids?.any { it.uuid == SERVICE_UUID } == true)

            // Update discovered devices list
            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }
            val item = DiscoveredBleDevice(
                name = if (name.isNotBlank()) name else "Unnamed BLE Device",
                address = address,
                rssi = rssi,
                isTarget = isTarget,
                device = device
            )

            if (existingIndex >= 0) {
                currentList[existingIndex] = item
            } else {
                currentList.add(item)
            }
            _discoveredDevices.value = currentList.sortedByDescending { it.isTarget }

            // Auto-connect when a matching fan is found
            if (isTarget && _connectionState.value == BleConnectionState.SCANNING) {
                Log.d(TAG, "Target ESP32 found: '$name' [$address], connecting...")
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
        // 1. Verify Bluetooth Adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth adapter unavailable or disabled")
            _connectionState.value = BleConnectionState.BLUETOOTH_OFF
            return
        }

        // 2. Verify Location on older Android versions (<= 11)
        if (!isLocationServiceEnabled()) {
            Log.w(TAG, "Location service disabled on Android <= 11")
            _connectionState.value = BleConnectionState.LOCATION_OFF
            return
        }

        if (_connectionState.value == BleConnectionState.CONNECTED || _connectionState.value == BleConnectionState.CONNECTING) {
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.w(TAG, "BLE Scanner not available")
            _connectionState.value = BleConnectionState.DISCONNECTED
            return
        }

        isAutoReconnectActive = true
        _connectionState.value = BleConnectionState.SCANNING
        _discoveredDevices.value = emptyList()

        // Check already bonded devices first for instant reconnect
        try {
            val bonded = bluetoothAdapter.bondedDevices?.firstOrNull {
                it.name?.contains("ESP32", ignoreCase = true) == true ||
                it.name?.contains("Fan", ignoreCase = true) == true
            }
            if (bonded != null) {
                Log.d(TAG, "Found bonded fan device: ${bonded.name} [${bonded.address}], connecting directly")
                connectToDevice(bonded)
                return
            }
        } catch (_: Exception) {}

        // Pass an empty ScanFilter to satisfy Android 12+ neverForLocation while receiving all BLE packets
        val filters = listOf(ScanFilter.Builder().build())

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            scanner.startScan(filters, settings, scanCallback)
            Log.d(TAG, "BLE scan started (low latency)...")
        } catch (e: Exception) {
            Log.w(TAG, "Filtered scan failed, attempting fallback: ${e.message}")
            try {
                scanner.startScan(null, settings, scanCallback)
            } catch (ex: Exception) {
                Log.e(TAG, "Cannot start scan: ${ex.message}")
                _connectionState.value = BleConnectionState.DISCONNECTED
                return
            }
        }

        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            // Safety timeout: 14 seconds scan window
            delay(14000)
            if (_connectionState.value == BleConnectionState.SCANNING) {
                stopScan()
                if (_discoveredDevices.value.none { it.isTarget }) {
                    _connectionState.value = BleConnectionState.NO_DEVICE_FOUND
                } else {
                    _connectionState.value = BleConnectionState.DISCONNECTED
                }
            }
        }
    }

    fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping scan: ${e.message}")
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        targetDevice = device
        _connectionState.value = BleConnectionState.CONNECTING

        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null

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
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    // GATT Callback Handler
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "GATT connected. Requesting high priority & discovering services...")
                        gatt?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                        gatt?.discoverServices()
                        reconnectAttempt = 0
                    } else {
                        Log.w(TAG, "GATT connection status failed: $status")
                        handleDisconnect(gatt)
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "GATT disconnected (status $status)")
                    handleDisconnect(gatt)
                }
            }
        }

        private fun handleDisconnect(gatt: BluetoothGatt?) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            try {
                gatt?.close()
            } catch (_: Exception) {}
            bluetoothGatt = null

            if (isAutoReconnectActive) {
                scheduleReconnect()
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

                    // Subscribe to live RPM notifications
                    enableRpmNotifications(gatt)
                } else {
                    Log.w(TAG, "ESP32 Fan Service UUID not found in GATT database")
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                Log.d(TAG, "Notifications enabled on CCCD. Reading initial fan speed...")
                // Read current speed from ESP32
                charSpeed?.let { gatt.readCharacteristic(it) }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicUpdate(characteristic.uuid, value)
                if (characteristic.uuid == CHAR_SPEED_UUID) {
                    charMode?.let { gatt.readCharacteristic(it) }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                @Suppress("DEPRECATION")
                handleCharacteristicUpdate(characteristic.uuid, characteristic.value)
                if (characteristic.uuid == CHAR_SPEED_UUID && gatt != null) {
                    charMode?.let {
                        @Suppress("DEPRECATION")
                        gatt.readCharacteristic(it)
                    }
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

    fun sendSpeedCommand(percent: Int) {
        val gatt = bluetoothGatt ?: return
        val characteristic = charSpeed ?: return
        val clamped = percent.coerceIn(0, 100).toByte()

        val writeType = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
        characteristic.writeType = writeType

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, byteArrayOf(clamped), writeType)
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

        val writeType = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }
        characteristic.writeType = writeType

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, byteArrayOf(modeByte), writeType)
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
