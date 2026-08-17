package com.marshall.motif.ble

import java.util.UUID

/**
 * GATT profile for Marshall (Zound Industries) Bluetooth devices.
 *
 * UUID schemes:
 *  - Modern Zound:    `0000{code}-1337-1dea-feed-c0ffee70c0de`
 *  - Zound legacy:    `0000{code}-0000-1000-8000-00805f9b34fb`
 *  - Bluetooth std:   `0000{code}-0000-1000-8000-00805f9b34fb`
 *  - Tymphany earbuds battery (Motif II ANC)
 *
 * See: reverse-engineered protocol notes in the project README.
 */
object MarshallGatt {

    /** Zound primary service (modern). */
    val PRIMARY_SERVICE: UUID = UUID.fromString("FA302D24-D775-4343-B9ED-8CC68ACE3284")

    /** Common legacy BLE service used by older builds / clones. */
    val LEGACY_SERVICE: UUID = UUID.fromString("0000fccd-0000-1000-8000-00805f9b34fb")

    /** Client Characteristic Configuration Descriptor. */
    val CCC: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Standard Battery service + characteristic. */
    val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL_STATUS: UUID = UUID.fromString("00002bed-0000-1000-8000-00805f9b34fb")

    /**
     * Tymphany earbud batteries (Motif II). Official headers name 50001
     * “right” and 50002 “left”, but on Motif those are swapped vs the
     * physical buds / Android Device details.
     */
    val LEFT_BATTERY: UUID = UUID.fromString("7a573e5d-9330-4d9b-8660-63c33fc50001")
    val RIGHT_BATTERY: UUID = UUID.fromString("7a573e5d-9330-4d9b-8660-63c33fc50002")
    val CASE_BATTERY: UUID = UUID.fromString("7a573e5d-9330-4d9b-8660-63c33fc50003")

    // ---- Device Information (Bluetooth standard) ----
    val MANUFACTURER: UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    val MODEL_NUMBER: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
    val SERIAL_NUMBER: UUID = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
    val FIRMWARE_REVISION: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
    val HARDWARE_REVISION: UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")

    // ---- Zound proprietary characteristics ----
    val RENAME: UUID = UUID.fromString("00000003-1337-1dea-feed-c0ffee70c0de")
    val VOLUME: UUID = UUID.fromString("00000007-1337-1dea-feed-c0ffee70c0de")
    val AUDIO_CONTROL: UUID = UUID.fromString("00000009-1337-1dea-feed-c0ffee70c0de")
    val NOW_PLAYING: UUID = UUID.fromString("0000000a-1337-1dea-feed-c0ffee70c0de")
    val UI_SOUNDS: UUID = UUID.fromString("0000000b-1337-1dea-feed-c0ffee70c0de")
    /** Legacy UI sounds characteristic. */
    val UI_SOUNDS_LEGACY: UUID = UUID.fromString("0000aa12-0000-1000-8000-00805f9b34fb")
    val ACTION_BUTTON_EVENT: UUID = UUID.fromString("0000000c-1337-1dea-feed-c0ffee70c0de")
    val TOUCH_MAP: UUID = UUID.fromString("0000000d-1337-1dea-feed-c0ffee70c0de")
    val GRAPHICAL_EQ: UUID = UUID.fromString("0000000f-1337-1dea-feed-c0ffee70c0de")
    val GRAPHICAL_EQ_LEGACY: UUID = UUID.fromString("0000aa16-0000-1000-8000-00805f9b34fb")
    val ANC_CONFIG: UUID = UUID.fromString("00000013-1337-1dea-feed-c0ffee70c0de")
    val ANC_CONFIG_LEGACY: UUID = UUID.fromString("0000aa20-0000-1000-8000-00805f9b34fb")
    val TOUCH_LOCK: UUID = UUID.fromString("00000014-1337-1dea-feed-c0ffee70c0de")
    val EQ_SETTINGS: UUID = UUID.fromString("00000017-1337-1dea-feed-c0ffee70c0de")
    val EQ_SETTINGS_LEGACY: UUID = UUID.fromString("0000aa25-0000-1000-8000-00805f9b34fb")
    val EQ_CUSTOM: UUID = UUID.fromString("00000018-1337-1dea-feed-c0ffee70c0de")
    val EQ_CUSTOM_LEGACY: UUID = UUID.fromString("0000aa26-0000-1000-8000-00805f9b34fb")
    val EQ_CUSTOM_SIG: UUID = UUID.fromString("00000018-0000-1000-8000-00805f9b34fb")
    val TRANSPARENCY_VALUE: UUID = UUID.fromString("00000019-1337-1dea-feed-c0ffee70c0de")
    val ANC_VALUE: UUID = UUID.fromString("0000001a-1337-1dea-feed-c0ffee70c0de")
    val ECO_CHARGING: UUID = UUID.fromString("0000001d-1337-1dea-feed-c0ffee70c0de")
    val WEAR_SENSOR_STATUS: UUID = UUID.fromString("00000027-1337-1dea-feed-c0ffee70c0de")
    val WEAR_SENSOR_ACTION: UUID = UUID.fromString("00000028-1337-1dea-feed-c0ffee70c0de")
    val SOUNDSTAGE: UUID = UUID.fromString("00000033-1337-1dea-feed-c0ffee70c0de")
    val TONE_CONTROL: UUID = UUID.fromString("00000025-1337-1dea-feed-c0ffee70c0de")
    val TONE_CONTROL_LEGACY: UUID = UUID.fromString("0000aa33-0000-1000-8000-00805f9b34fb")
    val TONE_CONTROL_SIG: UUID = UUID.fromString("00000025-0000-1000-8000-00805f9b34fb")

