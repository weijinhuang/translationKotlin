package com.hwj.translation.bean

class Language {

    var languageId: Int? = null

    var languageName: String? = null

    var languageDes: String? = null

    var languageDesPinYin: String? = null

    var projectId: String? = null

    var languageOrder: Int? = null

    constructor()

    constructor(languageName: String?, languageDes: String?, languageDesPinYin: String? = null) {
        this.languageName = languageName
        this.languageDes = languageDes
        this.languageDesPinYin = languageDesPinYin
    }

    constructor(languageName: String?, languageDes: String?, projectId: String?, languageOrder: Int? = null) {
        this.languageName = languageName
        this.languageDes = languageDes
        this.projectId = projectId
        this.languageOrder = languageOrder
    }

    constructor(languageName: String?, languageDes: String?, languagePinYin: String?, projectId: String?, languageOrder: Int? = null) {
        this.languageName = languageName
        this.languageDes = languageDes
        this.projectId = projectId
        this.languageOrder = languageOrder
        this.languageDesPinYin = languagePinYin
    }

    override fun toString(): String {
        return "Language(languageId=$languageId, languageName=$languageName, languageDes=$languageDes, languageDesPinYin=$languageDesPinYin, projectId=$projectId, languageOrder=$languageOrder)"
    }

}