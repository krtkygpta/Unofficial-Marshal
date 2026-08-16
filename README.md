# Unofficial Marshall

A fast companion for Marshall Bluetooth earbuds and headphones. Not the
official Marshall app, not affiliated with Marshall Group, Zound, or Airoha.

The name uses “Marshall” only to say **what devices it talks to**. The
launcher title is **Unofficial Marshall** on purpose so it is not passed
off as Marshall Bluetooth.

The stock app is slow. This is a small Compose stand-in: BLE connect, ANC /
EQ / touch, and (on Motif II) the same custom-EQ path the official app uses.

**Tested:** Marshall Motif II A.N.C.  
**Likely works:** other Zound / Marshall devices on the same BLE profile
(see [Supported devices](#supported-devices)).

## Package name / official app

This build uses the **same Android package name** as official Marshall
Bluetooth:

`com.zoundindustries.marshallbt`

Some Motif / Airoha behaviour (SPP RACE, pairing identity, firmware checks)
keys off that id. Using it lets this APK spoof those checks.

**You cannot install this next to the official app.** Android allows one
app per package. Uninstall Marshall Bluetooth first, then sideload. Putting
the official app back later replaces this one (or the install is refused if
the signing keys differ).

## Features

- Scan and pair Motif-class devices (`MOTIF II A.N.C. [LE]`)
- Battery for left, right, and case
- ANC / Transparency / Off plus 4-level strength
- EQ presets (Marshall, Custom, Bass boost, Mid boost, Treble, Mid reduction)
- **Custom 5-band EQ** on Motif II (official Airoha SPP + `libnative-peq`)
- Touch map, wear detect, UI sounds
- Home-screen 2×2 widget (battery + ANC pages)
- Light / dark / system theme

## Requirements

- Android 8.0+ (API 26)
- **arm64-v8a** phone (the native PEQ library is 64-bit only)
- Bluetooth on; Motif II must already be paired for A2DP (music)

## Install

A signed release APK is produced by:

```bat
gradlew.bat assembleRelease
```

Output:

`app/build/outputs/apk/release/app-release.apk`

Sideload it **after** uninstalling official Marshall Bluetooth (same package
id — they cannot coexist). The home-screen name is **Unofficial Marshall**.

## Build from source

Need JDK 17 and Android SDK 35.

```bat
gradlew.bat :app:assembleDebug
gradlew.bat :app:assembleRelease
```

Release signing: copy `keystore.properties.example` to `keystore.properties`,
point `storeFile` at a JKS, and fill in the passwords. If that file is
missing, release is signed with the Android debug key (fine for personal use,
not for Play).

## Custom EQ (Motif II)

Presets go over BLE GATT (`EQUALIZER_SETTINGS` / `0017`) on **profile 2**.

Slider bands go over **Bluetooth Classic SPP** to the Airoha RACE UUID
`00000000-0000-0000-0099-AABBCCDDEEFF` — the same path as the official app.

| Band     | Frequency |
|----------|-----------|
| Bass     | 160 Hz    |
| Low mid  | 400 Hz    |
| Mid      | 1 kHz     |
| High mid | 2.5 kHz   |
| Treble   | 6.25 kHz  |

Range is **±6 dB**. The buds must already be paired as a normal Bluetooth
headset so Classic SPP can open.

## Supported devices

GATT layout comes from the [marshall-protocol](https://github.com/c0ffee-audio/marshall-protocol)
notes (Marshall Bluetooth app 3.7.1). Same stack as:

- Motif II A.N.C. (tested)
- Motif A.N.C., Minor III / IV, Major IV / V, Monitor II / III A.N.C.
- Other Zound devices using `0000xxxx-1337-1dea-feed-c0ffee70c0de`

What works per model:

| Feature              | Motif II | Other Zound devices      |
|----------------------|----------|--------------------------|
| Connect / batteries  | Yes      | If they expose the chars |
| ANC + strength       | Yes      | If `0013` / `0019`/`001a`|
| EQ presets           | Yes      | If `0017` exists         |
| Custom 5-band EQ     | Yes      | Motif II Airoha path     |
| Touch / wear         | Yes      | If those chars exist     |
| Home widget          | Yes      | Yes                      |

Older Motif / speakers may use Graphical EQ (`000f`), custom preset (`0018`),
or tone control (`0025`) instead of Airoha SPP. Those writes are attempted
when the characteristic is present.

## Protocol

See [docs/PROTOCOL.md](docs/PROTOCOL.md).

## Project layout

```
app/src/main/java/com/marshall/motif/   UI + widget
app/src/main/java/com/marshall/motif/ble/  GATT, SPP, Motif EQ
app/src/main/jniLibs/arm64-v8a/         libnative-peq.so
docs/                                   protocol notes
```

## Disclaimer

Unofficial. Use at your own risk. Firmware differs by model and version;
if something misbehaves, disconnect, put the buds in the case, and reopen
the official app once.

## License

MIT — see [LICENSE](LICENSE). Native PEQ and Marshall trademarks: [NOTICE](NOTICE).
