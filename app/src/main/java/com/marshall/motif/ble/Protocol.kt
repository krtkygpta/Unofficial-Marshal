package com.marshall.motif.ble

/**
 * Byte-level encodings for the Marshall BLE protocol.
 * Values derived from the reverse-engineered protocol README and the
 * previous working implementation.
 */
object Protocol {

    // ---- ANC -------------------------------------------------------

    const val ANC_OFF = 0x00
    const val ANC_ON = 0x01
    const val ANC_TRANSPARENCY = 0x02

    fun ancModeLabel(mode: Int): String = when (mode) {
        ANC_ON -> "Noise cancelling"
        ANC_TRANSPARENCY -> "Transparency"
        else -> "Playback only"
    }

    fun ancModeShortLabel(mode: Int): String = when (mode) {
        ANC_ON -> "ANC"
        ANC_TRANSPARENCY -> "Aware"
        else -> "Off"
    }

    /** Single-byte write, 0=off, 1=ANC, 2=Transparency. */
    fun encodeAncMode(mode: Int) = byteArrayOf(mode.toByte())

    /** Widget / hardware cycle: Off → ANC → Transparency → Off. */
    fun nextAncMode(mode: Int): Int = when (mode) {
        ANC_OFF -> ANC_ON
        ANC_ON -> ANC_TRANSPARENCY
        else -> ANC_OFF
    }

    /**
     * Motif II exposes four discrete ANC / transparency levels (`ANC_TRA_4_LVL`).
     * We store/send the stepped percentage the firmware expects, not a free 0–100 range.
     */
    val STRENGTH_LEVEL_VALUES: IntArray = intArrayOf(25, 50, 75, 100)

    val STRENGTH_LEVEL_LABELS: List<String> = listOf("Low", "Medium", "High", "Max")

    fun strengthLevelCount(): Int = STRENGTH_LEVEL_VALUES.size

    fun strengthFromLevel(level: Int): Int =
        STRENGTH_LEVEL_VALUES[level.coerceIn(0, STRENGTH_LEVEL_VALUES.lastIndex)]

    /** Map a raw device value onto the nearest of the four levels. */
    fun nearestStrengthLevel(value: Int): Int {
        val v = value.coerceIn(0, 100)
        var best = 0
        var bestDist = Int.MAX_VALUE
        STRENGTH_LEVEL_VALUES.forEachIndexed { i, step ->
            val d = kotlin.math.abs(step - v)
            if (d < bestDist) {
                bestDist = d
                best = i
            }
        }
        return best
    }

    fun snapStrength(value: Int): Int = strengthFromLevel(nearestStrengthLevel(value))

    /** Strength is a percentage (0..100) stored as a single byte — always snapped to 4 levels. */
    fun encodeStrength(strength: Int) = byteArrayOf(snapStrength(strength).toByte())

    // ---- EQ ---------------------------------------------------------

    /**
     * Motif II A.N.C. sound profiles shown in the official Marshall app.
     *
     * IDs match the earbud EQ step scheme (0–5), not the full speaker genre list.
     * Curve `shape` values are UI-only (-1..1) for the equaliser illustration.
     */
    enum class EqPreset(val id: Int, val label: String, val shape: FloatArray) {
        MARSHALL(0, "Marshall", floatArrayOf(0.25f, 0.15f, 0.05f, 0.15f, 0.2f)),
        CUSTOM(1, "Custom", floatArrayOf(0.35f, 0.2f, 0.1f, 0.15f, 0.25f)),
        BASS_BOOST(2, "Bass boost", floatArrayOf(1f, 0.55f, 0.05f, -0.1f, -0.2f)),
        MID_BOOST(3, "Mid boost", floatArrayOf(0.05f, 0.45f, 1f, 0.45f, 0.05f)),
        TREBLE_BOOST(4, "Treble boost", floatArrayOf(-0.25f, -0.1f, 0.1f, 0.55f, 1f)),
        MID_REDUCTION(5, "Mid reduction", floatArrayOf(0.35f, 0.1f, -0.85f, 0.1f, 0.35f));

        companion object {
            fun fromId(id: Int): EqPreset = entries.firstOrNull { it.id == id } ?: MARSHALL
        }
    }

    /**
     * Changing the EQ preset requires two writes:
     *  1. assign a preset to a step slot  [0x01, stepIndex, presetId]
     *  2. activate that step slot        [0x00, stepIndex]
     *
     * Motif II has 3 step slots (0, 1, 2) — "profile 1/2/3" in the official app.
     * We were writing slot 0 (profile 1). Slot 1 is profile 2.
     */
    const val EQ_STEP_PROFILE_2 = 0x01

    fun eqPresetWrites(presetId: Int, step: Int = EQ_STEP_PROFILE_2): List<ByteArray> {
        val slot = step.coerceIn(0, 2).toByte()
        return listOf(
            byteArrayOf(0x01, slot, presetId.toByte()),
            byteArrayOf(0x00, slot),
        )
    }

