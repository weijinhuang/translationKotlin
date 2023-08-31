package com.hwj.translation.bean

class Translation() {

    var translationId: Int? = null

    var translationKey: String? = null

    var languageId: String? = null

    var translationContent: String? = null

    var projectId: String? = null

    override fun toString(): String {
        return "Translation(translationId=$translationId, translationKey=$translationKey, languageId=$languageId, translationContent=$translationContent, projectId=$projectId)"
    }


}