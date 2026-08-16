package com.marshall.motif.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Official Marshall custom EQ uses Airoha RACE over Classic SPP, not BLE GATT.
 * UUID from AirohaController: 00000000-0000-0000-0099-AABBCCDDEEFF
 */
@SuppressLint("MissingPermission")
class RaceSpp(private val log: (String) -> Unit) {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00000000-0000-0000-0099-AABBCCDDEEFF")
    }

    @Volatile
    private var socket: BluetoothSocket? = null

    fun close() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    fun findClassicMotif(adapter: BluetoothAdapter?): BluetoothDevice? {
        val bonded = try {
            adapter?.bondedDevices
        } catch (_: SecurityException) {
            null
        } ?: return null
        return bonded.firstOrNull { device ->
            val name = device.name.orEmpty()
            name.contains("MOTIF", ignoreCase = true) &&
                !name.contains("[LE]", ignoreCase = true)
        } ?: bonded.firstOrNull { it.name.orEmpty().contains("MOTIF", ignoreCase = true) }
    }

    suspend fun ensureConnected(adapter: BluetoothAdapter?): Boolean = withContext(Dispatchers.IO) {
        val open = socket
        if (open != null && open.isConnected) return@withContext true
        close()
        val device = findClassicMotif(adapter)
        if (device == null) {
            log("spp: no bonded Motif Classic device")
            return@withContext false
        }
        log("spp: connecting ${device.name} ${device.address}")
        val created = try {
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        } catch (e: Exception) {
            log("spp: create socket failed ${e.message}")
            return@withContext false
        }
        try {
            created.connect()
            socket = created
            log("spp: connected")
            true
        } catch (e: Exception) {
            try {
                created.close()
            } catch (_: Exception) {
            }
            log("spp: connect failed ${e.message}")
            false
        }
    }

    suspend fun send(packet: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val current = socket ?: return@withContext false
        try {
            current.outputStream.write(packet)
            current.outputStream.flush()
            val buf = ByteArray(512)
            val deadline = System.currentTimeMillis() + 1500
            var n = 0
            while (System.currentTimeMillis() < deadline) {
                val avail = current.inputStream.available()
                if (avail > 0) {
                    n = current.inputStream.read(buf)
                    break
                }
                Thread.sleep(40)
            }
            if (n > 0) {
                log("spp rx: " + buf.copyOf(n).toHex())
            } else {
                log("spp tx ${packet.size}b (no reply)")
            }
            true
        } catch (e: Exception) {
            log("spp write failed ${e.message}")
            close()
            false
        }
    }
}
