package com.hwj.translation.bean

class Translation() {

    var translationId: Int? = null

    var translationKey: String? = null

    var languageId: Int? = null

    var translationContent: String? = null

    var projectId: String? = null

    var moduleId: Int? = null

    var forceAdd:Boolean = false

    override fun toString(): String {
        return "Translation(translationId=$translationId, translationKey=$translationKey, languageId=$languageId, translationContent=$translationContent, projectId=$projectId,forceAdd=$forceAdd)"
    }


}