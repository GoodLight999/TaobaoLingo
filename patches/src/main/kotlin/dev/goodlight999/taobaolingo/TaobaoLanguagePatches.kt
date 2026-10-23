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

private object ResourceLocaleUpdater : Fingerprint(
    definingClass = "Lcom/alibaba/ability/localization/LocalizationManager;",
    name = "i",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", LANGUAGE),
    isStatic = false
)

private object LocalizedStringLookup : Fingerprint(
    definingClass = LOCALIZATION,
    name = "q",
    returnType = "Ljava/lang/String;",
    parameters = listOf("I"),
    isStatic = true
)

private object NamedStringLookup : Fingerprint(
    definingClass = LOCALIZATION,
    name = "r",
    returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/lang/String;"),
    isStatic = true
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
    default = false
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

/**
 * Conservative UI-only localization experiment.
 * Only modify the Android Resources/Configuration updater's Language argument.
 * Do not override application language getters that feed networking / edition APIs.
 * LocalizationManager.a() invokes this method on each activity creation.
 */
@Suppress("unused")
val englishUiResourcesPatch = bytecodePatch(
    name = "TaobaoLingo: English UI resources only",
    description = "Set Android UI resources to English without changing language getters used by network and region logic (experimental).",
    default = false
) {
    compatibleWith(taobao)
    execute {
        ResourceLocaleUpdater.method.addInstructions(
            0,
            "sget-object p2, $LANGUAGE->ENGLISH:$LANGUAGE"
        )
    }
}

/**
 * Localized Resources.getString() for the 909 calls to Localization.q(int).
 * Creates a separate configuration context rather than modifying shared
 * Resources or reporting a false locale to the network, edition or login SDKs.
 *
 * Uses only three existing local registers in q(int) (5 registers total,
 * one parameter; v0-v3 are locals in Taobao 10.66.30).
 * Fails through to the original implementation if the app context is absent.
 * This does not translate remote JSON, WebView, or images.
 */
@Suppress("unused")
val englishResourceStringsPatch = bytecodePatch(
    name = "TaobaoLingo: English resource strings",
    description = "English ID-based and named string resources without modifying networking or regional locale (experimental).",
    default = false
) {
    compatibleWith(taobao)
    execute {
        LocalizedStringLookup.method.addInstructions(
            0,
            """
                sget-object v0, Lcom/alibaba/ability/localization/LocalizationManager;->INSTANCE:Lcom/alibaba/ability/localization/LocalizationManager;
                invoke-virtual {v0}, Lcom/alibaba/ability/localization/LocalizationManager;->b()Landroid/content/Context;
                move-result-object v0
                if-eqz v0, :taobaolingo_original
                invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
                move-result-object v1
                invoke-virtual {v1}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;
                move-result-object v1
                new-instance v2, Landroid/content/res/Configuration;
                invoke-direct {v2, v1}, Landroid/content/res/Configuration;-><init>(Landroid/content/res/Configuration;)V
                sget-object v1, Ljava/util/Locale;->ENGLISH:Ljava/util/Locale;
                invoke-virtual {v2, v1}, Landroid/content/res/Configuration;->setLocale(Ljava/util/Locale;)V
                invoke-virtual {v0, v2}, Landroid/content/Context;->createConfigurationContext(Landroid/content/res/Configuration;)Landroid/content/Context;
                move-result-object v0
                invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
                move-result-object v0
                invoke-virtual {v0, p0}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
                move-result-object v0
                return-object v0
                :taobaolingo_original
            """.trimIndent()
        )
        NamedStringLookup.method.addInstructions(
            0,
            """
                sget-object v0, Lcom/alibaba/ability/localization/LocalizationManager;->INSTANCE:Lcom/alibaba/ability/localization/LocalizationManager;
                invoke-virtual {v0}, Lcom/alibaba/ability/localization/LocalizationManager;->b()Landroid/content/Context;
                move-result-object v0
                if-eqz v0, :taobaolingo_original
                invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
                move-result-object v1
                invoke-virtual {v1}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;
                move-result-object v1
                new-instance v2, Landroid/content/res/Configuration;
                invoke-direct {v2, v1}, Landroid/content/res/Configuration;-><init>(Landroid/content/res/Configuration;)V
                sget-object v1, Ljava/util/Locale;->ENGLISH:Ljava/util/Locale;
                invoke-virtual {v2, v1}, Landroid/content/res/Configuration;->setLocale(Ljava/util/Locale;)V
                invoke-virtual {v0, v2}, Landroid/content/Context;->createConfigurationContext(Landroid/content/res/Configuration;)Landroid/content/Context;
                move-result-object v0
                invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
                move-result-object v1
                const-string v2, "string"
                invoke-virtual {v0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
                move-result-object v3
                invoke-virtual {v1, p0, v2, v3}, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
                move-result v2
                if-eqz v2, :taobaolingo_original
                invoke-virtual {v1, v2}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
                move-result-object v0
                return-object v0
                :taobaolingo_original
            """.trimIndent()
        )
    }
}

