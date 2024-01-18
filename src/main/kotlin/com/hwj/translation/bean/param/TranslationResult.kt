package com.hwj.translation.bean.param

class TranslationResult {
    var sourceLanguage:String? = null
    var targetLanguage:String? = null
    var transResult:String? = null
    var errorCode:Int = -1
    override fun toString(): String {
        return "BaiduTranslationResult(from=$sourceLanguage, to=$targetLanguage, transResult=$transResult, errorCode=$errorCode)"
    }


}
