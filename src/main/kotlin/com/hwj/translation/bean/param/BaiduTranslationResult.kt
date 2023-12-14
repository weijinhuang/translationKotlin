package com.hwj.translation.bean.param

class BaiduTranslationResult {
    var from:String? = null
    var to:String? = null
    var trans_result:List<BaiduTranslation>? = null
    var error_code:String? = null
    override fun toString(): String {
        return "BaiduTranslationResult(from=$from, to=$to, trans_result=$trans_result, error_code=$error_code)"
    }


}