    /**
     * Airoha RACE over GATT (Motif II CUSTOM_AIROHA_EQ).
     * Service name is ASCII "PRIM" / "irohaBLE"; chars are "CHAR" / "2A"|"1A"|"0A".
     */
    val AIROHA_SERVICE: UUID = UUID.fromString("5052494d-2dab-0341-6972-6f6861424c45")
    val AIROHA_TX: UUID = UUID.fromString("43484152-2dab-3241-6972-6f6861424c45")
    val AIROHA_RX: UUID = UUID.fromString("43484152-2dab-3141-6972-6f6861424c45")
    val AIROHA_META: UUID = UUID.fromString("43484152-2dab-3041-6972-6f6861424c45")

    /** Official LE_AUDIO_CONFIG — enable bit for LE Audio / LC3. */
    val BT_CONNECTION_CONTROL: UUID = UUID.fromString("00000034-1337-1dea-feed-c0ffee70c0de")
    val LE_AUDIO_CONFIG: UUID = UUID.fromString("0000003d-1337-1dea-feed-c0ffee70c0de")
    val LE_AUDIO_CONFIG_SIG: UUID = UUID.fromString("0000003d-0000-1000-8000-00805f9b34fb")

    /** Every characteristic we know how to talk to. */
    val KNOWN: Set<UUID> = setOf(
        RENAME, VOLUME, AUDIO_CONTROL, NOW_PLAYING, UI_SOUNDS, UI_SOUNDS_LEGACY,
        ACTION_BUTTON_EVENT,
        TOUCH_MAP, GRAPHICAL_EQ, GRAPHICAL_EQ_LEGACY, ANC_CONFIG, ANC_CONFIG_LEGACY,
        TOUCH_LOCK, EQ_SETTINGS, EQ_SETTINGS_LEGACY, EQ_CUSTOM, EQ_CUSTOM_LEGACY, EQ_CUSTOM_SIG,
        TRANSPARENCY_VALUE, ANC_VALUE, ECO_CHARGING, WEAR_SENSOR_STATUS,
        WEAR_SENSOR_ACTION, SOUNDSTAGE, TONE_CONTROL, TONE_CONTROL_LEGACY, TONE_CONTROL_SIG,
        AIROHA_TX, AIROHA_RX, AIROHA_META,
        LE_AUDIO_CONFIG, LE_AUDIO_CONFIG_SIG,
        BT_CONNECTION_CONTROL,
    )

    /** 16-bit code from any 128-bit UUID variant (modern, legacy, SIG base). */
    fun shortCode(uuid: UUID): Int = ((uuid.mostSignificantBits ushr 32) and 0xFFFFL).toInt()

    /**
     * Map a discovered characteristic onto the logical modern UUID we store in [chars].
     * Motif II firmware has been seen advertising the same feature under the 1337
     * base, the legacy aaXX base, and the Bluetooth SIG base.
     */
    fun resolve(uuid: UUID): UUID? {
        val mapped = canonical(uuid)
        if (mapped != uuid || KNOWN.contains(uuid)) return mapped
        return when (shortCode(uuid)) {
            0x0003 -> RENAME
            0x0007 -> VOLUME
            0x0009 -> AUDIO_CONTROL
            0x000A -> NOW_PLAYING
            0x000B, 0xAA12 -> UI_SOUNDS
            0x000C -> ACTION_BUTTON_EVENT
            0x000D -> TOUCH_MAP
            0x000F, 0xAA16 -> GRAPHICAL_EQ
            0x0013, 0xAA20 -> ANC_CONFIG
            0x0014, 0xAA21 -> TOUCH_LOCK
            0x0017, 0xAA25 -> EQ_SETTINGS
            0x0018, 0xAA26 -> EQ_CUSTOM
            0x0019, 0xAA27 -> TRANSPARENCY_VALUE
            0x001A, 0xAA28 -> ANC_VALUE
            0x001D, 0xAA2B -> ECO_CHARGING
            0x0025, 0xAA33 -> TONE_CONTROL
            0x0027 -> WEAR_SENSOR_STATUS
            0x0028 -> WEAR_SENSOR_ACTION
            0x0033 -> SOUNDSTAGE
            0x0034 -> BT_CONNECTION_CONTROL
            0x003D -> LE_AUDIO_CONFIG
            else -> null
        }
    }

