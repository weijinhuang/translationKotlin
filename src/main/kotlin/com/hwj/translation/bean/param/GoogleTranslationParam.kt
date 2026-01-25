package com.hwj.translation.bean.param

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.net.URLEncoder
@JsonIgnoreProperties(ignoreUnknown = true)

class GoogleTranslationParam {
    var content:String? = null
    var sourceLanguage:String? = null
    var targetLanguage:String? = null
    var targetLanguageList:List<String>? = null
    override fun toString(): String {
        return "GoogleTranslationParam(content=$content, sourceLanguage=$sourceLanguage, targetLanguage=$targetLanguage)"
    }


}