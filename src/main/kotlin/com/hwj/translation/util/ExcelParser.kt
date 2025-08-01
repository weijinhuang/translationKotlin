package com.hwj.translation.util

import com.hwj.translation.bean.SimpleTranslationRow
import com.hwj.translation.bean.TranslationParseSimpleInfo
import com.hwj.translation.bean.TranslationRow
import com.hwj.translation.interfaces.TranslationParser
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

class ExcelParser : TranslationParser {
    override fun parse(excelContentStr: String, inputStream: InputStream): List<SimpleTranslationRow> {
        val workbook: Workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0)
        val rows = mutableListOf<SimpleTranslationRow>()

        // 获取语言列表（第一行，跳过第一个单元格）
        val headerRow = sheet.getRow(0)
        val languages = (1 until headerRow.lastCellNum.toInt())
            .map { headerRow.getCell(it)?.toString()?.trim() ?: "" }
            .filter { it.isNotBlank() }

        // 从第二行开始处理数据（索引1）
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            val keyCell = row.getCell(0) ?: continue
            val key = keyCell.toString().trim()

            val translations = mutableListOf<TranslationParseSimpleInfo>()

            // 从第二列开始处理翻译内容
            for ((langIndex, language) in languages.withIndex()) {
                val cell = row.getCell(langIndex + 1) // +1 跳过key列
                val content = cell?.toString()?.trim() ?: ""

                TranslationParseSimpleInfo(
                    languageName = language,
                    translationKey = key,
                    translationContent = content
                ).also { translations.add(it) }
            }

            if (key.isNotBlank()) {
                rows.add(SimpleTranslationRow(key, translations))
            }
        }

        workbook.close()
        return rows
    }
}