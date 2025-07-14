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
/**
override fun addLanguage2(languageDes: String, languageName: String, projectId: String): Language? {
val sqlStr = "INSERT INTO TB_LANGUAGE(languageDes,languageName,projectId) VALUES(?,?,?)"
println("sqlStr -> $sqlStr")

val keyHolder = GeneratedKeyHolder()

val affectedRows = mJdbcTemplate.update({ connection ->
val ps = connection.prepareStatement(sqlStr, arrayOf("languageId"))
ps.setString(1, languageDes)
ps.setString(2, languageName)
ps.setString(3, projectId)
ps
}, keyHolder)

return if (affectedRows > 0) {
keyHolder.key?.let {
val languageId = it.toInt()
println("新增Language:$languageId")
Language().apply {
this.languageId = languageId?.toInt()
this.languageName = languageName
this.languageDes = languageDes
this.projectId = projectId
this.languageOrder = 0
}
} ?: null
} else {
return null
}
}
 */