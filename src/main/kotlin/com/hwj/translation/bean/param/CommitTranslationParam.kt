package com.hwj.translation.bean.param

import com.google.cloud.translate.Translation
import com.hwj.translation.bean.param.translation.TranslationSimpleInfo

class CommitTranslationParam {

    var translationKey: String? = null

    var moduleId: Int? = null

    var translationList: List<TranslationSimpleInfo>? = null

    var action: String = "update" // update更新， add 添加
}