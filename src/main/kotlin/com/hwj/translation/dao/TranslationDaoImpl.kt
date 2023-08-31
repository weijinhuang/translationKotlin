package com.hwj.translation.dao

import com.hwj.translation.bean.Language
import com.hwj.translation.bean.Project
import com.hwj.translation.bean.Translation
import com.hwj.translation.print
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service("translationDaoImpl")
class TranslationDaoImpl : TranslationDao {

    @Autowired
    private lateinit var mJdbcTemplate: JdbcTemplate

    override fun getAllTranslationByProjectId(projectId: String): List<Translation> {
        val sqlStr = "SELECT * FROM tb_translation WHERE projectId='$projectId'"
        val translations = mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Translation::class.java))
        println("getAllTranslationByProjectId:size：${translations.size}")
        return translations
    }

    override fun getLanguageList(projectId: String): List<Language> {
        val sqlStr = "SELECT * FROM tb_language WHERE projectId='$projectId'"
        val languageList = mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Language::class.java))
        return languageList
    }


    override fun queryTranslationByKey(key: String, projectId: String, languageId: String): List<Translation> {
        val sqlStr = "SELECT * FROM tb_translation WHERE translationKey=? AND projectId=? AND languageId=?"
        val params = arrayOf<Any>(key, projectId, languageId)
        return mJdbcTemplate.query(sqlStr, params, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun queryProjectsByPackageName(projectId: String): List<Project?>? {
        val sqlStr = "SELECT * FROM TB_PROJECT WHERE projectId=?"
        val params = arrayOf<Any>(projectId)
        return mJdbcTemplate.query(sqlStr, params, BeanPropertyRowMapper(Project::class.java))
    }

    override fun addTranslation(translation: Translation): Boolean {
        return try {
            translation.projectId?.let { projectId ->
                translation.translationKey?.let { key ->
                    translation.languageId?.let { languageId ->
//                    val queryTranslationByKey = queryTranslationByKey(key, projectId, languageId)
//                    if (queryTranslationByKey.isNotEmpty()) {
//                        println("已有翻译，更新")
//                        updateTranslation(translation)
//                    } else {
//                    }
                        val sqlStr =
                            "INSERT INTO TB_TRANSLATION(translationKey,languageId,translationContent,projectId) VALUES(?,?,?,?)"
                        mJdbcTemplate.update(sqlStr, key, languageId, translation.translationContent, projectId) > 0
                    }

                }
            } ?: false
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }

    }

    override fun addProject(project: Project): Boolean {
        return try {
            val sqlStr = "INSERT INTO TB_PROJECT(projectId,projectName) VALUES(?,?)"
            mJdbcTemplate.update(sqlStr, project.projectId, project.projectName) > 0
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun queryLanguageByLanguageId(languageId: String, projectId: String): List<Language?>? {
        val sqlStr = "SELECT * FROM TB_LANGUAGE WHERE languageId=? AND projectId=?"
        val params = arrayOf<Any>(languageId, projectId)
        return mJdbcTemplate.query(sqlStr, params, BeanPropertyRowMapper(Language::class.java))
    }

    override fun getAllProject(): List<Project> {
        val sqlStr = "SELECT * FROM TB_PROJECT"
        val projects = mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Project::class.java))
        projects.print()
        return projects
    }


    override fun queryProjectByPackageName(projectId: String): Project? {
        val sqlStr = "SELECT * FROM TB_PROJECT WHERE package_name=?"
        val params = arrayOf<Any>(projectId)
        return mJdbcTemplate.queryForObject(sqlStr, params, BeanPropertyRowMapper(Project::class.java))
    }

    override fun updateTranslation(translation: Translation): Boolean {
        return translation.projectId?.let { projectId ->
            val sqlStr =
                "UPDATE TB_TRANSLATION SET translationContent=? WHERE translationKey=? AND projectId=? AND languageId=?"
//            val params = arrayOf(translation.translationContent, translation.translationKey, translation.projectId)
            mJdbcTemplate.update(
                sqlStr,
                translation.translationContent,
                translation.translationKey,
                translation.projectId,
                translation.languageId
            ) > 0
        } ?: false

    }

    override fun deleteTranslationByKey(translationKey: String, projectId: String): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE translationKey='$translationKey' AND projectId='$projectId'"
        return mJdbcTemplate.update(sqlStr) > 0
    }

    override fun addLanguage(languageId: String, languageName: String, projectId: String): Boolean {
        val sqlStr = "INSERT INTO TB_LANGUAGE(languageId,languageName,projectId) VALUES(?,?,?)"
        return mJdbcTemplate.update(sqlStr, languageId, languageName, projectId) > 0
    }

    override fun deleteTranslationByLanguageId(projectId: String, languageId: String): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE languageId='$languageId' AND projectId='$projectId'"
        return try {
            mJdbcTemplate.execute(sqlStr)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun deleteTranslationByTranslationKey(projectId: String, translationKey: String): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE translationKey='$translationKey' AND projectId='$projectId'"
        return try {
            mJdbcTemplate.execute(sqlStr)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }


    override fun deleteLanguage(language: Language): Boolean {
        val sqlStr =
            "DELETE FROM tb_language WHERE languageId='${language.languageId}' AND projectId='${language.projectId}'"
        return try {
            mJdbcTemplate.execute(sqlStr)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }
}