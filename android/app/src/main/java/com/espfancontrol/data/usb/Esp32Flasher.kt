package com.espfancontrol.data.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.CRC32

sealed class FlashingState {
    object Idle : FlashingState()
    object DeviceDetected : FlashingState()
    object Connecting : FlashingState()
    data class Progress(val percentage: Int, val bytesWritten: Int, val totalBytes: Int) : FlashingState()
    object Success : FlashingState()
    data class Error(val message: String) : FlashingState()
}

class Esp32Flasher(private val context: Context) {

    companion object {
        private const val TAG = "Esp32Flasher"
        private const val ASSET_FIRMWARE_NAME = "firmware_merged.bin"

        // SLIP Protocol Constants
        private const val SLIP_DELIMITER: Byte = 0xC0.toByte()
        private const val SLIP_ESC: Byte = 0xDB.toByte()
        private const val SLIP_ESC_DELIM: Byte = 0xDC.toByte()
        private const val SLIP_ESC_ESC: Byte = 0xDD.toByte()

        // Espressif Bootloader Command Codes
        private const val CMD_FLASH_BEGIN: Byte = 0x02
        private const val CMD_FLASH_DATA: Byte = 0x03
        private const val CMD_FLASH_END: Byte = 0x04
        private const val CMD_MEM_BEGIN: Byte = 0x05
        private const val CMD_MEM_END: Byte = 0x06
        private const val CMD_MEM_DATA: Byte = 0x07
        private const val CMD_SYNC: Byte = 0x08
        private const val CMD_WRITE_REG: Byte = 0x09
        private const val CMD_READ_REG: Byte = 0x0A
        private const val CMD_SPI_SET_PARAMS: Byte = 0x0B
        private const val CMD_SPI_ATTACH: Byte = 0x0D

        private const val FLASH_BLOCK_SIZE = 1024 // 1 KB chunks for maximum OTG stability
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _flashingState = MutableStateFlow<FlashingState>(FlashingState.Idle)
    val flashingState: StateFlow<FlashingState> = _flashingState.asStateFlow()

    fun findConnectedEspDevice(): UsbDevice? {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            return null
        }
        val device = availableDrivers[0].device
        _flashingState.value = FlashingState.DeviceDetected
        return device
    }

    suspend fun flashFirmware(): Boolean = withContext(Dispatchers.IO) {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            _flashingState.value = FlashingState.Error("No USB device detected. Please connect ESP32 via OTG.")
            return@withContext false
        }

