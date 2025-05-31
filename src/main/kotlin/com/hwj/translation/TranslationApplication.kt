package com.hwj.translation

import com.hwj.translation.bean.GET_LANGUAGE_LIST
import com.hwj.translation.bean.param.QueryLanguageListParam
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TranslationApplication

fun main(args: Array<String>) {
	runApplication<TranslationApplication>(*args)
}
