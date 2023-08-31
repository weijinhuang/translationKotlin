package com.hwj.translation.bean

class Language {
    var languageId: String? = null

    var languageName: String? = null

    var projectId:String? = null

    override fun toString(): String {
        return "Language(languageId=$languageId, languageName=$languageName, projectId=$projectId)"
    }


}