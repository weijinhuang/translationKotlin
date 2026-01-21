package com.hwj.translation.bean.param

import com.hwj.translation.bean.TranslatedResultKV

class TranslationResult {
    var sourceLanguage:String? = null
    var targetLanguage:String? = null
    var transResult:String? = null
    var translationResultList:List<TranslatedResultKV>? = null
    var errorCode:Int = -1

}
