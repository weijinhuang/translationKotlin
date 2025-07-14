package com.hwj.translation.interfaces

import com.hwj.translation.bean.TranslationParseSimpleInfo

interface TranslationParser {
    fun parse(content: String): List<TranslationParseSimpleInfo>
}