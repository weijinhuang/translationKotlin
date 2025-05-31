package com.hwj.translation.bean.param

data class MergeTranslationParam(
    val projectId:String?,

    val mainTranslationKey: String?,

    val translationToBeHideKeyList: List<String>?

)