        val driver = availableDrivers[0]
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            _flashingState.value = FlashingState.Error("USB permission denied. Please grant permission.")
            return@withContext false
        }

        val port = driver.ports[0]
        try {
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port.dtr = false
            port.rts = false

            _flashingState.value = FlashingState.Connecting

            // Enter ESP32 bootloader mode via DTR/RTS hardware lines
            enterBootloader(port)

            // Sync with ESP32-C3 Bootloader
            if (!syncBootloader(port)) {
                _flashingState.value = FlashingState.Error("Failed to sync with ESP32. Ensure BOOT button was held.")
                return@withContext false
            }

            // Read bundled firmware from assets
            val firmwareBytes = loadFirmwareAsset()
            if (firmwareBytes.isEmpty()) {
                _flashingState.value = FlashingState.Error("Firmware binary asset missing.")
                return@withContext false
            }

            // Attach SPI Flash & set parameters
            sendCommand(port, CMD_SPI_ATTACH, ByteArray(8))

            // Begin Flash write
            val totalBlocks = (firmwareBytes.size + FLASH_BLOCK_SIZE - 1) / FLASH_BLOCK_SIZE
            val eraseSize = firmwareBytes.size

            val beginPayload = ByteArray(16)
            writeInt32(beginPayload, 0, eraseSize)
            writeInt32(beginPayload, 4, totalBlocks)
            writeInt32(beginPayload, 8, FLASH_BLOCK_SIZE)
            writeInt32(beginPayload, 12, 0x0) // Offset 0x0

            if (!sendAndVerify(port, CMD_FLASH_BEGIN, beginPayload)) {
                _flashingState.value = FlashingState.Error("Flash erase failed.")
                return@withContext false
            }

            // Write Flash Blocks
            var offset = 0
            for (seq in 0 until totalBlocks) {
                val chunkSize = minOf(FLASH_BLOCK_SIZE, firmwareBytes.size - offset)
                val blockData = ByteArray(16 + chunkSize)

                writeInt32(blockData, 0, chunkSize)
                writeInt32(blockData, 4, seq)
                writeInt32(blockData, 8, 0)
                writeInt32(blockData, 12, 0)
                System.arraycopy(firmwareBytes, offset, blockData, 16, chunkSize)

                if (!sendAndVerify(port, CMD_FLASH_DATA, blockData)) {
                    _flashingState.value = FlashingState.Error("Write failed at block $seq")
                    return@withContext false
                }

                offset += chunkSize
                val pct = ((seq + 1) * 100) / totalBlocks
                _flashingState.value = FlashingState.Progress(pct, offset, firmwareBytes.size)
            }

            // Finish Flashing
            val endPayload = ByteArray(4)
            writeInt32(endPayload, 0, 1) // 1 = reboot to app
            sendAndVerify(port, CMD_FLASH_END, endPayload)

            // Reset ESP32 into normal execution mode
            resetToApp(port)

            _flashingState.value = FlashingState.Success
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Flashing exception", e)
            _flashingState.value = FlashingState.Error("Flashing error: ${e.localizedMessage}")
            return@withContext false
        } finally {
            try {
                port.close()
            } catch (_: Exception) {}
        }
    }

    private fun enterBootloader(port: UsbSerialPort) {
        try {
            port.dtr = false
            port.rts = true
            Thread.sleep(100)
            port.dtr = true
            port.rts = false
            Thread.sleep(100)
            port.dtr = false
            port.rts = false
            Thread.sleep(50)
        } catch (_: Exception) {}
    }

    private fun resetToApp(port: UsbSerialPort) {
        try {
            port.dtr = false
            port.rts = true
            Thread.sleep(100)
            port.rts = false
            Thread.sleep(100)
        } catch (_: Exception) {}
    }

    private fun syncBootloader(port: UsbSerialPort): Boolean {
        val syncFrame = byteArrayOf(
            0x07, 0x07, 0x12, 0x20,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55,
            0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55
        )

        for (attempt in 0..15) {
            sendCommand(port, CMD_SYNC, syncFrame)
            Thread.sleep(50)
            val resp = readPacket(port, 100)
            if (resp != null && resp.isNotEmpty() && resp[0] == 0x01.toByte()) {
                return true
            }
        }
        return false
    }

    private fun sendCommand(port: UsbSerialPort, cmd: Byte, payload: ByteArray) {
        val slipBuffer = ByteArrayOutputStream()
        slipBuffer.write(SLIP_DELIMITER.toInt())

        val packet = ByteArrayOutputStream()
        packet.write(0x00) // Direction: Request
        packet.write(cmd.toInt())
        packet.write(payload.size and 0xFF)
        packet.write((payload.size shr 8) and 0xFF)

        // Calculate simple checksum
        var checksum: Byte = 0xEF.toByte()
        for (b in payload) {
            checksum = (checksum.toInt() xor b.toInt()).toByte()
        }
        packet.write(checksum.toInt())
        packet.write(0x00)
        packet.write(0x00)
        packet.write(0x00)
        packet.write(payload)

        // SLIP Escape
        for (b in packet.toByteArray()) {
            when (b) {
                SLIP_DELIMITER -> {
                    slipBuffer.write(SLIP_ESC.toInt())
                    slipBuffer.write(SLIP_ESC_DELIM.toInt())
                }
                SLIP_ESC -> {
                    slipBuffer.write(SLIP_ESC.toInt())
                    slipBuffer.write(SLIP_ESC_ESC.toInt())
                }
                else -> slipBuffer.write(b.toInt())
            }
        }
        slipBuffer.write(SLIP_DELIMITER.toInt())

        val bytes = slipBuffer.toByteArray()
        port.write(bytes, 2000)
    }

    private fun sendAndVerify(port: UsbSerialPort, cmd: Byte, payload: ByteArray): Boolean {
        sendCommand(port, cmd, payload)
        val resp = readPacket(port, 4000)
        return resp != null && resp.size >= 1 && resp[0] == 0x01.toByte()
    }

    private fun readPacket(port: UsbSerialPort, timeoutMs: Int): ByteArray? {
        val buffer = ByteArray(256)
        val accumulated = ByteArrayOutputStream()
        val deadline = System.currentTimeMillis() + timeoutMs

        while (System.currentTimeMillis() < deadline) {
            val len = port.read(buffer, 100)
            if (len > 0) {
                accumulated.write(buffer, 0, len)
                val bytes = accumulated.toByteArray()
                // Simple SLIP delimiter check
                if (bytes.size >= 4 && bytes.last() == SLIP_DELIMITER) {
                    return bytes
                }
            }
        }
        return null
    }

    private fun loadFirmwareAsset(): ByteArray {
        return try {
            val inputStream: InputStream = context.assets.open(ASSET_FIRMWARE_NAME)
            val bytes = inputStream.readBytes()
            inputStream.close()
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "Could not open $ASSET_FIRMWARE_NAME from assets", e)
            ByteArray(0)
        }
    }

    private fun writeInt32(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }
}

