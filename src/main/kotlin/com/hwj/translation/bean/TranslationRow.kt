package com.hwj.translation.bean

data class TranslationRow(
    val key: String,
    val translations: Map<Int, Translation> // 语言ID到翻译的映射
)