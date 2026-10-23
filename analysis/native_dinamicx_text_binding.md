# Native DinamicX text binding: targeted English UI patch (Taobao 10.66.30)

**Status: new experimental approach. Morphe build and offline DEX tests pass; no user-visible localization or app runtime success is yet proven.**

## Why the earlier patches missed the UI

The original APK has 595,518 implemented methods across 13 DEX files. Direct call-site analysis showed that `Localization.q(int)` (909 invocation instructions) is frequently used by imaging, sharing, permissions and other subsystems. It was **not established** as the text source for the My Taobao home UI.

Instead, `assets/mytaobao/mtb_2025.json` describes My Taobao content delivered by `mtop.taobao.reborn.mclaren.stream` and rendered using dynamically downloaded DinamicX templates. Replacing only Android resource-string lookups does not translate this data.

## Native rendering paths found in the original code

In `classes6.dex`, the original app contains two text widget classes:

| Actual DEX class | Identified by | Writes to text |
| --- | --- | --- |
| `Ltb/gi6;` | `DXButterTextWidgetNode` | `setText(CharSequence)` and `onSetStringAttribute(long,String)` |
| `Ltb/cx5;` | `DXFastTextWidgetNode` | `setText(CharSequence)` and `onSetStringAttribute(long,String)` |

Crucially, `onSetStringAttribute` does not call `setText`. It stores the string **directly** in the `text` field whenever the attribute ID matches **38178040921** (Dalvik `const-wide`), and the view renders that field later. Therefore the first two-method prototype would have bypassed many actual template-driven UI texts. The corrected patch changes **four methods** and checks the text attribute ID before doing any replacement. Font and unrelated string attributes are not intentionally altered.

## Actual patch behavior

`TaobaoLingo: Native DinamicX English labels (experimental)` defines **108 exact Chinese→English UI label mappings**, including `我的淘宝`→`My Taobao`, `我的订单`→`My Orders`, `搜索`→`Search`, and `设置`→`Settings`.

- It only transforms a label when the source text exactly equals one of the 108 keys.
- It changes the value passed to the native text-widget binding, **not** the underlying API response, account session, locale, shopping region or destination.
- It does not translate arbitrary product listings, seller prose, image text or unrecognized labels.
- It does not install a translation overlay or perform OCR.
- **No visible translated text has yet been confirmed on a phone.** The user confirmed the prior English error screens and Agree buttons were present in unmodified Taobao.

## Verification performed

A reproducible **eight-label proof build** implementing the same Dalvik injection logic was compiled against Morphe Desktop 1.18.1/Patcher 1.15.1 and applied to the user's original Taobao 10.66.30 APK with `STRIP_SAFE` (unsigned output):

```text
Applied: TaobaoLingo: Native DinamicX English labels (proof)
Writing 2 modified/new classes to new DEX files
Rebuilding DEX: classes6.dex (stripping 2 modified classes, keeping 8771)
```

An independent DEX analysis compared **all 595,518 implemented methods** and confirmed:

- Exactly four changed methods: the two `setText` methods and two `onSetStringAttribute` methods.
- Original instruction sequences preserved after injected prefixes, original register counts and try-block counts retained.
- Explicit text-attribute hash guard for both `onSetStringAttribute` methods.
- Actual Chinese and English text constants encoded in the modified methods.
- No changes to any other non-DEX APK resources.

Separately, GitHub Actions built the **full 108-label Morphe 2.0 `.mpp` successfully**. This is a compile result, **not** an 108-label APK installation/runtime test. The 108-label patch is opt-in and disabled by default.

Source: `patches/src/main/kotlin/dev/goodlight999/taobaolingo/DinamicXEnglishLabelsPatch.kt`. Independent verification helper: `tests/VerifyDinamicXNativeLabels.java`.

## Still not resolved

The modified APK's signing identity differs from Taobao's original signer. The earlier reported black screen, network/login issues and unresponsive `我的淘宝` are **not fixed or proven to be caused by any particular mechanism**. This text-binding-only patch may visibly translate some live DinamicX labels, but full English UI and functional login require real device validation. The next comparison must begin with an original-app Chinese label and confirm that the *same* label changes on the patched app, not assume that pre-existing English constitutes translation.
