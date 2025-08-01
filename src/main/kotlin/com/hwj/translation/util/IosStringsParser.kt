package com.hwj.translation.util

import com.hwj.translation.bean.SimpleTranslationRow
import com.hwj.translation.bean.TranslationParseSimpleInfo
import com.hwj.translation.bean.TranslationRow
import com.hwj.translation.interfaces.TranslationParser
import java.io.InputStream

class IosStringsParser : TranslationParser {

    // 正则匹配: "key" = "value";
//    private val pattern = "\"([^\"]+)\"\\s*=\\s*\"((?:\\\\\"|[^\"])*)\"\\s*;".toRegex()
    private val regex = "\"([^\"]+)\"\\s*=\\s*([^;]+);".toRegex()

    override fun parse(content: String, stream: InputStream): List<SimpleTranslationRow> {
        val list = regex.findAll(content).mapNotNull { matchResult ->
            if (matchResult.groupValues.size >= 3) {
                val key = matchResult.groupValues[1]
                val rawValue = matchResult.groupValues[2]
                val value = unescapeIosString(rawValue)
                TranslationParseSimpleInfo(key, value)
            } else null
        }.toList()
        return listOf(SimpleTranslationRow("", list))
    }

    private fun unescapeIosString(input: String): String {
        return input.replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
    }
}