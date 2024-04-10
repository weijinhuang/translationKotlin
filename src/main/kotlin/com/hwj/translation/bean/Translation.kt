package com.hwj.translation.bean

class Translation() {

    var translationId: Int? = null

    var translationKey: String? = null

    var languageId: Int? = null

    var translationContent: String? = null

    var oldTranslationContent: String? = null

    var projectId: String? = null

    var moduleId: Int? = null

    var forceAdd: Boolean = false

    var comment: String? = null

    var referto: String? = null

    var hide: Int = 0

    override fun toString(): String {
        return "Translation(translationId=$translationId, translationKey=$translationKey, languageId=$languageId, translationContent=$translationContent, oldTranslationContent=$oldTranslationContent, projectId=$projectId, moduleId=$moduleId, forceAdd=$forceAdd, comment=$comment)"
    }


}