package com.hwj.translation.bean.param

import com.hwj.translation.bean.Translation
import com.hwj.translation.bean.TranslationRow

class CommitTranslationRowParam(){
    var forceAdd: Boolean? = false
    val projectId: String? = null
    val moduleId: Int? = null
    val translationKey: String? = null
    val translations: List<Translation>? = null
}
