/*
 * TaobaoLingo — Japanese region English selector, server-model path.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.goodlight999.taobaolingo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch

private const val SITE_MODEL = "Lcom/taobao/android/editionswitcher/boarding/LocaleModel\$SiteLangModel;"
private const val LANG_MODEL = "Lcom/taobao/android/editionswitcher/boarding/LocaleModel\$LangModel;"

private val editionCompatibility = Compatibility(
    name = "Taobao",
    packageName = "com.taobao.taobao",
    apkFileType = ApkFileType.APK,
    targets = listOf(AppTarget(version = "10.66.30", isExperimental = true))
)

/**
 * Taobao parses the server-provided locale model in this callback and stores
 * it as LocaleModel.reqData. This fingerprint is version-pinned; index 87
 * is immediately after the reqData assignment in 10.66.30.
 */
private object EditionLocaleResponse : Fingerprint(
    definingClass = "Lcom/taobao/android/editionswitcher/api/EditionApi\$1;",
    name = "onSuccess",
    returnType = "V",
    parameters = listOf(
        "I",
        "Lmtopsdk/mtop/domain/MtopResponse;",
        "Lmtopsdk/mtop/domain/BaseOutDo;",
        "Ljava/lang/Object;"
    ),
    isStatic = false
)

/**
 * Only adds a non-default en_US option to the CURRENT JP locale list.
 * Does not force a language switch, select another region, or modify
 * network/edition language getters. Server acceptance of Japan+English is
 * not established and remains an on-device integration test.
 */
@Suppress("unused")
val japanEnglishServerPickerPatch = bytecodePatch(
    name = "TaobaoLingo: Enable English for Japan picker (experimental)",
    description = "Add English in Japan both to bundled locale options and the parsed server response.",
    default = false
) {
    compatibleWith(editionCompatibility)
    dependsOn(japanEnglishPickerPatch)
    execute {
        EditionLocaleResponse.method.addInstructions(
            87,
            """
                iget-object v9, v6, Lcom/taobao/android/editionswitcher/boarding/LocaleModel;->currentCountryInfo:$SITE_MODEL
                if-eqz v9, :taobaolingo_done
                iget-object v10, v9, $SITE_MODEL->site:Ljava/lang/String;
                const-string v11, "JP"
                invoke-virtual {v11, v10}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v10
                if-eqz v10, :taobaolingo_done
                iget-object v10, v9, $SITE_MODEL->langs:Ljava/util/List;
                if-eqz v10, :taobaolingo_done
                invoke-interface {v10}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                move-result-object v11
                :taobaolingo_scan
                invoke-interface {v11}, Ljava/util/Iterator;->hasNext()Z
                move-result v13
                if-eqz v13, :taobaolingo_add
                invoke-interface {v11}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v13
                check-cast v13, $LANG_MODEL
                iget-object v13, v13, $LANG_MODEL->lang:Ljava/lang/String;
                const-string v14, "en_US"
                invoke-virtual {v14, v13}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v13
                if-nez v13, :taobaolingo_done
                goto :taobaolingo_scan
                :taobaolingo_add
                new-instance v11, $LANG_MODEL
                invoke-direct {v11}, $LANG_MODEL-><init>()V
                const-string v13, "en_US"
                iput-object v13, v11, $LANG_MODEL->lang:Ljava/lang/String;
                const-string v13, "English"
                iput-object v13, v11, $LANG_MODEL->langName:Ljava/lang/String;
                const/4 v13, 0x0
                iput-boolean v13, v11, $LANG_MODEL->defaultLang:Z
                invoke-interface {v10, v11}, Ljava/util/List;->add(Ljava/lang/Object;)Z
                :taobaolingo_done
            """.trimIndent()
        )
    }
}