    /** Map legacy characteristic UUIDs onto the modern logical UUID. */
    fun canonical(uuid: UUID): UUID = when (uuid) {
        UI_SOUNDS_LEGACY -> UI_SOUNDS
        GRAPHICAL_EQ_LEGACY -> GRAPHICAL_EQ
        ANC_CONFIG_LEGACY -> ANC_CONFIG
        EQ_SETTINGS_LEGACY -> EQ_SETTINGS
        EQ_CUSTOM_LEGACY, EQ_CUSTOM_SIG -> EQ_CUSTOM
        TONE_CONTROL_LEGACY, TONE_CONTROL_SIG -> TONE_CONTROL
        LE_AUDIO_CONFIG_SIG -> LE_AUDIO_CONFIG
        else -> uuid
    }

    fun eqCustomAliases(): List<UUID> = listOf(EQ_CUSTOM, EQ_CUSTOM_LEGACY, EQ_CUSTOM_SIG)

    fun eqSettingsAliases(): List<UUID> = listOf(
        EQ_SETTINGS,
        EQ_SETTINGS_LEGACY,
        UUID.fromString("00000017-0000-1000-8000-00805f9b34fb"),
    )

    fun graphicalEqAliases(): List<UUID> = listOf(
        GRAPHICAL_EQ,
        GRAPHICAL_EQ_LEGACY,
        UUID.fromString("0000000f-0000-1000-8000-00805f9b34fb"),
    )

    fun leAudioConfigAliases(): List<UUID> = listOf(
        LE_AUDIO_CONFIG,
        LE_AUDIO_CONFIG_SIG,
    )

    fun toneControlAliases(): List<UUID> = listOf(
        TONE_CONTROL,
        TONE_CONTROL_LEGACY,
        TONE_CONTROL_SIG,
    )

    /** Standard device-info characteristics. */
    val INFO: Set<UUID> = setOf(
        MANUFACTURER, MODEL_NUMBER, SERIAL_NUMBER, FIRMWARE_REVISION, HARDWARE_REVISION,
    )
}

/** Friendly label for a characteristic, used in diagnostics. */
fun friendlyName(uuid: UUID): String = when (uuid) {
    MarshallGatt.RENAME -> "Rename"
    MarshallGatt.VOLUME -> "Volume"
    MarshallGatt.AUDIO_CONTROL -> "Audio control"
    MarshallGatt.NOW_PLAYING -> "Now playing"
    MarshallGatt.UI_SOUNDS -> "UI sounds"
    MarshallGatt.ACTION_BUTTON_EVENT -> "Button event"
    MarshallGatt.TOUCH_MAP -> "Touch map"
    MarshallGatt.GRAPHICAL_EQ -> "Graphical EQ"
    MarshallGatt.ANC_CONFIG -> "ANC config"
    MarshallGatt.TOUCH_LOCK -> "Touch lock"
    MarshallGatt.EQ_SETTINGS -> "EQ settings"
    MarshallGatt.EQ_CUSTOM -> "Custom EQ"
    MarshallGatt.TRANSPARENCY_VALUE -> "Transparency level"
    MarshallGatt.ANC_VALUE -> "ANC level"
    MarshallGatt.ECO_CHARGING -> "Eco charging"
    MarshallGatt.WEAR_SENSOR_STATUS -> "Wear sensor"
    MarshallGatt.WEAR_SENSOR_ACTION -> "Auto play/pause"
    MarshallGatt.TONE_CONTROL -> "Tone control"
    MarshallGatt.SOUNDSTAGE -> "Soundstage"
    MarshallGatt.AIROHA_TX -> "Airoha RACE TX"
    MarshallGatt.AIROHA_RX -> "Airoha RACE RX"
    MarshallGatt.AIROHA_META -> "Airoha RACE meta"
    MarshallGatt.LE_AUDIO_CONFIG, MarshallGatt.LE_AUDIO_CONFIG_SIG -> "LE Audio config"
    MarshallGatt.BT_CONNECTION_CONTROL -> "Multipoint"
    MarshallGatt.RIGHT_BATTERY -> "Battery R"
    MarshallGatt.LEFT_BATTERY -> "Battery L"
    MarshallGatt.CASE_BATTERY -> "Battery case"
    MarshallGatt.BATTERY_LEVEL -> "Battery level"
    else -> uuid.toString().take(8)
}
