package com.hwj.translation.bean.param

import com.hwj.translation.bean.Translation

class CommitTranslationRowParam(){
    var action: String? = "ADD" //ADD UPDATE
    val projectId: String? = null
    val moduleId: Int? = null
    val newTranslationKey:String? = null //修改key之后，旧key
    val translationKey: String? = null
    val translations: List<Translation>? = null
}
