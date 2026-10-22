/*
 * TaobaoLingo — experimental Taobao localization patches for Morphe.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.goodlight999.taobaolingo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch

private const val LANGUAGE = "Lcom/alibaba/ability/localization/constants/Language;"
private const val LOCALIZATION = "Lcom/alibaba/ability/localization/Localization;"

private val taobao = Compatibility(
    name = "Taobao",
    packageName = "com.taobao.taobao",
    apkFileType = ApkFileType.APK,
    targets = listOf(AppTarget(version = "10.66.30", isExperimental = true))
)

private object LanguageGetter : Fingerprint(
    definingClass = LOCALIZATION,
    name = "h",
    returnType = LANGUAGE,
    parameters = emptyList(),
    isStatic = true
)

private object LocaleGetter : Fingerprint(
    definingClass = "Lcom/alibaba/ability/localization/LocalizationManager;",
    name = "d",
    returnType = "Ljava/util/Locale;",
    parameters = emptyList(),
    isStatic = false
)

private object InternationalFlag : Fingerprint(
    definingClass = LOCALIZATION,
    name = "o",
    returnType = "Z",
    parameters = emptyList(),
    isStatic = true
)

private object SceneFlag : Fingerprint(
    definingClass = "Lcom/taobao/analysis/scene/SceneIdentifier;",
    name = "isI18nEdition",
    returnType = "I", // Not Z in 10.66.30.
    parameters = emptyList(),
    isStatic = true
)

/**
 * Uses Taobao's own English language enum and Java Locale.ENGLISH.
 * The shopping / shipping region is NOT edited in persistent settings.
 * This does not translate remote WebView / server-rendered content.
 */
@Suppress("unused")
val forceEnglishUiPatch = bytecodePatch(
    name = "TaobaoLingo: Force English UI",
    description = "Use bundled English locale while retaining configured shopping region (experimental).",
    default = true
) {
    compatibleWith(taobao)
    execute {
        LanguageGetter.method.addInstructions(
            0,
            """
                sget-object v0, $LANGUAGE->ENGLISH:$LANGUAGE
                return-object v0
            """.trimIndent()
        )
        LocaleGetter.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/util/Locale;->ENGLISH:Ljava/util/Locale;
                return-object v0
            """.trimIndent()
        )
    }
}

/**
 * Optional experiment: switches on two edition tests in addition to language override.
 * This may affect non-language behaviour. Never enabled by default.
 */
@Suppress("unused")
val enableInternationalUiPatch = bytecodePatch(
    name = "TaobaoLingo: International UI flags",
    description = "Optionally set international-edition tests; may change more than UI (experimental).",
    default = false
) {
    compatibleWith(taobao)
    execute {
        InternationalFlag.method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
        SceneFlag.method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
    }
}
