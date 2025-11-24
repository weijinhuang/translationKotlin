package com.hwj.translation.util

import com.hwj.translation.bean.ParseTranslationResult
import com.hwj.translation.bean.SimpleTranslationRow
import com.hwj.translation.bean.TranslationParseSimpleInfo
import com.hwj.translation.bean.TranslationRow
import com.hwj.translation.interfaces.TranslationParser
import java.io.InputStream

class IosStringsParser : TranslationParser {

    // 正则匹配: "key" = "value";
//    private val pattern = "\"([^\"]+)\"\\s*=\\s*\"((?:\\\\\"|[^\"])*)\"\\s*;".toRegex()
    private val regex  = "\"([^\"]+)\"\\s*=\"\\s*([^;]+)\";".toRegex()

    private val regex2 = """"([^"]+)"\s*=\s*"([^"]+)";""".toRegex()
    override fun parse(content: String, stream: InputStream): ParseTranslationResult {
        val translationRowList = mutableListOf<SimpleTranslationRow>()
        val lines = content.lines()

        lines.forEach { line  ->
            val matchResult = regex.find(line.trim())
            if (matchResult != null) {
                val (key, value) = matchResult.destructured
                val valueUnescape = unescapeIosString(value)
                val translationParseSimpleInfo = TranslationParseSimpleInfo(key, valueUnescape,"")
                translationRowList.add(SimpleTranslationRow(key, listOf(translationParseSimpleInfo)))
            }
        }
        return ParseTranslationResult().apply {
            languageTitleList = listOf("")
            this.translationRowList =translationRowList
        }
    }

    private fun unescapeIosString(input: String): String {
        return input.replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
    }
}