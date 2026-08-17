package com.marshall.motif.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import com.marshall.motif.EqLibrary
import com.marshall.motif.EqSnapshot
import com.marshall.motif.WidgetStateStore
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A device found during BLE scanning. */
data class ScanDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isMarshall: Boolean,
)

/**
 * Owns the Bluetooth LE connection to a Marshall device and exposes the live
 * device state as Compose-observable mutable state.
 *
 * All GATT operations are serialized through a single worker coroutine.
 */
@SuppressLint("MissingPermission")
class BleManager private constructor(private val context: Context) {
    private var activity: Activity? = null

    fun attachActivity(activity: Activity?) {
        this.activity = activity
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 71
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val PREF_EQ_PATH = "custom_eq_path"
        private const val PREF_KNOWN_DEVICES = "known_devices"

        /** Notify only what the UI needs. Now-playing / volume / RACE flood the radio. */
        private val NOTIFY_CHARS = setOf(
            MarshallGatt.ANC_CONFIG,
            MarshallGatt.ANC_VALUE,
            MarshallGatt.TRANSPARENCY_VALUE,
            MarshallGatt.EQ_SETTINGS,
            MarshallGatt.EQ_CUSTOM,
            MarshallGatt.LE_AUDIO_CONFIG,
            MarshallGatt.BT_CONNECTION_CONTROL,
            MarshallGatt.GRAPHICAL_EQ,
            MarshallGatt.LEFT_BATTERY,
            MarshallGatt.RIGHT_BATTERY,
            MarshallGatt.CASE_BATTERY,
            MarshallGatt.BATTERY_LEVEL,
            MarshallGatt.TOUCH_LOCK,
            MarshallGatt.UI_SOUNDS,
            MarshallGatt.WEAR_SENSOR_ACTION,
            MarshallGatt.WEAR_SENSOR_STATUS,
            MarshallGatt.TOUCH_MAP,
            MarshallGatt.ECO_CHARGING,
        )

        private val SKIP_IDLE_CHARS = setOf(
            MarshallGatt.NOW_PLAYING,
            MarshallGatt.VOLUME,
            MarshallGatt.AUDIO_CONTROL,
            MarshallGatt.ACTION_BUTTON_EVENT,
            MarshallGatt.AIROHA_TX,
            MarshallGatt.AIROHA_RX,
            MarshallGatt.AIROHA_META,
        )

        @Volatile
        private var instance: BleManager? = null

        fun get(context: Context): BleManager {
            return instance ?: synchronized(this) {
                instance ?: BleManager(context.applicationContext).also { instance = it }
            }
        }
    }

    var state by mutableStateOf(DeviceState())
        private set

    var scanResults by mutableStateOf<List<ScanDevice>>(emptyList())
        private set

    var scanning by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    var logs by mutableStateOf<List<String>>(emptyList())
        private set

    val isScanning: Boolean get() = scanning

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ops = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    private val preferences = context.getSharedPreferences("marshall_control", Context.MODE_PRIVATE)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    var knownDevices by mutableStateOf(loadKnownDevices())
        private set

    private val chars = HashMap<UUID, BluetoothGattCharacteristic>()
    private val standardBatteryChars = ArrayList<BluetoothGattCharacteristic>()
    private val batteryStatusChars = ArrayList<BluetoothGattCharacteristic>()
    private val extraWritables = ArrayList<BluetoothGattCharacteristic>()
    private var batteriesFromIdentifier = false

    private var gatt: BluetoothGatt? = null
    private var gattOp: CompletableDeferred<Int>? = null
    private var pendingPermissionAction: (() -> Unit)? = null
    private var reconnectJob: Job? = null
    private var customEqJob: Job? = null
    private var eqHoldUntilMs: Long = 0L
    @Volatile private var raceReply: CompletableDeferred<ByteArray>? = null
    private var reconnectAttempts = 0
    private var userInitiated = false
    private var servicesRequested = false
    private var attMtu: Int = 23
    private val raceSpp = RaceSpp { logLine(it) }
    private val eqLibrary = EqLibrary(context)
    private var multipointPending = 0
    private val multipointScratch = ArrayList<BtHost>()

    init {
        scope.launch {
            for (op in ops) {
                try {
                    op()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // a single failed op must not stall the queue
                }
            }
        }
    }

    // ------------------------------------------------------------------
    //  Permissions
    // ------------------------------------------------------------------

