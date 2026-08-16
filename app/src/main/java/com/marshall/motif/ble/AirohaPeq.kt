package com.marshall.motif.ble

import com.airoha.libNativePeq.NativePeq
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Motif II (CustomEQType.PLANT) custom EQ from the official Marshall app.
 *
 * Five user bands, all BAND_PASS, gains ±6 dB. Realtime RACE 0x0E03 payload
 * is NativePeq coefficients per sample rate — not a raw DSP_PARA_PEQ_STRU.
 */
object AirohaPeq {

    val BAND_HZ: IntArray = intArrayOf(160, 400, 1000, 2500, 6250)
    private val BAND_Q: FloatArray = floatArrayOf(0.7f, 0.7f, 1.0f, 1.0f, 1.0f)
    private val RATES = intArrayOf(44100, 48000, 88200, 96000)
    private val RATE_IDS = shortArrayOf(1, 2, 5, 6)

    const val RACE_SUSPEND = 0x0E00
    const val RACE_RESUME = 0x0E01
    const val RACE_PEQ = 0x0E03
    const val RACE_OPEN_AEQ = 0x0E0A

    /** Enable adaptive/user PEQ path (Airoha OPEN_AEQ). */
    fun openAeq(): ByteArray = Protocol.raceRequest(RACE_OPEN_AEQ, byteArrayOf(0x01))

    /**
     * Realtime PEQ SET: setting_mode=1, target=A2DP, phase=0, nvkey 0
     * (apply now, do not persist), then DSP_PARA_PEQ_STRU of 5 peaks.
     */
    fun suspendDsp(): ByteArray = Protocol.raceRequest(RACE_SUSPEND, byteArrayOf(0x00))

    fun resumeDsp(): ByteArray = Protocol.raceRequest(RACE_RESUME, byteArrayOf(0x00))

    fun peqSetIir(bands: List<Int>, nvkey: Int = 0, target: Int = 0): ByteArray {
        return realtimeUpdate(bands) ?: run {
            val payload = byteArrayOf(0x01, target.toByte(), 0x00, 0x00) +
                shortLe(nvkey) +
                graphicNvkey(bands)
            Protocol.raceRequest(RACE_PEQ, payload)
        }
    }

    /**
     * Official PeqStageRealTimeUpdate payload:
     *   target(1) + rateCount(2) + reserved(2) +
     *   repeating [rateId(2) + coefCount(2) + native coefs]
     */
    fun realtimeUpdate(bandsDb: List<Int>, maxPacketBytes: Int = 240): ByteArray? {
        val gains = (0 until 5).map { bandsDb.getOrElse(it) { 0 }.coerceIn(-6, 6).toDouble() }
        val chunks = ArrayList<ByteArray>()
        try {
            RATES.forEachIndexed { index, hz ->
                NativePeq.setParam(0, hz.toDouble(), 5, 1, 0, 0)
                BAND_HZ.forEachIndexed { band, freq ->
                    NativePeq.setPeqPoint(0, band, freq.toDouble(), gains[band], BAND_Q[band].toDouble())
                }
                if (NativePeq.generateCofe(0) != 0) return null
                NativePeq.changeRescaleCofe(0, 0.0)
                val count = NativePeq.getCofeCount(0).toShort()
                val coefs = shortsToLe(NativePeq.getCofeParam(0))
                chunks += shortLe(RATE_IDS[index].toInt()) + shortLe(count.toInt()) + coefs
            }
        } catch (_: Throwable) {
            return null
        }
        // Prefer 44.1 + 48 so the packet fits a 247-byte MTU; add 88.2/96 if MTU allows.
        val ordered = chunks.toMutableList()
        fun pack(n: Int): ByteArray {
            val body = ArrayList<Byte>()
            body += 0x00
            shortLe(n).forEach { body += it }
            body += 0x00
            body += 0x00
            repeat(n) { i -> ordered[i].forEach { body += it } }
            return Protocol.raceRequest(RACE_PEQ, body.toByteArray())
        }
        for (n in ordered.size downTo 1) {
            val packet = pack(n)
            if (packet.size <= maxPacketBytes) return packet
        }
        return pack(1)
    }

    private fun shortsToLe(values: ShortArray): ByteArray {
        val out = ByteArray(values.size * 2)
        values.forEachIndexed { i, s ->
            val v = s.toInt()
            out[i * 2] = (v and 0xff).toByte()
            out[i * 2 + 1] = ((v shr 8) and 0xff).toByte()
        }
        return out
    }

    fun peqGet(): ByteArray = Protocol.raceRequest(RACE_PEQ, byteArrayOf(0x00, 0x00, 0x00, 0x00))

    /** Switch the active PEQ index (1 = Marshall Custom). */
    fun peqSelectIndex(index: Int): ByteArray =
        Protocol.raceRequest(RACE_PEQ, byteArrayOf(0x02, 0x00, index.toByte(), 0x00))

