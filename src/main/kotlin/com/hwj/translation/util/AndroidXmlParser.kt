package com.hwj.translation.util

import com.hwj.translation.bean.ParseTranslationResult
import com.hwj.translation.bean.SimpleTranslationRow
import com.hwj.translation.bean.TranslationParseSimpleInfo
import com.hwj.translation.interfaces.TranslationParser
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.InputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class AndroidXmlParser  : TranslationParser {
    override fun parse(content: String, stream: InputStream): ParseTranslationResult {
        val doc = parseXml(content)
        val translationSimpleInfoList = mutableListOf<TranslationParseSimpleInfo>()
        val translationRowList = mutableListOf<SimpleTranslationRow>()
        val stringNodes = doc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            parseTranslationParseSimpleInfoNode(node)?.let { translationSimpleInfo ->
                translationSimpleInfoList.add(translationSimpleInfo)
                translationRowList.add(SimpleTranslationRow(translationSimpleInfo.translationKey, listOf(translationSimpleInfo)))
            }
        }
        return ParseTranslationResult().apply {
            languageTitleList = listOf("")
            this.translationRowList = translationRowList
        }
    }

    private fun parseXml(xmlContent: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val builder = factory.newDocumentBuilder()
        return builder.parse(InputSource(StringReader(xmlContent)))
    }

    private fun parseTranslationParseSimpleInfoNode(node: Node): TranslationParseSimpleInfo? {
        return node.attributes.getNamedItem("name")?.nodeValue?.let { key ->
            node.textContent.trim().takeIf { it.isNotBlank() }?.let { content ->
                    TranslationParseSimpleInfo(key, content,"")
                }
        }
    }
}