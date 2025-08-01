package com.hwj.translation.bean

data class SimpleTranslationRow(
    val key: String,
    val translations: List<TranslationParseSimpleInfo>
)
