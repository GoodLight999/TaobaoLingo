# TaobaoLingo

[Morphe](https://github.com/MorpheApp/morphe-patcher) patch for Taobao's existing **English** Android string resources. Developed against **com.taobao.taobao 10.66.30** (ARM64). Independent of Taobao and Morphe.

> **Experimental — app runtime not yet verified.** The earlier global Force English UI patch produced English navigation but a black home, non-responsive Account and login/network errors on a physical device. That broken patch and the international-edition flag overrides have been **removed from the published patch source**. Their code remains in Git history only.

## Patch in this repository

**TaobaoLingo: English resource strings** (disabled by default).

The APK contains two string-localization entrypoints:
- `Localization.q(int)` — referenced by **909** invoke instructions in the original APK.
- `Localization.r(String)` — referenced by **7** invoke instructions.

Both are patched to read Android's bundled English resources from a **separate configuration context**. The patch does not change Taobao's global `Localization.h()` language value, `LocalizationManager.d()` locale getter, international-edition checks, user account settings or shopping-region preferences. No language is sent to an external translation service.

**Scope:** native resources with an available English translation. Dynamic pages, server-rendered text, WebViews, product names, product images, and strings not included in the English resource table are **not** translated. Japanese UI is **not yet implemented**.

## How to build

The primary Gradle build produces a Morphe Patcher **2.0** `.mpp`.

```sh
# JDK 21, Gradle 9.8.1, and GitHub Packages read authentication
export GITHUB_ACTOR=your-user
export GITHUB_TOKEN=your-GitHub-token-with-read-packages
gradle buildAndroid
```

Download the latest `.mpp` from [GitHub Actions](../../actions/workflows/build.yml). In a Morphe Manager compatible with Patcher 2.0, import the patch source and enable **only** `TaobaoLingo: English resource strings` for Taobao **10.66.30**.

Morphe Desktop **1.18.1** bundles an older Patcher (1.15.1), which cannot directly load the main Morphe 2.0 `.mpp`. For independent offline verification, build the version-adapted CLI bundle with `scripts/build_legacy_cli.py`. This old Desktop bundle is **not** suitable for import in Morphe Manager.

## Offline verification (10.66.30)

**Independent binary checks completed on the original 77,255,755-byte APK.**

- Original source SHA-256: `3e709717a60fdeeb59354df60fb6cf279ffb023c01e095e16bccfbad75f683dd`.
- Morphe CLI with `STRIP_SAFE`: patch application, APK reconstruction and signing successful, **0 failed patches**.
- Compared **595,518** implemented methods: **exactly 2** have altered instructions (`Localization.q` and `Localization.r`).
- The original instruction sequences are preserved after the inserted prefixes, along with the original method register counts and exception-handler counts.
- Original networking, account/edition language getters and all non-DEX, non-signature APK contents are unchanged.
- APK ZIP integrity and Android v2/v3 signatures verified. **The signing certificate is different from Taobao's original.**

Details: [latest binary verification](analysis/two_string_resource_validation.md), [runtime regression tracking](../../issues/1), and independent offline verifier `tests/VerifyLocalizedResources.java`.

**Important limitation:** APK signature validity proves only the repackaged file is structurally sound. It does not prove that Taobao will accept a different signing certificate, log in, communicate with its servers, or render usable pages. **The login failure is not resolved yet.**

Morphe's official [Root mount](https://github.com/MorpheApp/morphe-manager/blob/main/docs/installers.md) avoids installing a re-signed APK, keeping the original app installed under its original package identity, but requires a rooted phone; ordinary installation does not have this property. No assumption is made that your phone is rooted.

## Offline reproduction

```sh
python3 scripts/verify_apk.py original-Taobao-10.66.30.apk --strict-hash
python3 scripts/build_legacy_cli.py \
  --desktop-jar morphe-desktop-1.18.1-all.jar \
  --out /tmp/taobaolingo-legacy.mpp

java -jar morphe-desktop-1.18.1-all.jar patch original-Taobao-10.66.30.apk \
  --bytecode-mode=STRIP_SAFE --exclusive \
  -p=/tmp/taobaolingo-legacy.mpp \
  '-e=TaobaoLingo: English resource strings' \
  -o=/tmp/taobaolingo.apk

javac -cp morphe-desktop-1.18.1-all.jar tests/VerifyLocalizedResources.java
java -cp tests:morphe-desktop-1.18.1-all.jar \
  VerifyLocalizedResources original-Taobao-10.66.30.apk /tmp/taobaolingo.apk
```

The proprietary Taobao APK is **never checked into this GitHub repository**. The `overlay/` work from earlier experiments is not part of the Morphe patch or its development plan.

## License

GPL-3.0-only for TaobaoLingo's original patch code. Unaffiliated with Alibaba, Taobao, and Morphe.