    private fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        }
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun hasPermission(name: String): Boolean =
        context.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun withPermissions(action: () -> Unit) {
        if (hasPermissions()) {
            action()
        } else {
            pendingPermissionAction = action
            activity?.requestPermissions(requiredPermissions(), REQUEST_CODE_PERMISSIONS)
        }
    }

    @SuppressLint("NewApi")
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode != REQUEST_CODE_PERMISSIONS) return
        val action = pendingPermissionAction
        pendingPermissionAction = null
        if (hasPermissions()) {
            action?.invoke()
        } else {
            postMessage("Bluetooth permission is required to control the earbuds")
        }
    }

    // ------------------------------------------------------------------
    //  Scanning
    // ------------------------------------------------------------------

    fun scan() {
        withPermissions {
            val bleScanner = adapter?.bluetoothLeScanner
            if (bleScanner == null) {
                postMessage("Bluetooth is unavailable. Turn on Bluetooth first.")
                return@withPermissions
            }
            if (!(adapter?.isEnabled == true)) {
                postMessage("Bluetooth is off. Turn it on first.")
                return@withPermissions
            }
            stopScan()
            scanning = true
            scanResults = emptyList()
            try {
                bleScanner.startScan(
                    null, ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build(), scanCallback
                )
            } catch (e: SecurityException) {
                scanning = false
                postMessage("Could not start scanning: permission denied")
            }
        }
    }

    fun stopScan() {
        try {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {
        }
        scanning = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: return
            if (name.isBlank()) return
            val marshall = name.contains("MOTIF", ignoreCase = true) ||
                    name.contains("Marshall", ignoreCase = true) ||
                    name.contains("Willem", ignoreCase = true) ||
                    name.contains("Major", ignoreCase = true) ||
                    name.contains("Monitor", ignoreCase = true)
            scanResults = scanResults.filterNot { it.address == device.address } +
                    ScanDevice(name, device.address, result.rssi, marshall)
            scanning = true
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            postMessage("Bluetooth scan failed (error $errorCode)")
        }
    }

    // ------------------------------------------------------------------
    //  Connection
    // ------------------------------------------------------------------

    fun hasSavedDevice(): Boolean =
        preferences.getString("device_address", "").isNullOrEmpty().not() ||
            knownDevices.isNotEmpty()

    fun savedDeviceName(): String = preferences.getString("device_name", "") ?: ""

    fun restoreSavedDevice() {
        refreshBondedDevices()
        val address = preferences.getString("device_address", "") ?: return
        if (address.isEmpty()) return
        connect(address, userInitiated = false)
    }

    fun connect(address: String) = connect(address, userInitiated = true)

    fun switchTo(address: String) {
        if (address.isBlank()) return
        if (state.connected && state.deviceAddress.equals(address, ignoreCase = true)) return
        val label = knownDevices.firstOrNull { it.address.equals(address, ignoreCase = true) }?.name
            ?: address
        postMessage("Switching to $label")
        connect(address, userInitiated = true)
    }

    fun refreshBondedDevices() {
        val bonded = try {
            adapter?.bondedDevices
        } catch (_: SecurityException) {
            null
        } ?: return
        var changed = false
        val next = knownDevices.toMutableList()
        for (device in bonded) {
            val raw = device.name.orEmpty()
            if (!isGattTargetName(raw)) continue
            val address = device.address ?: continue
            if (next.any { it.address.equals(address, ignoreCase = true) }) continue
            next += KnownDevice(
                address = address,
                name = raw.removeSuffix(" [LE]").trim().ifBlank { "Marshall" },
                lastConnectedAt = 0L,
            )
            changed = true
        }
        if (changed) persistKnownDevices(next)
    }

    fun forgetKnownDevice(address: String) {
        if (address.isBlank()) return
        val wasCurrent = state.deviceAddress.equals(address, ignoreCase = true) ||
            (preferences.getString("device_address", "") ?: "").equals(address, ignoreCase = true)
        persistKnownDevices(knownDevices.filterNot { it.address.equals(address, ignoreCase = true) })
        if (!wasCurrent) {
            postMessage("Device forgotten")
            return
        }
        userInitiated = true
        reconnectJob?.cancel()
        closeGatt()
        val next = knownDevices.firstOrNull()
        if (next != null) {
            preferences.edit()
                .putString("device_address", next.address)
                .putString("device_name", next.name)
                .apply()
            state = DeviceState(deviceAddress = next.address, deviceName = next.name)
        } else {
            preferences.edit()
                .remove("device_address")
                .remove("device_name")
                .apply()
            state = DeviceState()
        }
        publishWidgetState()
        postMessage("Device forgotten")
    }

    private fun connect(address: String, userInitiated: Boolean) {
        withPermissions {
            val device = try {
                adapter?.getRemoteDevice(address)
            } catch (e: IllegalArgumentException) {
                postMessage("Invalid device address")
                return@withPermissions
            } ?: run {
                postMessage("Bluetooth adapter unavailable")
                return@withPermissions
            }
            this.userInitiated = userInitiated
            reconnectJob?.cancel()
            stopScan()
            closeGatt()
            val knownName = knownDevices.firstOrNull {
                it.address.equals(address, ignoreCase = true)
            }?.name.orEmpty()
            state = state.copy(
                connecting = true,
                connected = false,
                deviceAddress = address,
                deviceName = knownName.ifEmpty { state.deviceName },
            )
            rememberDevice(address, state.deviceName.ifBlank { knownName })
            logLine("connect -> $address")
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }
    }

    fun disconnect() {
        userInitiated = true
        reconnectJob?.cancel()
        closeGatt()
        state = state.copy(connected = false, connecting = false)
        publishWidgetState()
        postMessage("Disconnected")
    }

    fun forgetDevice() {
        val address = state.deviceAddress.ifEmpty {
            preferences.getString("device_address", "") ?: ""
        }
        if (address.isNotEmpty()) {
            forgetKnownDevice(address)
            return
        }
        userInitiated = true
        reconnectJob?.cancel()
        closeGatt()
        preferences.edit()
            .remove("device_address")
            .remove("device_name")
            .apply()
        state = DeviceState()
        publishWidgetState()
        postMessage("Device forgotten")
    }

    private fun rememberDevice(address: String, name: String) {
        if (address.isBlank()) return
        val label = name.removeSuffix(" [LE]").trim().ifBlank { "Marshall" }
        val existing = knownDevices.firstOrNull { it.address.equals(address, ignoreCase = true) }
        val entry = KnownDevice(
            address = address,
            name = label,
            lastConnectedAt = System.currentTimeMillis(),
        )
        val next = if (existing == null) {
            knownDevices + entry
        } else {
            knownDevices.map { if (it.address.equals(address, ignoreCase = true)) entry else it }
        }
        persistKnownDevices(next)
        preferences.edit()
            .putString("device_address", address)
            .putString("device_name", label)
            .apply()
    }

    private fun loadKnownDevices(): List<KnownDevice> {
        val parsed = parseKnownDevices(preferences.getString(PREF_KNOWN_DEVICES, null))
        val legacyAddress = preferences.getString("device_address", "") ?: ""
        val legacyName = preferences.getString("device_name", "") ?: ""
        if (legacyAddress.isEmpty() ||
            parsed.any { it.address.equals(legacyAddress, ignoreCase = true) }
        ) {
            return parsed.sortedByDescending { it.lastConnectedAt }
        }
        return (parsed + KnownDevice(legacyAddress, legacyName.ifBlank { "Marshall" }, 0L))
            .sortedByDescending { it.lastConnectedAt }
    }

    private fun persistKnownDevices(list: List<KnownDevice>) {
        val ordered = list
            .distinctBy { it.address.uppercase() }
            .sortedByDescending { it.lastConnectedAt }
        knownDevices = ordered
        val arr = JSONArray()
        ordered.forEach { device ->
            arr.put(
                JSONObject()
                    .put("a", device.address)
                    .put("n", device.name)
                    .put("t", device.lastConnectedAt),
            )
        }
        preferences.edit().putString(PREF_KNOWN_DEVICES, arr.toString()).apply()
    }

    private fun parseKnownDevices(raw: String?): List<KnownDevice> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val address = obj.optString("a").trim()
                    if (address.isEmpty()) continue
                    add(
                        KnownDevice(
                            address = address,
                            name = obj.optString("n").ifBlank { "Marshall" },
                            lastConnectedAt = obj.optLong("t"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun isGattTargetName(name: String): Boolean {
        if (name.isBlank()) return false
        val motif = name.contains("MOTIF", ignoreCase = true)
        // Motif Classic (A2DP) shares the name without [LE] and is the wrong GATT target.
        if (motif) return name.contains("[LE]", ignoreCase = true)
        return name.contains("Marshall", ignoreCase = true) ||
            name.contains("Willem", ignoreCase = true) ||
            name.contains("Major", ignoreCase = true) ||
            name.contains("Monitor", ignoreCase = true)
    }

    fun onDestroy() {
        stopScan()
        reconnectJob?.cancel()
        scope.cancel()
        closeGatt()
    }

    private fun closeGatt() {
        gattOp?.complete(BluetoothGatt.GATT_FAILURE)
        gattOp = null
        if (gatt != null) {
            try {
                gatt?.disconnect()
            } catch (_: Exception) {
            }
            try {
                gatt?.close()
            } catch (_: Exception) {
            }
        }
        gatt = null
        attMtu = 23
        raceSpp.close()
        chars.clear()
        standardBatteryChars.clear()
        batteryStatusChars.clear()
        extraWritables.clear()
        batteriesFromIdentifier = false
        servicesRequested = false
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    logLine("connected, discovering services")
                    servicesRequested = false
                    try {
                        // Short BLE intervals starve A2DP and make the buds crackle.
                        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
                        if (gatt.device.bondState == BluetoothDevice.BOND_NONE) {
                            gatt.device.createBond()
                        }
                        if (gatt.requestMtu(517) != true) {
                            requestServices(gatt)
                        }
                    } catch (e: SecurityException) {
                        connectionFailed("Permission denied during connection")
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    val wasConnected = state.connected
                    closeGatt()
                    state = state.copy(
                        connected = false,
                        connecting = false,
                        eqPreset = -1,
                        leAudioConfigAvailable = false,
                        leAudioPresent = false,
                        leAudioEnabled = false,
                        leAudioRaw = "",
                        multipointAvailable = false,
                        multipointHosts = emptyList(),
                        findRinging = null,
                    )
                    publishWidgetState()
                    if (wasConnected) postMessage("Device disconnected")
                    scheduleReconnect()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            attMtu = mtu
            logLine("mtu $mtu (write max ${mtu - 3})")
            requestServices(gatt)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connectionFailed("Connected but services could not be loaded")
                return
            }
            val gattMap = dumpGatt(gatt)
            populateCharacteristics(gatt)
            val rawName = gatt.device.name ?: ""
            val name = rawName.removeSuffix(" [LE]").trim()
            state = state.copy(
                connected = true,
                connecting = false,
                eqPreset = -1,
                deviceName = name.ifEmpty { state.deviceName },
                availableChars = chars.keys.toSet(),
                gattMap = gattMap,
                leAudioConfigAvailable = chars.containsKey(MarshallGatt.LE_AUDIO_CONFIG),
                multipointAvailable = chars.containsKey(MarshallGatt.BT_CONNECTION_CONTROL),
            )
            reconnectAttempts = 0
            rememberDevice(gatt.device.address, state.deviceName)
            postMessage("Connected to " + state.deviceName)
            gattMap.forEach { logLine(it) }
            scope.launch {
                readAll()
                subscribeAll()
                resumeAirohaDsp()
                raceSpp.ensureConnected(adapter)
                refreshMultipoint()
                maybeRestoreCustomEq()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            completeGattOp(status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    applyValue(characteristic, characteristic.value)
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            completeGattOp(status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    logLine("write ok " + friendlyName(characteristic.uuid))
                } else {
                    logLine("write fail " + friendlyName(characteristic.uuid) + " status=$status")
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            completeGattOp(status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            applyValue(characteristic, characteristic.value)
        }
    }

    private fun connectionFailed(reason: String) {
        closeGatt()
        state = state.copy(connecting = false, connected = false)
        publishWidgetState()
        postMessage(reason)
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (state.connected || userInitiated || !hasSavedDevice()) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(1000L * minOf(1 shl reconnectAttempts, 30))
            if (state.connected || userInitiated) return@launch
            reconnectAttempts++
            postMessage("Reconnecting\u2026")
            connect(
                state.deviceAddress.ifEmpty { preferences.getString("device_address", "") ?: "" },
                userInitiated = false
            )
        }
    }

    // ------------------------------------------------------------------
    //  Characteristic discovery
    // ------------------------------------------------------------------

    private fun populateCharacteristics(gatt: BluetoothGatt) {
        chars.clear()
        standardBatteryChars.clear()
        batteryStatusChars.clear()
        extraWritables.clear()
        batteriesFromIdentifier = false
        for (service: BluetoothGattService in gatt.services) {
            for (characteristic in service.characteristics) {
                val uuid = characteristic.uuid
                val resolved = MarshallGatt.resolve(uuid)
                when {
                    resolved != null -> chars[resolved] = characteristic
                    uuid == MarshallGatt.LEFT_BATTERY -> chars[uuid] = characteristic
                    uuid == MarshallGatt.RIGHT_BATTERY -> chars[uuid] = characteristic
                    uuid == MarshallGatt.CASE_BATTERY -> chars[uuid] = characteristic
                    uuid == MarshallGatt.BATTERY_LEVEL -> standardBatteryChars.add(characteristic)
                    uuid == MarshallGatt.BATTERY_LEVEL_STATUS -> batteryStatusChars.add(characteristic)
                    MarshallGatt.INFO.contains(uuid) -> chars[uuid] = characteristic
                    else -> {
                        logLine("unknown char $uuid props=${characteristic.properties}")
                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE) ||
                            hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
                        ) {
                            extraWritables.add(characteristic)
                        }
                    }
                }
            }
        }
        lookupByUuid(gatt, MarshallGatt.EQ_CUSTOM, MarshallGatt.eqCustomAliases())
        lookupByUuid(gatt, MarshallGatt.EQ_SETTINGS, MarshallGatt.eqSettingsAliases())
        lookupByUuid(gatt, MarshallGatt.GRAPHICAL_EQ, MarshallGatt.graphicalEqAliases())
        lookupByUuid(gatt, MarshallGatt.TONE_CONTROL, MarshallGatt.toneControlAliases())
        lookupByUuid(gatt, MarshallGatt.AIROHA_TX, listOf(MarshallGatt.AIROHA_TX))
        lookupByUuid(gatt, MarshallGatt.AIROHA_RX, listOf(MarshallGatt.AIROHA_RX))
        lookupByUuid(gatt, MarshallGatt.LE_AUDIO_CONFIG, MarshallGatt.leAudioConfigAliases())
        lookupByUuid(gatt, MarshallGatt.BT_CONNECTION_CONTROL, listOf(MarshallGatt.BT_CONNECTION_CONTROL))
        logLine(
            "le audio 003d=${chars.containsKey(MarshallGatt.LE_AUDIO_CONFIG)} " +
                "multipoint=${chars.containsKey(MarshallGatt.BT_CONNECTION_CONTROL)}",
        )
        logLine(
            "eq paths: settings=${chars.containsKey(MarshallGatt.EQ_SETTINGS)} " +
                "custom0018=${chars.containsKey(MarshallGatt.EQ_CUSTOM)} " +
                "graphical000f=${chars.containsKey(MarshallGatt.GRAPHICAL_EQ)} " +
                "tone0025=${chars.containsKey(MarshallGatt.TONE_CONTROL)} " +
                "airoha=${chars.containsKey(MarshallGatt.AIROHA_TX)} " +
                "extraWrites=${extraWritables.size}",
        )
    }

    /** Android often omits a char from the iterator but still returns it by UUID. */
    private fun lookupByUuid(gatt: BluetoothGatt, key: UUID, aliases: List<UUID>) {
        if (chars.containsKey(key)) return
        for (service in gatt.services) {
            for (alias in aliases) {
                val found = try {
                    service.getCharacteristic(alias)
                } catch (_: Exception) {
                    null
                }
                if (found != null) {
                    chars[key] = found
                    logLine("found ${friendlyName(key)} as $alias on ${service.uuid}")
                    return
                }
            }
        }
        logLine("lookup miss ${friendlyName(key)}")
    }

    private fun dumpGatt(gatt: BluetoothGatt): List<String> {
        val lines = ArrayList<String>()
        for (service in gatt.services) {
            lines += "svc ${service.uuid}"
            for (characteristic in service.characteristics) {
                val rw = buildString {
                    if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) append('R')
                    if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE)) append('W')
                    if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) append('w')
                    if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)) append('N')
                    if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_INDICATE)) append('I')
                }
                lines += "  ${characteristic.uuid} $rw"
            }
        }
        return lines
    }

    private fun completeGattOp(status: Int, extra: () -> Unit = {}) {
        val op = gattOp ?: return
        gattOp = null
        extra()
        if (!op.isCompleted) op.complete(status)
    }

    private fun requestServices(gatt: BluetoothGatt) {
        if (servicesRequested) return
        servicesRequested = true
        try {
            gatt.discoverServices()
        } catch (e: SecurityException) {
            connectionFailed("Permission denied during connection")
        }
    }

    private fun refreshGattCache(gatt: BluetoothGatt) {
        try {
            val refreshed = gatt.javaClass.getMethod("refresh").invoke(gatt) as? Boolean
            logLine("gatt cache refresh=$refreshed")
        } catch (_: Exception) {
        }
    }

    private suspend fun readAll() {
        val readable = chars.entries
            .filter { (uuid, characteristic) ->
                uuid !in SKIP_IDLE_CHARS &&
                    hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)
            }
            .map { it.value }
        for (characteristic in readable) {
            ops.send { gattRead(characteristic) }
        }
        for (characteristic in batteryStatusChars) {
            if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) {
                ops.send { gattRead(characteristic) }
            }
        }
        for (characteristic in standardBatteryChars) {
            if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) {
                ops.send { gattRead(characteristic) }
            }
        }
    }

    private fun hasDedicatedBatteries(): Boolean =
        chars.containsKey(MarshallGatt.LEFT_BATTERY) ||
            chars.containsKey(MarshallGatt.RIGHT_BATTERY) ||
            chars.containsKey(MarshallGatt.CASE_BATTERY)

    private suspend fun subscribeAll() {
        val wanted = chars.entries
            .filter { (uuid, characteristic) ->
                uuid in NOTIFY_CHARS &&
                    (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY) ||
                        hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_INDICATE))
            }
            .map { it.value } +
            standardBatteryChars.filter {
                hasProperty(it, BluetoothGattCharacteristic.PROPERTY_NOTIFY) ||
                    hasProperty(it, BluetoothGattCharacteristic.PROPERTY_INDICATE)
            }
        for (characteristic in wanted) {
            ops.send { enableNotifications(characteristic) }
        }
    }

    // ------------------------------------------------------------------
    //  Low level GATT helpers
    // ------------------------------------------------------------------

    private suspend fun gattRead(characteristic: BluetoothGattCharacteristic): Int {
        val deferred = CompletableDeferred<Int>()
        gattOp = deferred
        val started = try {
            gatt?.readCharacteristic(characteristic) == true
        } catch (e: SecurityException) {
            false
        }
        if (!started) {
            gattOp = null
            deferred.complete(BluetoothGatt.GATT_FAILURE)
            return BluetoothGatt.GATT_FAILURE
        }
        return deferred.await()
    }

    private suspend fun gattWrite(characteristic: BluetoothGattCharacteristic, value: ByteArray): Boolean {
        val deferred = CompletableDeferred<Int>()
        gattOp = deferred
        val started = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt?.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
                        BluetoothGatt.GATT_SUCCESS
            } else {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                characteristic.value = value
                gatt?.writeCharacteristic(characteristic) == true
            }
        } catch (e: SecurityException) {
            false
        }
        if (!started) {
            gattOp = null
            deferred.complete(BluetoothGatt.GATT_FAILURE)
            return false
        }
        return deferred.await() == BluetoothGatt.GATT_SUCCESS
    }

    private suspend fun enableNotifications(characteristic: BluetoothGattCharacteristic): Boolean {
        val current = gatt ?: return false
        return try {
            if (!current.setCharacteristicNotification(characteristic, true)) return false
            val descriptor = characteristic.getDescriptor(MarshallGatt.CCC) ?: return false
            descriptor.value = if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_INDICATE)) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            val deferred = CompletableDeferred<Int>()
            gattOp = deferred
            val started = current.writeDescriptor(descriptor)
            if (!started) {
                gattOp = null
                deferred.complete(BluetoothGatt.GATT_FAILURE)
            }
            deferred.await() == BluetoothGatt.GATT_SUCCESS
        } catch (e: SecurityException) {
            false
        }
    }

    private suspend fun write(uuid: UUID, value: ByteArray, required: Boolean = true): Boolean {
        val characteristic = chars[uuid] ?: run {
            logLine("skip ${friendlyName(uuid)}: not on this device")
            if (required) postMessage("Not available on this device")
            return false
        }
        logLine("write ${friendlyName(uuid)}: " + value.toHex())
        val result = CompletableDeferred<Boolean>()
        ops.send { result.complete(gattWrite(characteristic, value)) }
        return result.await()
    }

    private fun hasProperty(characteristic: BluetoothGattCharacteristic, property: Int): Boolean =
        (characteristic.properties and property) != 0

    // ------------------------------------------------------------------
    //  Value parsing
    // ------------------------------------------------------------------

    private fun applyValue(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        if (value == null || value.isEmpty()) return
        val uuid = MarshallGatt.canonical(characteristic.uuid)
        when (uuid) {
            MarshallGatt.ANC_CONFIG -> state = state.copy(ancMode = value[0].toInt())
            MarshallGatt.ANC_VALUE ->
                state = state.copy(ancStrength = Protocol.snapStrength(value[0].toInt() and 0xff))

            MarshallGatt.TRANSPARENCY_VALUE ->
                state = state.copy(
                    transparencyStrength = Protocol.snapStrength(value[0].toInt() and 0xff),
                )

            MarshallGatt.UI_SOUNDS -> state = state.copy(soundsEnabled = Protocol.soundsFromValue(value))
            MarshallGatt.TOUCH_LOCK -> state = state.copy(touchEnabled = Protocol.touchLockFromValue(value))
            MarshallGatt.WEAR_SENSOR_ACTION -> state =
                state.copy(wearDetectEnabled = Protocol.wearDetectFromValue(value))

            MarshallGatt.AUDIO_CONTROL -> state = state.copy(playing = Protocol.playingFromValue(value))
            MarshallGatt.NOW_PLAYING -> {
                val (title, artist) = Protocol.metadataFromValue(value)
                state = state.copy(trackTitle = title, trackArtist = artist)
            }

            MarshallGatt.EQ_SETTINGS -> {
                val parsed = Protocol.eqPresetFromValue(value) ?: return
                if (System.currentTimeMillis() < eqHoldUntilMs && parsed != state.eqPreset) {
                    logLine("ignore stale EQ notify $parsed (holding ${state.eqPreset})")
                    return
                }
                logLine("eq preset $parsed from ${value.toHex()}")
                state = state.copy(eqPreset = parsed)
            }
            MarshallGatt.EQ_CUSTOM, MarshallGatt.GRAPHICAL_EQ ->
                state = state.copy(customEq = Protocol.customEqFromValue(value))
            MarshallGatt.AIROHA_RX, MarshallGatt.AIROHA_META -> {
                logLine("notify RACE: " + value.toHex())
                raceReply?.let { pending ->
                    if (!pending.isCompleted) pending.complete(value.copyOf())
                }
                // Device-originated request (05 5A) — ACK it so the race session stays up.
                if (value.size >= 6 && (value[1].toInt() and 0xff) == 0x5A) {
                    val ack = byteArrayOf(
                        0x05, 0x5B, 0x03, 0x00, value[4], value[5], 0x00,
                    )
                    scope.launch { writeNoResponse(MarshallGatt.AIROHA_TX, ack) }
                }
                return
            }
            MarshallGatt.BT_CONNECTION_CONTROL -> applyBtConnection(value)
            MarshallGatt.LE_AUDIO_CONFIG -> {
                val parsed = Protocol.leAudioFromValue(value)
                logLine(
                    "le audio present=${parsed.present} enabled=${parsed.enabled} raw=${value.toHex()}",
                )
                state = state.copy(
                    leAudioConfigAvailable = true,
                    leAudioPresent = parsed.present,
                    leAudioEnabled = parsed.enabled,
                    leAudioRaw = value.toHex(),
                )
            }
            MarshallGatt.ECO_CHARGING -> state = state.copy(batterySaverPreset = Protocol.batterySaverFromValue(value))
            MarshallGatt.TOUCH_MAP -> {
                val (left, right) = Protocol.touchMapsFromValue(value)
                state = state.copy(touchLeft = left, touchRight = right)
            }

            MarshallGatt.LEFT_BATTERY -> if (!batteriesFromIdentifier) {
                state = state.copy(leftBattery = batteryPercent(value))
            }
            MarshallGatt.RIGHT_BATTERY -> if (!batteriesFromIdentifier) {
                state = state.copy(rightBattery = batteryPercent(value))
            }
            MarshallGatt.CASE_BATTERY -> if (!batteriesFromIdentifier) {
                state = state.copy(caseBattery = batteryPercent(value))
            }
            MarshallGatt.BATTERY_LEVEL -> applyStandardBattery(characteristic, value)
            MarshallGatt.BATTERY_LEVEL_STATUS -> return
            MarshallGatt.MANUFACTURER -> state = state.copy(manufacturer = String(value).trim())
            MarshallGatt.MODEL_NUMBER -> state = state.copy(model = String(value).trim())
            MarshallGatt.SERIAL_NUMBER -> state = state.copy(serial = String(value).trim())
            MarshallGatt.FIRMWARE_REVISION -> state = state.copy(firmware = String(value).trim())
            MarshallGatt.HARDWARE_REVISION -> state = state.copy(hardware = String(value).trim())
            else -> return
        }
        publishWidgetState()
        logLine("notify ${friendlyName(uuid)}: " + value.toHex())
    }

    private fun publishWidgetState() {
        WidgetStateStore.save(
            context = context,
            left = state.leftBattery,
            right = state.rightBattery,
            case = state.caseBattery,
            ancMode = state.ancMode,
            name = state.deviceName,
            connected = state.connected,
        )
    }

    private fun batteryPercent(value: ByteArray): Int =
        (value[0].toInt() and 0xff).coerceIn(0, 100)

    /**
     * Generic 0x2A19 instances have no L/R/case meaning unless Battery Level
     * Status names them (13=left, 14=right, 6=case) — same ids Android uses
     * on the system Device details screen. Discovery-order mapping rotated
     * Motif's three percentages. Prefer those identifiers; otherwise keep
     * the Tymphany L/R/case UUIDs.
     */
    private fun applyStandardBattery(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        val percent = batteryPercent(value)
        val identified = identifierSlot(statusBytes(characteristic))
        val slot = identified ?: if (hasDedicatedBatteries()) {
            null
        } else {
            when (standardBatteryChars.indexOf(characteristic)) {
                0 -> BatterySlot.LEFT
                1 -> BatterySlot.CASE
                2 -> BatterySlot.RIGHT
                else -> null
            }
        }
        if (identified != null) batteriesFromIdentifier = true
        when (slot) {
            BatterySlot.LEFT -> state = state.copy(leftBattery = percent)
            BatterySlot.RIGHT -> state = state.copy(rightBattery = percent)
            BatterySlot.CASE -> state = state.copy(caseBattery = percent)
            null -> Unit
        }
    }

    private enum class BatterySlot { LEFT, RIGHT, CASE }

    private fun statusBytes(level: BluetoothGattCharacteristic): ByteArray? = try {
        level.service?.getCharacteristic(MarshallGatt.BATTERY_LEVEL_STATUS)?.value
    } catch (_: Exception) {
        null
    }

    /** Official BatteryServiceWrapper.BatteryType ids. */
    private fun identifierSlot(status: ByteArray?): BatterySlot? {
        if (status == null || status.isEmpty()) return null
        val flags = status[0].toInt() and 0xff
        val identifier = if (flags and 0x01 != 0 && status.size >= 5) {
            status[3].toInt() and 0xff
        } else if (status.size >= 4) {
            status[3].toInt() and 0xff
        } else {
            return null
        }
        return when (identifier) {
            14 -> BatterySlot.LEFT
            13 -> BatterySlot.RIGHT
            6 -> BatterySlot.CASE
            else -> null
        }
    }

    // ------------------------------------------------------------------
    //  High level device controls (called from UI)
    // ------------------------------------------------------------------

    fun setAncMode(mode: Int) {
        state = state.copy(ancMode = mode)
        publishWidgetState()
        scope.launch { write(MarshallGatt.ANC_CONFIG, Protocol.encodeAncMode(mode)) }
    }

    fun setAncStrength(strength: Int) {
        val snapped = Protocol.snapStrength(strength)
        state = state.copy(ancStrength = snapped)
        scope.launch { write(MarshallGatt.ANC_VALUE, Protocol.encodeStrength(snapped)) }
    }

    fun setTransparencyStrength(strength: Int) {
        val snapped = Protocol.snapStrength(strength)
        state = state.copy(transparencyStrength = snapped)
        scope.launch { write(MarshallGatt.TRANSPARENCY_VALUE, Protocol.encodeStrength(snapped)) }
    }

    fun setAncStrengthLevel(level: Int) = setAncStrength(Protocol.strengthFromLevel(level))

    fun setTransparencyStrengthLevel(level: Int) =
        setTransparencyStrength(Protocol.strengthFromLevel(level))

    fun refreshEq() {
        if (!state.connected) return
        scope.launch {
            val characteristic = chars[MarshallGatt.EQ_SETTINGS] ?: return@launch
            if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) {
                ops.send { gattRead(characteristic) }
            }
        }
    }

    fun refreshMultipoint() {
        if (!chars.containsKey(MarshallGatt.BT_CONNECTION_CONTROL)) return
        scope.launch { write(MarshallGatt.BT_CONNECTION_CONTROL, Protocol.btConnectionList(), required = false) }
    }

    fun removeMultipointHost(id: Int) {
        scope.launch {
            write(MarshallGatt.BT_CONNECTION_CONTROL, Protocol.btConnectionRemove(id), required = false)
            delay(200)
            refreshMultipoint()
        }
    }

    fun disconnectMultipointHost(id: Int) {
        scope.launch {
            write(MarshallGatt.BT_CONNECTION_CONTROL, Protocol.btConnectionDisconnect(id), required = false)
            delay(200)
            refreshMultipoint()
        }
    }

    fun ringBuds(channel: Int, start: Boolean) {
        scope.launch {
            if (!raceSpp.ensureConnected(adapter)) {
                postMessage("Classic SPP not up — cannot ring")
                return@launch
            }
            val packet = Protocol.findMePacket(channel, start)
            val ok = raceSpp.send(packet)
            logLine("find-me ch=$channel start=$start ok=$ok ${packet.toHex()}")
            if (ok) {
                state = state.copy(findRinging = if (start) channel else null)
            }
            postMessage(
                when {
                    !ok -> "Ring failed"
                    !start -> "Stopped ringing"
                    channel == 1 -> "Ringing left"
                    channel == 2 -> "Ringing right"
                    else -> "Ringing both"
                },
            )
        }
    }

    private fun applyBtConnection(value: ByteArray) {
        if (value.isEmpty()) return
        logLine("multipoint ${value.toHex()}")
        when (value[0].toInt() and 0xff) {
            Protocol.BT_CMD_LIST -> {
                val ids = value.drop(2).map { it.toInt() and 0xff }.filter { it != 0 }
                multipointScratch.clear()
                multipointPending = ids.size
                if (ids.isEmpty()) {
                    state = state.copy(multipointAvailable = true, multipointHosts = emptyList())
                } else {
                    scope.launch {
                        write(
                            MarshallGatt.BT_CONNECTION_CONTROL,
                            Protocol.btConnectionQuery(ids),
                            required = false,
                        )
                    }
                }
            }
            Protocol.BT_CMD_QUERY -> {
                val host = Protocol.parseBtHostQuery(value.copyOfRange(1, value.size))
                if (host != null) {
                    multipointScratch.removeAll { it.id == host.id }
                    multipointScratch.add(host)
                }
                multipointPending = (multipointPending - 1).coerceAtLeast(0)
                if (multipointPending == 0) {
                    state = state.copy(
                        multipointAvailable = true,
                        multipointHosts = multipointScratch.toList().sortedBy { it.id },
                    )
                }
            }
        }
    }

    private suspend fun maybeRestoreCustomEq() {
        delay(400)
        if (state.eqPreset != Protocol.EqPreset.CUSTOM.id) return
        val last = eqLibrary.lastBands()
        if (last.all { it == 0 } && state.customEq.all { it == 0 }) return
        val named = eqLibrary.activeName()?.takeIf { name ->
            eqLibrary.snapshots().any { it.name == name && it.bands == last }
        }
        setCustomEq(last, fromSnapshot = named)
        logLine("restored custom eq ${last.joinToString()}")
    }

    fun eqSnapshots(): List<EqSnapshot> = eqLibrary.snapshots()

    fun saveEqSnapshot(name: String) {
        eqLibrary.saveSnapshot(name, state.customEq)
        eqLibrary.setActive(name.trim())
        state = state.copy(customEqSnapshot = name.trim())
        postMessage("Saved “${name.trim()}”")
    }

    fun deleteEqSnapshot(name: String) {
        eqLibrary.deleteSnapshot(name)
        if (state.customEqSnapshot == name) {
            state = state.copy(customEqSnapshot = null)
        }
    }

    fun applyEqSnapshot(name: String) {
        val snap = eqLibrary.snapshots().firstOrNull { it.name == name } ?: return
        setCustomEq(snap.bands, fromSnapshot = snap.name)
    }

    fun setLeAudioEnabled(enabled: Boolean) {
        state = state.copy(leAudioEnabled = enabled)
        scope.launch {
            val packet = Protocol.encodeLeAudioEnabled(enabled)
            val ok = write(MarshallGatt.LE_AUDIO_CONFIG, packet)
            logLine("le audio write enabled=$enabled ok=$ok")
            postMessage(
                if (ok) {
                    if (enabled) "LE Audio flag written. Forget and re-pair to test LC3."
                    else "LE Audio flag cleared."
                } else {
                    "LE Audio config not available on this firmware."
                },
            )
            chars[MarshallGatt.LE_AUDIO_CONFIG]?.let { characteristic ->
                if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) {
                    ops.send { gattRead(characteristic) }
                }
            }
        }
    }

    fun setEqPreset(presetId: Int) {
        holdEqPreset(presetId)
        scope.launch {
            for (packet in Protocol.eqPresetWrites(presetId)) {
                writeFast(MarshallGatt.EQ_SETTINGS, packet)
            }
        }
    }

    private fun holdEqPreset(presetId: Int) {
        eqHoldUntilMs = System.currentTimeMillis() + 900
        state = state.copy(eqPreset = presetId)
    }

    fun setCustomEqBand(index: Int, value: Int) {
        if (index !in 0..4) return
        val next = state.customEq.toMutableList().apply {
            while (size < 5) add(0)
            this[index] = value.coerceIn(-6, 6)
        }
        setCustomEq(next)
    }

    fun setCustomEq(bands: List<Int>, fromSnapshot: String? = null) {
        val next = (0 until 5).map { bands.getOrElse(it) { 0 }.coerceIn(-6, 6) }
        holdEqPreset(Protocol.EqPreset.CUSTOM.id)
        val active = fromSnapshot
            ?: eqLibrary.snapshots().firstOrNull { it.bands == next }?.name
        eqLibrary.setActive(active)
        state = state.copy(customEq = next, customEqSnapshot = active)
        customEqJob?.cancel()
        customEqJob = scope.launch {
            delay(220)
            try {
                writeCustomEqNow(next)
            } catch (_: CancellationException) {
                // A newer slider commit replaced this write.
            } catch (e: Exception) {
                logLine("custom eq crash: ${e.message}")
            }
        }
    }

    /** Unstick A2DP after 0x0E00 DSP suspend froze playback. */
    private suspend fun resumeAirohaDsp() {
        if (!chars.containsKey(MarshallGatt.AIROHA_TX)) {
            gatt?.let { lookupByUuid(it, MarshallGatt.AIROHA_TX, listOf(MarshallGatt.AIROHA_TX)) }
        }
        if (!chars.containsKey(MarshallGatt.AIROHA_TX)) return
        for (packet in Protocol.airohaResumePackets()) {
            writeNoResponse(MarshallGatt.AIROHA_TX, packet)
            delay(30)
        }
        logLine("airoha DSP resume sent")
    }

    /**
     * Official Marshall path: Airoha RACE over Classic SPP
     * (UUID 00000000-0000-0000-0099-AABBCCDDEEFF), not BLE GATT.
     */
    private suspend fun writeCustomEqNow(bands: List<Int>) {
        val packet = AirohaPeq.realtimeUpdate(bands, maxPacketBytes = 4096)
        if (packet == null) {
            logLine("custom eq: native peq failed")
            return
        }
        if (!raceSpp.ensureConnected(adapter)) {
            logLine("custom eq: SPP not connected — GATT fallback")
            writeNoResponse(MarshallGatt.AIROHA_TX, packet)
            return
        }
        val ok = raceSpp.send(packet)
        if (ok) eqLibrary.saveLast(bands)
        logLine("custom eq bands=${bands.joinToString()} spp=$ok bytes=${packet.size}")
    }

    private suspend fun raceExchange(request: ByteArray): ByteArray? {
        val deferred = CompletableDeferred<ByteArray>()
        raceReply = deferred
        if (!writeNoResponse(MarshallGatt.AIROHA_TX, request)) {
            raceReply = null
            return null
        }
        val reply = withTimeoutOrNull(900) { deferred.await() }
        if (raceReply === deferred) raceReply = null
        return reply
    }

    private suspend fun tryWritePayloads(uuid: UUID, payloads: List<ByteArray>): Boolean {
        if (!chars.containsKey(uuid)) return false
        for (payload in payloads) {
            if (writeFast(uuid, payload)) return true
        }
        return false
    }

    /** Prefer write-without-response so EQ taps are not two round-trips. */
    private suspend fun writeFast(uuid: UUID, value: ByteArray): Boolean {
        val characteristic = chars[uuid] ?: return false
        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
            if (writeNoResponse(uuid, value)) return true
        }
        return write(uuid, value, required = false)
    }

    private suspend fun writeNoResponse(uuid: UUID, value: ByteArray): Boolean {
        val characteristic = chars[uuid] ?: return false
        logLine("writeNR ${friendlyName(uuid)}: " + value.toHex())
        val result = CompletableDeferred<Boolean>()
        ops.send {
            val started = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt?.writeCharacteristic(
                        characteristic,
                        value,
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
                    ) == BluetoothGatt.GATT_SUCCESS
                } else {
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    characteristic.value = value
                    gatt?.writeCharacteristic(characteristic) == true
                }
            } catch (_: SecurityException) {
                false
            }
            result.complete(started)
        }
        return result.await()
    }

    fun setSounds(enabled: Boolean) {
        state = state.copy(soundsEnabled = enabled)
        scope.launch {
            // Prefer double-byte FF form; fall back to single-byte 01/00 if needed.
            val ok = write(MarshallGatt.UI_SOUNDS, Protocol.encodeSounds(enabled))
            if (!ok) {
                write(MarshallGatt.UI_SOUNDS, Protocol.encodeSoundsSingle(enabled))
            }
        }
    }

    fun setTouchEnabled(enabled: Boolean) {
        state = state.copy(touchEnabled = enabled)
        scope.launch { write(MarshallGatt.TOUCH_LOCK, Protocol.encodeTouchLock(enabled)) }
    }

    fun setWearDetect(enabled: Boolean) {
        state = state.copy(wearDetectEnabled = enabled)
        scope.launch { write(MarshallGatt.WEAR_SENSOR_ACTION, Protocol.encodeWearDetect(enabled)) }
    }

    fun setBatterySaver(preset: String) {
        state = state.copy(batterySaverPreset = preset)
        scope.launch { write(MarshallGatt.ECO_CHARGING, Protocol.batterySaverBytes(preset)) }
    }

    fun setTouchAction(left: Boolean, gesture: Int, action: TouchAction) {
        val newLeft = if (left) state.touchLeft.with(gesture, action.byte) else state.touchLeft
        val newRight = if (!left) state.touchRight.with(gesture, action.byte) else state.touchRight
        state = state.copy(touchLeft = newLeft, touchRight = newRight)
        scope.launch {
            write(MarshallGatt.TOUCH_MAP, Protocol.encodeTouchMap(newLeft, newRight))
        }
    }

    fun renameDevice(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        state = state.copy(deviceName = clean)
        scope.launch { write(MarshallGatt.RENAME, clean.toByteArray(Charsets.UTF_8)) }
    }

    /** Best-effort play/pause command over the AUDIO_CONTROL channel. */
    fun togglePlay() {
        val command = if (state.playing) 0x01 else 0x00
        scope.launch { write(MarshallGatt.AUDIO_CONTROL, byteArrayOf(command.toByte())) }
    }

    fun consumeMessage() {
        message = null
    }

    private fun postMessage(text: String) {
        message = text
    }

    private fun logLine(line: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date())
        logs = (logs + "[$time] $line").takeLast(150)
    }

}

internal fun ByteArray.toHex(): String =
    joinToString(" ") { "%02X".format(it) }
