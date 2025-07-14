package com.hwj.translation.util

import com.hwj.translation.bean.TranslationParseSimpleInfo
import com.hwj.translation.interfaces.TranslationParser
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class AndroidXmlParser  : TranslationParser {
    override fun parse(xmlContent: String): List<TranslationParseSimpleInfo> {
        val doc = parseXml(xmlContent)
        val translationSimpleInfoList = mutableListOf<TranslationParseSimpleInfo>()
        val stringNodes = doc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            parseTranslationParseSimpleInfoNode(node)?.let { translationSimpleInfo ->
                translationSimpleInfoList.add(translationSimpleInfo)
            }
        }
        return translationSimpleInfoList
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
                    TranslationParseSimpleInfo(key, content)
                }
        }
    }
}