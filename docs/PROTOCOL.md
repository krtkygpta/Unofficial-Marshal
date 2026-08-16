# Protocol

Unofficial notes for this app. GATT names follow
[c0ffee-audio/marshall-protocol](https://github.com/c0ffee-audio/marshall-protocol).

## Dual radio (Motif II)

| Radio | Address | Used for |
|-------|---------|----------|
| BR/EDR (Classic) | Public, e.g. `00:25:D1:…` | A2DP music, Airoha RACE over SPP |
| BLE | Random, name `MOTIF II A.N.C. [LE]` | Zound GATT: ANC, presets, batteries, touch |

Custom EQ **must** go over Classic SPP. BLE GATT `PRIM`/`CHAR` accepts RACE
frames but Motif II does not apply them to the music DSP.

SPP UUID: `00000000-0000-0000-0099-AABBCCDDEEFF`

## EQ presets (`0017`)

Two writes, **step 1** (profile 2):

1. `[0x01, 0x01, presetId]` — assign  
2. `[0x00, 0x01]` — activate  

Motif earbud IDs used here: 0 Marshall, 1 Custom, 2 Bass boost, 3 Mid boost,
4 Treble boost, 5 Mid reduction.

Status notify `FF 02 <step> <p0> <p1> <p2> …` — byte 2 is the active step,
bytes 3–5 are the three slot presets. Ignore 2-byte `[0x00, step]` echoes.

## Custom EQ (Motif II / `CustomEQType.PLANT`)

Official Marshall app (`AirohaController`):

1. Connect SPP with `BtUpgradeMode.BTC`
2. Build `AirohaEQPayload` for category **101**
3. Five user bands, all `BAND_PASS` (type 2), Q `0.7, 0.7, 1, 1, 1`
4. Frequencies 160 / 400 / 1000 / 2500 / 6250 Hz, gain **−6…+6 dB**
5. `NativePeq` generates coefficients for 44.1 / 48 / 88.2 / 96 kHz
6. Realtime RACE `0x0E03` (`3587`) payload:

```
target:u8 = 0          # A2DP
rateCount:u16le
reserved:u16le = 0
repeat rateCount:
  rateId:u16le         # 1=44.1, 2=48, 5=88.2, 6=96
  coefCount:u16le
  coefs:u16le[coefCount]
```

Slider moves are realtime (`SaveOrNot = false`). A later save path writes
NVKEYs; this app only does realtime.

Do **not** follow a custom write with `0017` assign/activate Custom — that
reloads the stored curve and throws the sliders away.

Do **not** send DSP suspend `0x0E00` — it can mute A2DP until the buds reset.

BLE ATT MTU is often 247; a 4-rate packet is ~450 bytes. SPP has no such
limit. If you must use GATT, request MTU 517 or send 44.1+48 only.

## Other EQ feature paths

When the characteristic exists we still try:

| Feature | Char | Payload |
|---------|------|---------|
| `EQ_CUSTOM_SETTING` | `0018` | 5× signed or 0–24 centred dB |
| `TWO_BAND_GRAPHICAL_EQ` | `000f` | bass + treble |
| `TONE_CONTROL` | `0025` | bass / mid / treble |

Motif II does not expose `0018` / `000f` / `0025`.

## ANC

`0013`: `00` off, `01` ANC, `02` transparency.  
`0019` / `001a`: strength 25 / 50 / 75 / 100.
