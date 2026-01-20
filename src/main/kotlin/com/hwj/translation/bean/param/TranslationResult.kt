package com.hwj.translation.bean.param

class TranslationResult {
    var sourceLanguage:String? = null
    var targetLanguage:String? = null
    var transResult:String? = null
    var translationResultList:List<Pair<String,String>>? = null
    var errorCode:Int = -1

}
