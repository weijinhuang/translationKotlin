package com.hwj.translation.util

class StringUtil {
}

fun handleSingleQuotes(addTranslationSB: StringBuilder, translationContent: String): String {
    val charArray = translationContent.toCharArray()
    addTranslationSB.clear()
    for (i in charArray.indices) {
        var c = charArray[i]
        if (c == '\'') {
            if (i == 0) {
                addTranslationSB.append('\\')
                addTranslationSB.append(c)
            } else {
                val preChar = charArray[i - 1]
                if (preChar == '\\') {
                    addTranslationSB.append(c)
                } else {
                    addTranslationSB.append('\\')
                    addTranslationSB.append(c)
                }
            }
        } else {
            addTranslationSB.append(c)
        }
    }
    return addTranslationSB.toString()
}