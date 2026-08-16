# Contributing

PRs and issues are welcome, especially logs from devices that are not Motif II.

The public name is **Unofficial Marshall**. Do not drop the word unofficial
from the launcher label.

## Ground rules

- Do not commit `apks/`, `tools/tmp/`, keystores, or decompiled official sources.
- Keep GATT writes on the documented `0017` opcodes only (`0x01` assign, `0x00` activate).
- Custom Motif EQ stays on Classic SPP. Do not add DSP suspend (`0x0E00`).

## Useful logs

Settings → scroll the device log. Helpful lines:

- `spp: connected` / `spp: connect failed`
- `custom eq bands=… spp=true`
- `eq paths: settings=… airoha=…`

## Build

JDK 17, then `gradlew.bat :app:assembleDebug`.
