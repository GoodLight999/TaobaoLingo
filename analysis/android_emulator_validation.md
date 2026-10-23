# Android emulator execution verification

## First successful execution (2026-10-23 JST)

GitHub Actions [run 39879100073](https://github.com/GoodLight999/TaobaoLingo/actions/runs/39879100073) completed with `conclusion: success`, for commit `e646f4dfbe850a267bc0dca974211fa5084d909d`.

| Check | Outcome |
| --- | --- |
| Gradle build of app and instrumentation APK | PASS |
| Checksum-verified Android emulator 36.6.11 binary | PASS |
| Android 15 Google APIs x86_64 emulator boot | PASS |
| Install app + instrumentation APK via adb | PASS |
| Launch MainActivity | PASS |
| UIAutomator view tree contains TaobaoLingo and translation start button | PASS |
| App process alive | PASS |
| Chinese ML Kit OCR on programmatically drawn Chinese product text | **PASS**, 1 test, 4.247 sec |
| Screenshots and UI XML collected | PASS |

The generated emulator screenshot and `ocr-test.log` are available as the workflow diagnostic artifact `taobaolingo-emulator-smoke-diagnostics`. The UI is visible and the language picker/start/stop buttons render correctly.

**What this does not prove:** Taobao's ARM64-only app cannot run on this x86_64 image without a compatible ARM translation layer. Actual Taobao login, screen-capture permission, translation model downloads, and the UI overlay on Taobao are not covered in the first passing smoke test.

## Follow-up testing

A separate Chinese-to-Japanese ML Kit download-and-inference instrumentation test was added subsequently. Its pass/failure is tracked by later Actions runs. The screenshot-based OCR test is deterministic and uses no Taobao account or screenshot.

## Reliable emulator bootstrap

The Android Emulator latest SDK package download was corrupt in CI. Fixed emulator build 36.6.11 (SHA-256: `1eade4cf2df6ea8eeead4902c635897ba12aaa32aac4389eaae0fdb498a5b830`) was downloaded, and OS libraries installed. The Android-35 system image lacked device metadata expected by `avdmanager`, so the successful workflow created valid AVD metadata (`config.ini`, `.ini`) directly before booting.

The test is reproducible using [`.github/workflows/overlay-smoke.yml`](../.github/workflows/overlay-smoke.yml).