    /**
     * Parse a full EQ_SETTINGS status notify. Write echoes are ignored:
     *   [0x00, step]           CHANGE_ACTIVE_STEP — step is not a preset id
     *   [0x01, step, presetId] ASSIGN_STEP_PRESET
     * Status dump (6+ bytes): byte[2] is the active step, presets follow
     * at bytes [3], [4], [5] for slots 0/1/2.
     */
    fun eqPresetFromValue(value: ByteArray): Int? {
        if (value.isEmpty()) return null
        val b0 = value[0].toInt() and 0xff
        if (value.size == 2 && b0 == 0x00) return null
        if (value.size == 3 && b0 == 0x01) return value[2].toInt() and 0xff
        if (value.size >= 6) {
            val step = (value[2].toInt() and 0xff)
            if (step in 0..2 && value.size >= 4 + step) {
                return value[3 + step].toInt() and 0xff
            }
            return value[5].toInt() and 0xff
        }
        return null
    }

    /**
     * Custom EQ: five bands in dB.
     * Motif II / Airoha firmware usually stores signed -12..+12.
     * Some packets use unsigned 0..24 with 12 as the 0 dB centre.
     */
    fun customEqFromValue(value: ByteArray): List<Int> {
        if (value.size < 5) return listOf(4, 2, 0, 2, 4)
        val start = value.size - 5
        val signed = (start until value.size).map { value[it].toInt().coerceIn(-12, 12) }
        val unsigned = (start until value.size).map { (value[it].toInt() and 0xff) }
        val looksCentered = unsigned.all { it in 0..24 }
        return if (looksCentered && signed.any { it < 0 }.not() && unsigned.any { it > 12 }) {
            unsigned.map { (it - 12).coerceIn(-12, 12) }
        } else {
            signed
        }
    }

    fun customEqBytes(bands: List<Int>): ByteArray =
        bands.take(5).let { values ->
            ByteArray(5) { index -> (values.getOrElse(index) { 0 }.coerceIn(-12, 12)).toByte() }
        }

    /** Alternate 0..24 encoding some Airoha custom-EQ paths expect. */
    fun customEqBytesCentered(bands: List<Int>): ByteArray =
        bands.take(5).let { values ->
            ByteArray(5) { index ->
                (values.getOrElse(index) { 0 }.coerceIn(-12, 12) + 12).toByte()
            }
        }

    /**
     * The four custom-EQ feature paths from marshall-protocol
     * (`EQ_CUSTOM_SETTING`, `TWO_BAND_GRAPHICAL_EQ`, `TONE_CONTROL`,
     * `CUSTOM_AIROHA_EQ`). Each list is encodings to try on that char.
     */
    fun eqCustomSettingPayloads(bands: List<Int>): List<ByteArray> = listOf(
        customEqBytes(bands),
        customEqBytesCentered(bands),
    )

    /** TWO_BAND_GRAPHICAL_EQ on 000f: bass + treble. */
    fun twoBandGraphicalPayloads(bands: List<Int>): List<ByteArray> {
        val bass = bands.getOrElse(0) { 0 }.coerceIn(-12, 12)
        val treble = bands.getOrElse(4) { 0 }.coerceIn(-12, 12)
        return listOf(
            byteArrayOf(bass.toByte(), treble.toByte()),
            byteArrayOf((bass + 12).toByte(), (treble + 12).toByte()),
            customEqBytes(bands),
            customEqBytesCentered(bands),
        )
    }

    /** TONE_CONTROL on 0025: bass / mid / treble. */
    fun toneControlPayloads(bands: List<Int>): List<ByteArray> {
        val bass = bands.getOrElse(0) { 0 }.coerceIn(-12, 12)
        val mid = bands.getOrElse(2) { 0 }.coerceIn(-12, 12)
        val treble = bands.getOrElse(4) { 0 }.coerceIn(-12, 12)
        return listOf(
            byteArrayOf(bass.toByte(), mid.toByte(), treble.toByte()),
            byteArrayOf((bass + 12).toByte(), (mid + 12).toByte(), (treble + 12).toByte()),
            byteArrayOf(bass.toByte(), treble.toByte()),
            byteArrayOf((bass + 12).toByte(), (treble + 12).toByte()),
        )
    }

    /** Recover A2DP after a bad DSP suspend. Do not send 0x0E00 again. */
    fun airohaResumePackets(): List<ByteArray> = listOf(
        AirohaPeq.resumeDsp(),
        Protocol.raceRequest(AirohaPeq.RACE_RESUME, ByteArray(0)),
        Protocol.raceRequest(AirohaPeq.RACE_RESUME, byteArrayOf(0x01)),
    )

    fun airohaGraphicSetPackets(bands: List<Int>): List<ByteArray> = listOf(
        AirohaPeq.peqSetIir(bands, nvkey = 0, target = 0),
    )

    /**
     * Airoha RACE request: 05 5A | len_le | cmd_le | payload
     * length includes the 2-byte command id.
     */
    fun raceRequest(command: Int, payload: ByteArray = ByteArray(0)): ByteArray {
        val length = payload.size + 2
        return byteArrayOf(
            0x05,
            0x5A,
            (length and 0xff).toByte(),
            ((length shr 8) and 0xff).toByte(),
            (command and 0xff).toByte(),
            ((command shr 8) and 0xff).toByte(),
        ) + payload
    }

