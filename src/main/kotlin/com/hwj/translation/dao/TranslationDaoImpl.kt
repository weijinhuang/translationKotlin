package com.hwj.translation.dao

import com.hwj.translation.bean.Language
import com.hwj.translation.bean.Module
import com.hwj.translation.bean.Project
import com.hwj.translation.bean.Translation
import com.hwj.translation.print
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.PreparedStatement

@Service("translationDaoImpl")
class TranslationDaoImpl : TranslationDao {


    @Autowired
    private lateinit var mJdbcTemplate: JdbcTemplate


    /**-------Project---------*/


    override fun addProject(project: Project): Boolean {
        return try {
            val sqlStr = "INSERT INTO TB_PROJECT(projectId,projectName) VALUES(?,?)"
            println("sqlStr -> $sqlStr")
            mJdbcTemplate.update(sqlStr, project.projectId, project.projectName) > 0
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun deleteProject(project: Project): Boolean {
        return try {
            deleteTranslationByProjectId(projectId = project.projectId!!)
            deleteLanguageByProjectId(project.projectId!!)
            deleteModule(null, project.projectId!!)
            val sqlStr = "DELETE FROM tb_project WHERE projectId='${project.projectId}'"
            println("sqlStr -> $sqlStr")
            mJdbcTemplate.execute(sqlStr)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun queryProjectsByProjectId(projectId: String): List<Project?>? {
        val sqlStr = "SELECT * FROM TB_PROJECT WHERE projectId=?"
        println("sqlStr -> $sqlStr")
        val params = arrayOf<Any>(projectId)
        return mJdbcTemplate.query(sqlStr, params, BeanPropertyRowMapper(Project::class.java))
    }

    override fun getAllProject(): List<Project> {
        val sqlStr = "SELECT * FROM TB_PROJECT"
        println("sqlStr -> $sqlStr")
        val projects = mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Project::class.java))
        projects.print()
        return projects
    }

    /**-------Language---------*/
    override fun getLanguageList(projectId: String): List<Language> {
        val sqlStr = "SELECT * FROM tb_language WHERE projectId='$projectId'"
        println("sqlStr -> $sqlStr")
        val languageList = mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Language::class.java))
        return languageList
    }

