package com.hwj.translation.bean

class Language {
    var languageId: Int? = null

    var languageName: String? = null

    var languageDes: String? = null

    var projectId:String? = null

    var languageOrder:Int? = 0

    override fun toString(): String {
        return "Language(languageId=$languageId, languageName=$languageName, languageDes=$languageDes, projectId=$projectId, languageOrder=$languageOrder)"
    }


}