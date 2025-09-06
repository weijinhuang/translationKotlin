package com.hwj.translation.bean.param

class SearchTranslationByContentParam(
) {

    val projectId: String = ""                     // 项目ID
    val targetTranslationContent: String = ""     // 要搜索的翻译内容或translationKey
    val languageId: Int? = null     // 指定搜索的语言ID，null或负值时当作translationKey搜索
}