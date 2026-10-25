# Emulator native-bridge crash and APK identity: 2026-10-25

## Evidence from previous emulator A/B

The proprietary Taobao APK and signing keys are not distributed.

| Run | Android API | Original | Re-signed only | Meaning |
| --- | --- | --- | --- | --- |
| [40058202655](https://github.com/GoodLight999/TaobaoLingo/actions/runs/40058202655) | 33 | INSTALL_FAILED_NO_MATCHING_ABIS | INSTALL_FAILED_NO_MATCHING_ABIS | Both fail to install |
| [40058204146](https://github.com/GoodLight999/TaobaoLingo/actions/runs/40058204146) | 34 | Installs, crashes | Installs, crashes | Neither is a valid UI/login baseline |

Evidence branches: ci-results-33 and ci-results-34. Both API 34 UI
hierarchies were from com.google.android.apps.nexuslauncher, not Taobao.
The green result of those older workflows did not indicate a working app.

### Native crash in the unpatched re-signed control

The archived API 34 control logcat (ci-results-34/out/resigned/logcat.txt,
approximately lines 1208 and 1276-1290) shows:

- Fatal signal 6 (SIGABRT) in the Taobao process.
- Abort message: 'Cannot process signal 11'.
- libndk_translation.so in the native stack, with
  ndk_translation::HandleHostSignal.
- x86_64 emulator fingerprint in the tombstone.

This is direct evidence of a native-bridge signal-handling abort and
a reason not to attribute this emulator failure to app-signature validation.
The original APK also crashed. Its key log includes MTOP, SecurityGuard,
libsgmain.so and ART abort paths; an equivalent full original tombstone
has not yet been compared. Missing libsgmain.dm warnings alone are
not proof of a fatal missing library.

An independent test confounder was present: the older workflow installed
the original via streaming but the re-signed control via Incremental FS.

## Revised regression controls

The new emulator A/B CI:

1. Pins the original APK SHA-256 and validates a no-patch re-sign.
2. Compares decompressed ZIP members using --strict-control.
3. Forces non-incremental ADB installation for both.
4. Measures install success, main-process liveness and real Taobao UI nodes.
5. Returns a failure if either baseline misses the Taobao UI, preserving
   the diagnostic artifacts without claiming an operational application.
6. Publishes only selected report/status files to public evidence branches,
   not raw logcat or screenshots.

The independent unit tests distinguish install failures, launcher screens,
process death, malformed hierarchy XML and native-bridge abort markers.

The new real-apk-matrix CI independently rebuilds current Morphe Patcher 2
source against the exact original source APK and compares original,
zero-patch STRIP_SAFE, and native DinamicX-patched STRIP_SAFE variants,
signed with a common disposable test certificate. It publishes hashes,
method-diff and native-library content comparisons, not APK files or keys.

## What remains unresolved

The meaningful causal login test needs a native ARM64 Android environment
where the official APK runs normally. Official versus re-signed-only must
be compared before blaming the DinamicX injection. Actual live English
localization requires a before/after comparison of an originally Chinese
Taobao UI widget against the patched build.

The original app signer is not preserved when installing a repackaged APK
normally. Root-mount alternatives are separate experiments; their feasibility
on the user's device is not established.

**Current result:** Emulator signal-handling failure is supported by its
actual tombstone. The device Account/login regression remains unexplained.
Real Taobao native English UI has not been confirmed.
