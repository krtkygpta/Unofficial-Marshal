package com.marshall.motif.ble

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

/** A single touch gesture -> action mapping for one ear. */
data class TouchMap(
    val single: Int = 0x12,
    val double: Int = 0x13,
    val triple: Int = 0x0e,
    val long: Int = 0x06,
    val hold: Int = 0x09,
) {
    operator fun get(gesture: Int): Int = when (gesture) {
        0 -> single
        1 -> double
        2 -> triple
        3 -> long
        4 -> hold
        else -> 0x00
    }

    fun with(gesture: Int, action: Int): TouchMap = when (gesture) {
        0 -> copy(single = action)
        1 -> copy(double = action)
        2 -> copy(triple = action)
        3 -> copy(long = action)
        4 -> copy(hold = action)
        else -> this
    }

    companion object {
        val GESTURES = listOf("Single tap", "Double tap", "Triple tap", "Long press", "Tap & hold")

        fun fromPacket(bytes: ByteArray, offset: Int): TouchMap {
            if (bytes.size - offset < 5) return TouchMap()
            return TouchMap(
                single = bytes[offset].toInt() and 0xff,
                double = bytes[offset + 1].toInt() and 0xff,
                triple = bytes[offset + 2].toInt() and 0xff,
                long = bytes[offset + 3].toInt() and 0xff,
                hold = bytes[offset + 4].toInt() and 0xff,
            )
        }
    }
}

/** Immutable snapshot of everything we know about the connected device. */
data class DeviceState(
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val deviceName: String = "",
    val deviceAddress: String = "",

    val leftBattery: Int? = null,
    val rightBattery: Int? = null,
    val caseBattery: Int? = null,

    val ancMode: Int = 0,                    // 0 off, 1 anc, 2 transparency
    val ancStrength: Int = 50,               // snapped to 25/50/75/100 (4 levels)
    val transparencyStrength: Int = 50,      // snapped to 25/50/75/100 (4 levels)

    val playing: Boolean = false,
    val trackTitle: String = "",
    val trackArtist: String = "",

    val soundsEnabled: Boolean = true,
    val touchEnabled: Boolean = true,
    val wearDetectEnabled: Boolean = true,

    val eqPreset: Int = 0,                   // preset id, see EqPreset
    /** Five custom EQ bands in dB, from -12 to +12. */
    val customEq: List<Int> = listOf(0, 0, 0, 0, 0),
    val batterySaverPreset: String = "none", // none / standard / medium / max

    val touchLeft: TouchMap = TouchMap(),
    val touchRight: TouchMap = TouchMap(),

    val manufacturer: String = "",
    val model: String = "",
    val serial: String = "",
    val firmware: String = "",
    val hardware: String = "",
    /** Characteristics that were actually found on the connected device. */
    val availableChars: Set<UUID> = emptySet(),
    /** Full GATT map (service / characteristic / properties) for diagnostics. */
    val gattMap: List<String> = emptyList(),
) {
    fun has(uuid: UUID): Boolean = availableChars.contains(uuid)
}

/** Touch gesture actions understood by the earbuds. */
enum class TouchAction(val byte: Int, val label: String, val subtitle: String) {
    NOTHING(0x00, "Do nothing", "Disable this gesture"),
    ASSISTANT(0x01, "Voice assistant", "Trigger the phone assistant"),
    ANC_OFF_TRA(0x04, "ANC: Off \u2194 Transparency", "Cycle noise control"),
    ANC_ALL(0x05, "ANC: full cycle", "Off \u2194 ANC \u2194 Transparency"),
    ANC_TRA(0x06, "ANC \u2194 Transparency", "Cycle noise control"),
    ANC_OFF(0x07, "ANC \u2194 Off", "Cycle noise control"),
    SPOTIFY(0x09, "Spotify Tap", "Launch Spotify playback"),
    VOLUME_UP(0x0a, "Volume up", "Raise the volume"),
    VOLUME_DOWN(0x0b, "Volume down", "Lower the volume"),
    PLAY_CALL(0x0c, "Play / pause + calls", "Media and call control"),
    PREVIOUS(0x0e, "Previous track", "Jump to previous track"),
    PLAY_PAUSE(0x12, "Play / pause", "Toggle playback"),
    NEXT(0x13, "Next track", "Jump to next track");

    companion object {
        fun fromByte(byte: Int): TouchAction =
            entries.firstOrNull { it.byte == byte } ?: NOTHING
    }
}
