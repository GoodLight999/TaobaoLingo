# TaobaoLingo

Experimental Morphe patches for Taobao localization. This project is independent of Taobao and Morphe.

**Status: prototype; Android runtime not yet tested.** Developed against `com.taobao.taobao` 10.66.30 (arm64-v8a). Build success and DEX signature matching **do not establish** that the patched app works.

## Available patches

| Patch | Default | Purpose |
| --- | --- | --- |
| Force English UI | On | Override Taobao's bundled language getter and Locale getter to English. |
| International UI flags | Off | Switch on two international-edition checks, which may affect behavior beyond language. |

No persisted account, country, shipping, or payment settings are directly modified. App consumers of the overridden locale might nevertheless alter non-UI behavior. Server-rendered and image text is not translated. **Japanese translation remains a future feature**.

## Build

Requires JDK 21 and Gradle 9.8.1. See the [Build workflow](.github/workflows/build.yml) for the CI command and its downloadable `.mpp` artifact after a successful run.

```bash
# Github Packages authentication is required for Morphe dependencies.
export GITHUB_ACTOR=your-user
export GITHUB_TOKEN=your-read-packages-token
gradle buildAndroid
```

When ready, import the generated `patches/build/libs/*.mpp` as a local source in Morphe Manager.

## APK verification

```bash
python3 scripts/verify_apk.py /path/to/taobao-10.66.30.apk --strict-hash
```

The original APK is **not** distributed here. The script verifies the SHA-256 and DEX definitions against the originally studied APK. Matching the APK is not proof of successful patch application.

## Device test

1. In Morphe, patch an original `10.66.30` APK, enabling **only Force English UI**.
2. Confirm successful patching and launch; check settings, product listing/detail, cart, and account UI.
3. Confirm that shipping location and currency are still correct.
4. If needed, retry with International UI flags and report differences.

Modifying an APK changes its signature; replacing an original installation in place usually fails. Log out or back up your session as needed.

## Contributing

Please attach Morphe patch logs and a description of untranslated views in an issue. Never include account credentials.

GPL-3.0-only. Unaffiliated with Alibaba, Taobao, or Morphe.
