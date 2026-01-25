package com.hwj.translation.bean.param

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class DeepSeekTranslationParam {
    var content:String? = null
    var sourceLanguage:String? = null
    var targetLanguageList:List<String>? = null
    var translateContext:String? = null
}
