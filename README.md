# TaobaoLingo

Experimental [Morphe](https://github.com/MorpheApp/morphe-patcher) patches for Taobao's English interface, developed for `com.taobao.taobao` **10.66.30** (arm64-v8a APK). Unaffiliated with Taobao, Alibaba, or Morphe.

**Status:** The Morphe 2.0 `.mpp` builds on GitHub Actions. Independently, a Desktop 1.18.1-compatible build of the same patch logic **successfully patched, rebuilt, aligned, and signed the supplied Taobao APK**. DEX instructions and APK signing have been verified. **Android installation, app startup, and visible English-language UI have *not* yet been tested.** This is still an experimental patch, not a confirmed working translation.

## Patches

| Patch | Default | Function |
| --- | --- | --- |
| TaobaoLingo: Force English UI | On | Make `Localization.h()` return Taobao's `Language.ENGLISH` and `LocalizationManager.d()` return `Locale.ENGLISH`. |
| TaobaoLingo: International UI flags | Off | Alter two international-edition checks, which can affect more than just UI. |

The default patch does **not** rewrite stored shopping country, shipping addresses, or account preferences. Other app logic might nevertheless rely on the overwritten locale; the true region behavior is unknown until a device test. WebViews, server-rendered material, product descriptions, and text embedded in images may remain Chinese. Japanese translation is not implemented.

## Download the Morphe patch

Download the `.mpp` build artifact from a successful [GitHub Actions build](../../actions/workflows/build.yml), then import it as a custom patch source in a **compatible** Morphe Manager. Select original Taobao **10.66.30** APK and initially enable **only Force English UI**.

**Morphe version caveat:** The main `.mpp` targets Morphe Patcher **2.0.0**. Morphe Desktop **1.18.1** uses Patcher **1.15.1** and *cannot load* the main `.mpp` (`NoSuchMethodError` on `Compatibility`). For offline proof, we compiled an **older-API CLI test bundle** from the same patch source. This compatibility bundle is **not** a Manager-compatible `.mpp` and does not contain `classes.dex`. See [full offline test report](analysis/offline_verification.md).

## Build from source

JDK 21 and Gradle 9.8.1 are required. GitHub Packages credentials with `read:packages` are required, even for public dependencies.

```sh
export GITHUB_ACTOR=your-username
export GITHUB_TOKEN=your-GitHub-PAT-with-read-packages
gradle buildAndroid
# patches/build/libs/*.mpp
```

The CI workflow builds the same artifact and offers it as a downloadable workflow artifact (no proprietary APK is included).

## Independent verification

The full evidence and SHA-256 hashes are documented in [`analysis/offline_verification.md`](analysis/offline_verification.md). Verified steps include matching the source APK, patch application without failed patches, DEX output checks for the two English-return instructions, unchanged original international flag methods, ZIP integrity, and Android APK signature schemes **v1/v2/v3**.

To verify the original Taobao APK locally:

```sh
python3 scripts/verify_apk.py /path/to/original-taobao-10.66.30.apk --strict-hash
```

The input APK and its extracted proprietary code are never committed to this repository.

To reproduce the legacy offline test bundle (requires Morphe Desktop 1.18.1 JAR and `kotlinc`):

```sh
python3 scripts/build_legacy_cli.py --desktop-jar /path/to/morphe-desktop-1.18.1-all.jar --out /tmp/taobaolingo-legacy.mpp
```

This test output is **for legacy Desktop CLI testing only**, and must not be imported in Morphe Manager.

## Last remaining checks: on-device

1. Install the patched APK. A re-signed APK generally cannot update the original app in place because the signing certificates differ. If you need to uninstall the original, retain any information necessary for login again.
2. Confirm app startup, login, and actual English rendering in settings, listings, product details, cart, and profile. Note any unchanged Chinese text or WebView pages.
3. Confirm shopping region, shipping destination, currency, and purchase flow remain as intended.
4. Record logs and screenshots (without credentials or personal data) in GitHub Issues if anything fails. Do not enable the optional international flags for the first run.

## License

GPL-3.0-only. The Taobao APK is proprietary and is **not** redistributed by this repository.
