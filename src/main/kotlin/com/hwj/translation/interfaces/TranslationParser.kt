package com.hwj.translation.interfaces

import com.hwj.translation.bean.SimpleTranslationRow
import com.hwj.translation.bean.TranslationParseSimpleInfo
import com.hwj.translation.bean.TranslationRow
import java.io.InputStream

interface TranslationParser {
    fun parse(content: String,stream:InputStream): List<SimpleTranslationRow>

}