    /** DSP realtime PEQ — Motif II CUSTOM_AIROHA_EQ. See [AirohaPeq]. */
    const val RACE_DSPREALTIME_PEQ = AirohaPeq.RACE_PEQ

    fun airohaPeqPackets(bands: List<Int>): List<ByteArray> = airohaGraphicSetPackets(bands)

    // ---- Battery saver / eco charging -------------------------------

    val BATTERY_SAVER_PRESETS = listOf("none", "standard", "medium", "max")

    fun batterySaverLabel(preset: String): String = when (preset) {
        "standard" -> "Standard"
        "medium" -> "Medium"
        "max" -> "Max"
        else -> "None"
    }

    fun batterySaverBytes(preset: String): ByteArray = when (preset) {
        "standard" -> byteArrayOf(0x02, 0x5a, 0x04, 0x00, 0x80.toByte(), 0x80.toByte(), 0x00)
        "medium" -> byteArrayOf(0x02, 0x5a, 0x01, 0x00, 0x80.toByte(), 0x80.toByte(), 0x00)
        "max" -> byteArrayOf(0x02, 0x5a, 0x01, 0x00, 0x0f, 0x23, 0x00)
        else -> byteArrayOf(0x02, 0x64, 0x04, 0x00, 0x80.toByte(), 0x80.toByte(), 0x00)
    }

    fun batterySaverFromValue(value: ByteArray): String =
        BATTERY_SAVER_PRESETS.firstOrNull {
            value.contentEquals(batterySaverBytes(it))
        } ?: "custom"

    // ---- Simple toggles ----------------------------------------------

    /**
     * UI interaction sounds.
     * Modern Motif firmware often uses 0xFF / 0x00 (single or double byte).
     * We accept several on/off encodings when reading back.
     */
    fun encodeSounds(enabled: Boolean): ByteArray =
        if (enabled) byteArrayOf(0xff.toByte(), 0xff.toByte()) else byteArrayOf(0x00, 0x00)

    /** Alternate single-byte form some firmwares expect. */
    fun encodeSoundsSingle(enabled: Boolean): ByteArray =
        byteArrayOf(if (enabled) 0x01 else 0x00)

    fun soundsFromValue(value: ByteArray): Boolean {
        if (value.isEmpty()) return false
        val b = value[0].toInt() and 0xff
        return b == 0xff || b == 0x01 || b == 0x02 || (value.size >= 2 && (value[1].toInt() and 0xff) == 0xff)
    }

    /** Touch lock: 00 = touch enabled, 01 = touch locked. */
    fun encodeTouchLock(enabled: Boolean) = byteArrayOf(if (enabled) 0x00 else 0x01)
    fun touchLockFromValue(value: ByteArray) = value.firstOrNull()?.toInt() == 0

    /** Wear sensor action (auto play/pause): 03 on, 02 off. */
    fun encodeWearDetect(enabled: Boolean) =
        if (enabled) byteArrayOf(3, 3, 2) else byteArrayOf(2, 2, 2)
    fun wearDetectFromValue(value: ByteArray) = value.firstOrNull()?.toInt() == 3

    // ---- Touch map ----------------------------------------------------

    /**
     * Packet layout: [sideByte, single, double, triple, long, hold]
     * Left side byte = 0x00, right side byte = 0x01.
     * A full write is [left][right] = 12 bytes.
     * The read-back is padded with a 3 byte header (15 bytes).
     */
    fun encodeTouchMap(left: TouchMap, right: TouchMap): ByteArray = byteArrayOf(
        0x00,
        left.single.toByte(), left.double.toByte(), left.triple.toByte(), left.long.toByte(), left.hold.toByte(),
        0x01,
        right.single.toByte(), right.double.toByte(), right.triple.toByte(), right.long.toByte(), right.hold.toByte(),
    )

    fun touchMapsFromValue(value: ByteArray): Pair<TouchMap, TouchMap> {
        val offset = if (value.size >= 15) 3 else 0
        if (value.size - offset < 12) return TouchMap() to TouchMap()
        val left = TouchMap.fromPacket(value, offset + 1)
        val right = TouchMap.fromPacket(value, offset + 7)
        return left to right
    }

    // ---- Playback / now playing --------------------------------------

    fun playingFromValue(value: ByteArray) = value.firstOrNull()?.toInt() == 0x01

    fun metadataFromValue(value: ByteArray): Pair<String, String> {
        val text = String(value).trim()
        val parts = text.split('\u0000').map { cleanMetadata(it) }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "" to ""
        if (parts.size == 1 && parts[0].length <= 2) return "" to ""
        return if (parts.size >= 2) parts[0] to parts[1] else parts[0] to ""
    }

    private fun cleanMetadata(part: String): String {
        var cleaned = part.replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "").trim()
        while (cleaned.startsWith("=") || cleaned.startsWith("-")) {
            cleaned = cleaned.substring(1).trim()
        }
        return cleaned
    }
}