    override fun queryLanguageByLanguageName(languageName: String, projectId: String): List<Language?>? {
        val sqlStr = "SELECT * FROM TB_LANGUAGE WHERE languageName='$languageName' AND projectId='$projectId'"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Language::class.java))
    }

    override fun addLanguage(languageDes: String, languageName: String, projectId: String): Boolean {
        val sqlStr = "INSERT INTO TB_LANGUAGE(languageDes,languageName,projectId) VALUES(?,?,?)"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.update(sqlStr, languageDes, languageName, projectId) > 0
    }

    override fun deleteLanguage(languageId: Int): Boolean {
        val sqlStr =
            "DELETE FROM tb_language WHERE languageId='$languageId'"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.execute(sqlStr)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun deleteLanguageByProjectId(projectId: String): Boolean {
        val sqlStr =
            "DELETE FROM tb_language WHERE projectId='$projectId'"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.execute(sqlStr)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    /**-------Module---------*/


    /**-------Translation---------*/
    override fun queryTranslationByLanguage(languageId: Int, projectId: String): List<Translation> {
        val sqlStr = "SELECT * FROM tb_translation WHERE projectId='$projectId' AND languageId='$languageId'"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun getAllTranslationByProjectId(projectId: String): List<Translation> {
        val sqlStr = "SELECT * FROM tb_translation WHERE projectId='$projectId'"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun queryTranslationByModule(moduleId: Int, projectId: String): List<Translation> {
        val sqlStr = "SELECT * FROM tb_translation WHERE moduleId='$moduleId' AND projectId='$projectId' "
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun queryTranslationByKeyInLanguage(
        key: String,
        projectId: String,
        languageId: Int
    ): List<Translation> {
        val sqlStr =
            "SELECT * FROM tb_translation WHERE translationKey='${key.trim()}' AND projectId='$projectId' AND languageId='$languageId'"
//        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun addTranslation(translation: Translation): Boolean {
        println("新增翻译：$translation")
        return try {
            translation.projectId?.let { projectId ->
                translation.translationKey?.let { key ->
                    translation.languageId?.let { languageId ->
                        val moduleId = translation.moduleId ?: -1

                        val sqlStr2 = "INSERT INTO TB_TRANSLATION(translationKey,languageId,translationContent,projectId,moduleId) VALUES(?,?,?,?,?)"
                        try {
                            mJdbcTemplate.update(
                                sqlStr2
                            ) {
                                it.setString(1, key.trim())
                                it.setInt(2, languageId)
                                it.setString(3, translation.translationContent?.trim())
                                it.setString(4, projectId)
                                it.setInt(5, moduleId)
                            } > 0
                        } catch (e: Exception) {
                            print("Key:$key -> ${translation.translationContent}")
                            e.printStackTrace()
                            false
                        }
                    }

                }
            } ?: false
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }

    }

    override fun updateTranslation(translation: Translation): Boolean {
        return translation.projectId?.let { projectId ->
//            val module = translation.moduleId ?: -1
//            val sqlStr =
//                "UPDATE TB_TRANSLATION SET translationContent=?,moduleId=? WHERE translationKey=? AND projectId=? AND languageId=?"
//            val params = arrayOf(translation.translationContent, translation.translationKey, translation.projectId)
//            println("sqlStr -> $sqlStr")

            val sqlStr2 =
                "INSERT INTO TB_TRANSLATION(translationKey,languageId,translationContent,projectId,moduleId) VALUES(?,?,?,?,?)"
//            mJdbcTemplate.update { con ->
//                if (null == mAddTranslationPrepareStatement) {
//                    mAddTranslationPrepareStatement = con.prepareStatement(sqlStr2)
//                }
//                mAddTranslationPrepareStatement?.let {
//                    it.setString(1, translation.translationKey)
//                    it.setInt(2, translation.languageId ?: 0)
//                    it.setString(3, translation.translationContent)
//                    it.setString(4, projectId)
//                    it.setInt(5, translation.moduleId ?: 0)
//                }
//                mAddTranslationPrepareStatement!!
//            } > 0
            mJdbcTemplate.update(
                sqlStr2
            ) {
                it.setString(1, translation.translationKey)
                it.setInt(2, translation.languageId ?: 0)
                it.setString(3, translation.translationContent?.trim())
                it.setString(4, projectId)
                it.setInt(5, translation.moduleId ?: 0)
            } > 0

//            mJdbcTemplate.update(
//                sqlStr,
//                translation.translationContent,
//                module,
//                translation.translationKey,
//                translation.projectId,
//                translation.languageId
//            ) > 0
        } ?: false

    }

    override fun deleteTranslationByKey(translationKey: String, projectId: String): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE translationKey='$translationKey' AND projectId='$projectId'"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.update(sqlStr) > 0
    }

    override fun deleteTranslationByLanguageId(projectId: String, languageId: Int): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE languageId='$languageId' AND projectId='$projectId'"
        println("sqlStr -> $sqlStr")
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
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.execute(sqlStr)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun deleteTranslationByProjectId(projectId: String): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE projectId='$projectId'"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.execute(sqlStr)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun addModule(moduleName: String, projectId: String): Boolean {
        val sqlStr =
            "INSERT INTO TB_FUNCTION_MODULE(moduleName,projectId) VALUES('$moduleName','$projectId')"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.update(sqlStr) > 0
        } catch (e: java.lang.Exception) {
            false
        }
    }

    override fun deleteModule(moduleId: Int?, projectId: String): Boolean {
        val sqlStr = if (null == moduleId) {
            "DELETE FROM TB_FUNCTION_MODULE WHERE projectId='$projectId'"
        } else {
            "DELETE FROM TB_FUNCTION_MODULE WHERE moduleId='$moduleId' AND projectId='$projectId'"
        }
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.execute(sqlStr)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getAllModules(projectId: String): List<Module> {
        val sqlStr = "SELECT * FROM TB_FUNCTION_MODULE WHERE projectId='$projectId'"
        println("sqlStr -> $sqlStr")
        val modules = mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Module::class.java))
        return modules
    }

    override fun queryModuleByName(moduleName: String, projectId: String): List<Module> {
        val sqlStr = "SELECT * FROM TB_FUNCTION_MODULE WHERE projectId='$projectId' AND moduleName='$moduleName'"
        println("sqlStr -> $sqlStr")
        val modules = mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Module::class.java))
        return modules
    }

    override fun queryModuleById(moduleId: Int, projectId: String): List<Module> {
        val sqlStr = "SELECT * FROM TB_FUNCTION_MODULE WHERE projectId='$projectId' AND moduleId='$moduleId'"
//        println("sqlStr -> $sqlStr")
        val modules = mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Module::class.java))
        return modules
    }
}