    /**
     * Build a SET from a GET response so we keep the firmware's nvkey / path
     * and only replace the filter payload.
     */
    fun peqSetFromGet(reply: ByteArray, bands: List<Int>): ByteArray {
        // 05 5B | len | cmd | [status] setting target phase reserved [nvkey_le] [data]
        if (reply.size < 8) return peqSetIir(bands)
        var i = 6
        // optional status byte on responses
        if (i < reply.size && (reply[i].toInt() and 0xff) <= 1) i++
        val rest = if (i < reply.size) reply.copyOfRange(i, reply.size) else byteArrayOf()
        var nvkey = 0
        if (rest.size >= 6) {
            nvkey = (rest[4].toInt() and 0xff) or ((rest[5].toInt() and 0xff) shl 8)
        }
        val dataLen = (rest.size - 6).coerceAtLeast(0)
        return if (dataLen in 5..16 && dataLen < 40) {
            // firmware stored compact gains, not a full IIR blob
            val payload = byteArrayOf(0x01, rest.getOrElse(1) { 0 }, 0x00, 0x00) +
                shortLe(nvkey) + Protocol.customEqBytes(bands)
            Protocol.raceRequest(RACE_PEQ, payload)
        } else {
            peqSetIir(bands, nvkey)
        }
    }

    /**
     * Alternate SET some OEM stacks accept: 5 typed bands
     * (type / Hz / gain×100 / Q×100) instead of raw IIR.
     */
    fun peqSetParams(bands: List<Int>): ByteArray {
        val out = ArrayList<Byte>(4 + 1 + 5 * 7)
        out += 0x01
        out += 0x00
        out += 0x00
        out += 0x00
        out += 0x05
        BAND_HZ.forEachIndexed { index, hz ->
            val gain = bands.getOrElse(index) { 0 }.coerceIn(-12, 12)
            out += 0x06 // peaking
            shortLe(hz).forEach { out += it }
            shortLe(gain * 100).forEach { out += it }
            shortLe(100).forEach { out += it } // Q = 1.00
        }
        return Protocol.raceRequest(RACE_PEQ, out.toByteArray())
    }

    /**
     * EQ_SETTINGS only accepts 2–3 byte packets on Motif II.
     * Opcode 0x02 = set one custom band, gain stored 0..24 (12 = 0 dB).
     */
    fun eqSettingsBandWrites(bands: List<Int>): List<ByteArray> =
        (0 until 5).map { index ->
            val gain = bands.getOrElse(index) { 0 }.coerceIn(-12, 12)
            byteArrayOf(0x02, index.toByte(), (gain + 12).toByte())
        }

    /**
     * DSP_PARA_PEQ_STRU: numOfElement, peqAlgorithm=0, then per band
     * elementID=4 (peaking), numOfParameter=5, Q27 b0 b1 b2 a1 a2.
     * AB1565 PEQ coefs are Q27, not Q23.
     */
    fun graphicNvkey(bands: List<Int>): ByteArray {
        val out = ArrayList<Byte>(4 + 5 * (4 + 20))
        shortLe(5).forEach { out += it }
        shortLe(0).forEach { out += it }
        BAND_HZ.forEachIndexed { index, hz ->
            val gain = bands.getOrElse(index) { 0 }.coerceIn(-12, 12).toDouble()
            val coef = peakingQ27(hz.toDouble(), gain, q = 1.0, fs = 48_000.0)
            shortLe(4).forEach { out += it }
            shortLe(5).forEach { out += it }
            coef.forEach { value -> intLe(value).forEach { b -> out += b } }
        }
        return out.toByteArray()
    }

    private fun peakingQ27(hz: Double, gainDb: Double, q: Double, fs: Double): IntArray {
        if (gainDb == 0.0) {
            return intArrayOf(q27(1.0), 0, 0, 0, 0)
        }
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * hz / fs
        val alpha = sin(w0) / (2.0 * q)
        val cosw = cos(w0)
        val b0 = 1.0 + alpha * a
        val b1 = -2.0 * cosw
        val b2 = 1.0 - alpha * a
        val a0 = 1.0 + alpha / a
        val a1 = -2.0 * cosw
        val a2 = 1.0 - alpha / a
        return intArrayOf(
            q27(b0 / a0),
            q27(b1 / a0),
            q27(b2 / a0),
            q27(a1 / a0),
            q27(a2 / a0),
        )
    }

    private fun q27(value: Double): Int =
        (value * (1 shl 27)).roundToInt().coerceIn(Int.MIN_VALUE, Int.MAX_VALUE)

    private fun shortLe(value: Int): ByteArray = byteArrayOf(
        (value and 0xff).toByte(),
        ((value shr 8) and 0xff).toByte(),
    )

    private fun intLe(value: Int): ByteArray = byteArrayOf(
        (value and 0xff).toByte(),
        ((value shr 8) and 0xff).toByte(),
        ((value shr 16) and 0xff).toByte(),
        ((value shr 24) and 0xff).toByte(),
    )
}
