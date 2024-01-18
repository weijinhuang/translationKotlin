package com.hwj.translation.bean.param

import java.net.URLEncoder

class GoogleTranslationParam {
    var content:String? = null
    var sourceLanguage:String? = null
    var targetLanguage:String? = null
    override fun toString(): String {
        return "GoogleTranslationParam(content=$content, sourceLanguage=$sourceLanguage, targetLanguage=$targetLanguage)"
    }


}