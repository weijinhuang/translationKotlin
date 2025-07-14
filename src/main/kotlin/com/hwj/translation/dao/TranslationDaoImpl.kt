package com.hwj.translation.dao

import com.hwj.translation.bean.Language
import com.hwj.translation.bean.Module
import com.hwj.translation.bean.Project
import com.hwj.translation.bean.Translation
import com.hwj.translation.print
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
            mJdbcTemplate.update(sqlStr) {
                it.setString(1, project.projectId)
                it.setString(2, project.projectName ?: "")
            } > 0
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun deleteProject(project: Project): Boolean {
        return try {
            val deleteTranslationByProjectId = deleteTranslationByProjectId(projectId = project.projectId!!)
            val deleteLanguageByProjectId = deleteLanguageByProjectId(project.projectId!!)
            val deleteModule = deleteModule(null, project.projectId!!)
            println("deleteTranslationByProjectId:$deleteTranslationByProjectId,deleteLanguageByProjectId:$deleteLanguageByProjectId,deleteModule:$deleteModule,")
            val sqlStr = "DELETE FROM tb_project WHERE projectId=?"
            println("sqlStr -> $sqlStr")
            mJdbcTemplate.update(sqlStr) {
                it.setString(1, project.projectId)
            } > 0
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun queryProjectsByProjectId(projectId: String): List<Project?>? {
        val sqlStr = "SELECT * FROM TB_PROJECT WHERE projectId=?"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, PreparedStatementSetter { it.setString(1, projectId) }, BeanPropertyRowMapper(Project::class.java))
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
        val sqlStr = "SELECT * FROM tb_language WHERE projectId=?"
        println("sqlStr -> $sqlStr $projectId")
        val languageList = mJdbcTemplate.query(sqlStr, PreparedStatementSetter { it.setString(1, projectId) }, BeanPropertyRowMapper(Language::class.java))
        return languageList
    }

    override fun queryLanguageByLanguageName(languageName: String, projectId: String): List<Language?>? {
        val sqlStr = "SELECT * FROM TB_LANGUAGE WHERE languageName=? AND projectId=?"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
            it.setString(1, languageName)
            it.setString(2, projectId)
        }, BeanPropertyRowMapper(Language::class.java))
    }

    override fun addLanguage(languageDes: String, languageName: String, projectId: String): Boolean {
        val sqlStr = "INSERT INTO TB_LANGUAGE(languageDes,languageName,projectId) VALUES(?,?,?)"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.update(sqlStr) {
            it.setString(1, languageDes)
            it.setString(2, languageName)
            it.setString(3, projectId)
        } > 0
    }

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
            "DELETE FROM tb_language WHERE projectId=?"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.update(sqlStr) {
                it.setString(1, projectId)
            } > 0
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun updateLanguage2(languageId: Int, languageName: String, languageDes: String, languageOrder: Int): Boolean {
//        val sqlStr2 = "UPDATE TB_TRANSLATION SET translationContent=? ,translationKey=?  ,comment=? ,referto=?, hide=? WHERE translationId=?"
        val sqlStr = "UPDATE TB_LANGUAGE    SET languageName=?       ,languageDes=?     , languageOrder=?                      WHERE languageId=?"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.update(sqlStr) {
                it.setString(1, languageName)
                it.setString(2, languageDes)
                it.setInt(3, languageOrder)
                it.setInt(4, languageId)
            } > 0
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }
    /**-------Module---------*/


    /**-------Translation---------*/
    override fun queryTranslationByLanguage(languageId: Int, projectId: String): List<Translation> {
        val sqlStr = "SELECT * FROM tb_translation WHERE projectId=? AND languageId=? "
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
            it.setString(1, projectId)
            it.setInt(2, languageId)
        }, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun queryTranslationByLanguageWithHide(languageId: Int, projectId: String): List<Translation> {
        val sqlStr = "SELECT * FROM tb_translation WHERE projectId=? AND languageId=? "
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
            it.setString(1, projectId)
            it.setInt(2, languageId)
        }, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun getAllTranslationByProjectId(projectId: String): List<Translation> {
        val sqlStr = "SELECT * FROM tb_translation WHERE projectId=? ORDER BY translationId DESC"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, PreparedStatementSetter { it.setString(1, projectId) }, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun queryTranslationByModule(moduleId: Int, projectId: String): List<Translation> {
        val sqlStr = "SELECT * FROM tb_translation WHERE moduleId=? AND projectId=? ORDER BY translationId DESC"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
            it.setInt(1, moduleId)
            it.setString(2, projectId)
        }, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun queryTranslationByKey(key: String, projectId: String): List<Translation> {
        val sqlStr =
            "SELECT * FROM tb_translation WHERE translationKey=? AND projectId=?"
//        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
            it.setString(1, key)
            it.setString(2, projectId)

        }, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun queryTranslationByKeyInLanguage(
        key: String,
        projectId: String,
        languageId: Int
    ): List<Translation> {
        val sqlStr =
            "SELECT * FROM tb_translation WHERE translationKey=? AND projectId=? AND languageId=?"
//        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
            it.setString(1, key)
            it.setString(2, projectId)
            it.setInt(3, languageId)

        }, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun addTranslation(translations: List<Translation>): Boolean {
        val batchSize = 1000
        val startTime = System.currentTimeMillis()
        translations.chunked(batchSize).forEachIndexed { index, batch ->
            println("处理批次: ${index + 1}/${translations.size / batchSize}")
            batchAddTranslationsDeepSeek(batch)
        }
        val endTime = System.currentTimeMillis()
        val spendTime = endTime - startTime
        println("DeepSeek 批量插入完成，总共成功插入: ${translations.size} 条记录 花费时间：${spendTime}")
//        batchAddTranslationsDoubao(translations)
        return true
    }

    private fun batchAddTranslationsDoubao(translations: List<Translation>): Boolean {
        val startTime = System.currentTimeMillis()
        println("开始批量插入翻译数据，总数: ${translations.size} ")
        if (translations.isEmpty()) return true

        val batchSize = 1000 // 每批处理的记录数，可根据数据库和性能调整
        val batches = translations.chunked(batchSize)
        var totalSuccess = 0

        try {
            batches.forEachIndexed { batchIndex, batch ->
                val sql = "INSERT INTO TB_TRANSLATION(translationKey,languageId,translationContent,projectId,moduleId,comment,referto, hide) VALUES(?,?,?,?,?,?,?,?)"
                val batchArgs = batch.map { translation ->
                    arrayOf(
                        translation.translationKey?.trim(),
                        translation.languageId,
                        translation.translationContent?.trim(),
                        translation.projectId,
                        translation.moduleId ?: -1,
                        translation.comment ?: "",
                        translation.referto ?: "",
                        translation.hide ?: 0
                    )
                }

                val updateCounts = mJdbcTemplate.batchUpdate(sql, batchArgs)
                totalSuccess += updateCounts.sum()
                println("完成批次 ${batchIndex + 1}/${batches.size}，成功插入: ${updateCounts.sum()} 条")
            }

            val endTime = System.currentTimeMillis()
            val spendTime = endTime - startTime
            println("豆包批量插入完成，总共成功插入: $totalSuccess 条记录 花费时间：${spendTime}")
            return totalSuccess == translations.size
        } catch (e: Exception) {
            println("批量插入失败: ${e.message}")
            e.printStackTrace()
            return false
        }
    }


    private fun batchAddTranslationsDeepSeek(translations: List<Translation>): Boolean {
        val result = try {
            val sqlStr2 = "INSERT INTO TB_TRANSLATION(translationKey,languageId,translationContent,projectId,moduleId,comment,referto, hide) VALUES(?,?,?,?,?,?,?,?)"

            mJdbcTemplate.batchUpdate(sqlStr2, object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    val t = translations[i]
                    with(t) {
                        ps.setString(1, translationKey?.trim() ?: "")
                        ps.setInt(2, languageId ?: 0)
                        ps.setString(3, translationContent?.trim() ?: "")
                        ps.setString(4, projectId ?: "")
                        ps.setInt(5, moduleId ?: -1)
                        ps.setString(6, comment ?: "")
                        ps.setString(7, referto ?: "")
                        ps.setInt(8, hide ?: 0)
                    }
                }

                override fun getBatchSize() = translations.size
            })
            true
        } catch (e: Exception) {
            false
        }
        return result
    }

    override fun addTranslation(translation: Translation): Translation? {
        println("新增翻译：$translation")
        var result: Translation? = null
        try {
            translation.projectId?.let { projectId ->
                translation.translationKey?.let { key ->
                    translation.languageId?.let { languageId ->
                        val sqlStr2 = "INSERT INTO TB_TRANSLATION(translationKey,languageId,translationContent,projectId,moduleId,comment,referto, hide) VALUES(?,?,?,?,?,?,?,?)"
                        val keyHolder = GeneratedKeyHolder()
                        val affectedRows = mJdbcTemplate.update({ connection ->
                            val ps = connection.prepareStatement(sqlStr2, arrayOf("translationId"))
                            ps.setString(1, key.trim())
                            ps.setInt(2, languageId)
                            ps.setString(3, translation.translationContent?.trim())
                            ps.setString(4, projectId)
                            ps.setInt(5, translation.moduleId ?: -1)
                            ps.setString(6, translation.comment ?: "")
                            ps.setString(7, translation.referto ?: "")
                            ps.setInt(8, translation.hide ?: 0)
                            ps
                        }, keyHolder)
                        if (affectedRows > 0) {
                            keyHolder.key?.let {
                                translation.translationId = it.toInt()
                                println("添加翻译成功，id：${translation.translationId}")
                                result = translation
                            }
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return result
    }

    override fun updateTranslation(translation: Translation): Boolean {
        return translation.projectId?.let { projectId ->
            val sqlStr2 =
                "UPDATE TB_TRANSLATION SET translationContent=? ,translationKey=?  ,comment=? ,referto=?, hide=? WHERE translationId=?"
            mJdbcTemplate.update(
                sqlStr2
            ) {
                it.setString(1, translation.translationContent)
                it.setString(2, translation.translationKey)
                it.setString(3, translation.comment)
                it.setString(4, translation.referto ?: "")
                it.setInt(5, translation.hide ?: 0)
                it.setInt(6, translation.translationId ?: 0)
            } > 0
        } ?: false

    }

    override fun deleteTranslationByKey(translationKey: String, projectId: String): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE translationKey=? AND projectId=?"
        println("sqlStr -> $sqlStr")
        return mJdbcTemplate.update(sqlStr) {
            it.setString(1, translationKey)
            it.setString(2, projectId)
        } > 0
    }

    override fun deleteTranslationByLanguageId(projectId: String, languageId: Int): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE languageId=? AND projectId=?"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.update(sqlStr) {
                it.setInt(1, languageId)
                it.setString(2, projectId)
            } > 0
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun deleteTranslationByTranslationKey(projectId: String, translationKey: String): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE translationKey=? AND projectId=?"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.update(sqlStr) {
                it.setString(1, translationKey)
                it.setString(2, projectId)
            } > 0

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getTranslationCountOfLanguage(projectId: String, languageId: Int): Int {
        val sqlStr = "SELECT COUNT(*) FROM tb_translation WHERE projectId=? AND languageId=?"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.queryForObject(sqlStr, Integer::class.java, projectId, languageId) as Int

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            0
        }
    }

    override fun getTranslationCountOfProject(projectId: String): Int {
        val sqlStr = "SELECT COUNT(*) FROM tb_translation WHERE projectId=?"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.queryForObject(sqlStr, Integer::class.java, projectId) as Int
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            0
        }
    }

    override fun deleteTranslationByProjectId(projectId: String): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE projectId=?"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.update(sqlStr) {
                it.setString(1, projectId)
            } > 0

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun deleteTranslationByTranslationId(translationId: Int): Boolean {
        val sqlStr = "DELETE FROM tb_translation WHERE translationId=?"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.update(sqlStr) {
                it.setInt(1, translationId)
            } > 0

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun addModule(moduleName: String, projectId: String): Module? {
        val sqlStr =
            "INSERT INTO TB_FUNCTION_MODULE(moduleName,projectId) VALUES(?,?)"
        println("sqlStr -> $sqlStr")
        return try {
            val keyHolder = GeneratedKeyHolder()

            val affectedRows = mJdbcTemplate.update({ connection ->
                val ps = connection.prepareStatement(sqlStr, arrayOf("moduleId"))
                ps.setString(1, moduleName)
                ps.setString(2, projectId)
                ps
            }, keyHolder)

            if (affectedRows > 0) {
                keyHolder.key?.let {
                    println("新增Module : ${it.toLong()}")
                    Module().apply {
                        this.moduleId = it.toInt()
                        this.moduleName = moduleName
                        this.projectId = projectId
                    }
                }
            } else {
                null
            }

        } catch (e: java.lang.Exception) {
            null
        }
    }

    override fun deleteModule(moduleId: Int?, projectId: String): Boolean {
        if (null == moduleId) {
            val sqlStr = "DELETE FROM TB_FUNCTION_MODULE WHERE projectId=?"
            println("sqlStr -> $sqlStr")
            return try {
                mJdbcTemplate.update(sqlStr) {
                    it.setString(1, projectId)
                } > 0
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                false
            }
        } else {
            val sqlStr = "DELETE FROM TB_FUNCTION_MODULE WHERE moduleId=? AND projectId=?"
            println("sqlStr -> $sqlStr")
            return try {
                mJdbcTemplate.update(sqlStr) {
                    it.setInt(1, moduleId)
                    it.setString(2, projectId)
                } > 0
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                false
            }
        }

    }

    override fun getAllModules(projectId: String): List<Module> {
        val sqlStr = "SELECT * FROM TB_FUNCTION_MODULE WHERE projectId=?"
        println("sqlStr -> $sqlStr")

        val modules = mJdbcTemplate.query(sqlStr, PreparedStatementSetter { it.setString(1, projectId) }, BeanPropertyRowMapper(Module::class.java))

//        val modules = mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(Module::class.java))
        return modules
    }

    override fun queryModuleByName(moduleName: String, projectId: String): List<Module> {
        val sqlStr = "SELECT * FROM TB_FUNCTION_MODULE WHERE projectId=? AND moduleName=?"
        println("sqlStr -> $sqlStr")
        val modules = mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
            it.setString(1, projectId)
            it.setString(2, moduleName)
        }, BeanPropertyRowMapper(Module::class.java))
        return modules
    }

    override fun queryModuleById(projectId: String): List<Module> {
        val sqlStr = "SELECT * FROM TB_FUNCTION_MODULE WHERE projectId=?"
//        println("sqlStr -> $sqlStr")
        val modules = mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
            it.setString(1, projectId)
        }, BeanPropertyRowMapper(Module::class.java))
        return modules
    }
}