# Taobao APK three-variant binary triage

Content hashes refer to decompressed ZIP members; APK file hashes also include signature blocks and ZIP metadata.

| Variant | APK SHA-256 | ZIP members | Signature verified |
| --- | --- | ---: | --- |
| original | `3e709717a60fdeeb59354df60fb6cf279ffb023c01e095e16bccfbad75f683dd` | 5985 | PASS |
| resigned_only | `3735b315e10a5d84274d45429e533db2cad3ce776e5cdec5219b814fba57a4d0` | 5985 | PASS |

## original -> resigned_only

Changed: 0; added: 2; removed: 2

| Category | Entries |
| --- | ---: |
| v1_signature_metadata | 4 |

### Non-signature member differences

None.

## Interpretation limits

- Identical native library bytes exclude direct library mutation, not signature verification by native code.
- An unchanged DEX blob is stronger than matching method counts; a changed DEX still needs a method-level verifier.
- ZIP integrity and valid re-signing do not establish successful login, Account navigation, or live UI localization.
- No runtime behavior was tested by this script.
