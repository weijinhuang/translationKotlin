package com.hwj.translation.util

import com.hwj.translation.interfaces.TranslationParser

class TranslationParserFactory {
}

fun getParserForFile(filename: String): TranslationParser {
    return when {
        filename.endsWith(".xml", ignoreCase = true) -> AndroidXmlParser()
        filename.endsWith(".strings", ignoreCase = true) -> IosStringsParser()
        else -> ExcelParser()
    }
}