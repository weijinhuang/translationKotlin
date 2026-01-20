package com.hwj.translation.dao

import com.hwj.translation.bean.*
import com.hwj.translation.print
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.support.GeneratedKeyHolder
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
        println(" SELECT * FROM tb_translation WHERE translationKey=$key AND projectId=$projectId")
        return mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
            it.setString(1, key)
            it.setString(2, projectId)

        }, BeanPropertyRowMapper(Translation::class.java))
    }

    override fun batchImportTranslation(translations: List<Translation>, defaultModuleId: Int): Boolean {
        val SQL_UPSERT =
            "INSERT INTO tb_translation (translationKey, languageId, translationContent,projectId, moduleId, comment, hide, referto) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE translationContent = ?,comment = ?,hide = ?, referto = ?"
        var batchSize = 1000 // 每批处理量
        if (batchSize > translations.size) {
            batchSize = translations.size
        }
        val startTime = System.currentTimeMillis()
        println("批量插入开始 projectId:${translations.first().projectId}")
        translations.chunked(batchSize).forEach { chunk ->
            val batchUpdateResult = mJdbcTemplate.batchUpdate(SQL_UPSERT, object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    val t = chunk[i]
                    t.translationKey?.let { translationKey ->
                        t.languageId?.let { languageId ->
                            t.projectId?.let { projectId ->
                                ps.setString(1, translationKey)
                                ps.setInt(2, languageId)
                                ps.setString(3, t.translationContent)
                                ps.setString(4, t.projectId)
                                ps.setInt(5, t.moduleId ?: defaultModuleId)
                                ps.setString(6, t.comment)
                                ps.setInt(7, t.hide)
                                ps.setString(8, t.referto)
                                // 更新字段 (VALUES()获取传入值)
                                ps.setString(9, t.translationContent)
                                ps.setString(10, t.comment)
                                ps.setInt(11, t.hide)
                                ps.setString(12, t.referto)
                            }
                        }
                    }

                }

                override fun getBatchSize() = chunk.size
            })
            var successLine = 0
            var failedLine = 0
            batchUpdateResult.forEach {
                if (it > 0) {
                    successLine++
                } else {
                    failedLine++
                }
            }

            println("批量插入执行行数：${batchUpdateResult.size}, 成功：${successLine} 失败：${failedLine}")
        }

        val endTime = System.currentTimeMillis()
        println("batchImportTranslation结束，花费时间：${endTime - startTime}")
        return true
    }

    private fun batchAddTranslationsDeepSeek(translations: List<Translation>, defaultModuleId: Int): Boolean {
        val result = try {
            val sqlStr2 = "INSERT INTO TB_TRANSLATION(translationKey,languageId,translationContent,projectId,moduleId,comment,referto, hide) VALUES(?,?,?,?,?,?,?,?)"

            mJdbcTemplate.batchUpdate(sqlStr2, object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    val t = translations[i]

                    t.translationKey?.let { translationKey ->
                        t.languageId?.let { languageId ->
                            t.projectId?.let { projectId ->
                                ps.setString(1, translationKey.trim())
                                ps.setInt(2, languageId)
                                ps.setString(3, t.translationContent?.trim() ?: "")
                                ps.setString(4, projectId)
                                ps.setInt(5, t.moduleId ?: defaultModuleId)
                                ps.setString(6, t.comment ?: "")
                                ps.setString(7, t.referto ?: "")
                                ps.setInt(8, t.hide ?: 0)
                            }
                            Unit
                        }
                        Unit
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

    override fun addTranslation(translations: List<Translation>, defaultModuleId: Int): Boolean {
        val batchSize = 1000
        val startTime = System.currentTimeMillis()
        translations.chunked(batchSize).forEachIndexed { index, batch ->
            println("处理批次: ${index + 1}/${translations.size / batchSize}")
            batchAddTranslationsDeepSeek(batch, defaultModuleId)
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

    override fun getTranslationCountOfKeyInProject(projectId: String, translationKey: String): Int {
        val sqlStr = "SELECT COUNT(*) FROM tb_translation WHERE projectId=? AND translationKey=?"
        println("sqlStr -> $sqlStr")
        return try {
            mJdbcTemplate.queryForObject(sqlStr, Integer::class.java, projectId, translationKey) as Int

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

    /**-------Translation Pagination---------*/
    override fun getTranslationRowsPaginated(projectId: String, offset: Int, limit: Int): List<TranslationRow> {
        val sqlStr = """
            SELECT DISTINCT t.translationKey
            FROM tb_translation t
            WHERE t.projectId = ?
            ORDER BY t.translationKey
            LIMIT ? OFFSET ?
        """.trimIndent()

        println("sqlStr -> $sqlStr")

        // 首先获取分页的translationKey列表
        val translationKeys = mJdbcTemplate.queryForList(
            sqlStr,
            String::class.java,
            projectId,
            limit,
            offset
        )

        if (translationKeys.isEmpty()) {
            return emptyList()
        }

        // 然后获取这些key对应的所有translation记录
        val placeholders = translationKeys.joinToString(",") { "?" }
        val detailSql = """
            SELECT * FROM tb_translation 
            WHERE projectId = ? AND translationKey IN ($placeholders)
            ORDER BY translationKey, languageId
        """.trimIndent()

        val params = mutableListOf<Any>().apply {
            add(projectId)
            addAll(translationKeys)
        }

        val allTranslations = mJdbcTemplate.query(
            detailSql,
            PreparedStatementSetter { ps ->
                params.forEachIndexed { index, param ->
                    ps.setObject(index + 1, param)
                }
            },
            BeanPropertyRowMapper(Translation::class.java)
        )

        // 按translationKey分组
        val translationMap = allTranslations.groupBy { it.translationKey ?: "" }

        // 构建TranslationRow列表
        return translationKeys.mapNotNull { key ->
            translationMap[key]?.let { translations ->
                TranslationRow(key, translations)
            }
        }
    }

    override fun getTotalTranslationKeysCount(projectId: String): Long {
        val sqlStr = "SELECT COUNT(DISTINCT translationKey) FROM tb_translation WHERE projectId = ?"
        println("sqlStr -> $sqlStr")

        return try {
            mJdbcTemplate.queryForObject(sqlStr, Long::class.java, projectId) ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    /**-------Translation Search by Content---------*/
    override fun searchTranslationKeysByContent(projectId: String, targetTranslationContent: String, languageId: Int): List<String> {
        val sqlStr = """
            SELECT DISTINCT t.translationKey
            FROM tb_translation t
            WHERE t.projectId = ? 
            AND t.languageId = ? 
            AND t.translationContent LIKE ?
            ORDER BY t.translationKey
        """.trimIndent()

        println("sqlStr -> $sqlStr")

        return try {
            val searchPattern = "%$targetTranslationContent%"
            mJdbcTemplate.queryForList(
                sqlStr,
                String::class.java,
                projectId,
                languageId,
                searchPattern
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getTranslationRowsByKeys(projectId: String, translationKeys: List<String>): List<TranslationRow> {
        if (translationKeys.isEmpty()) {
            return emptyList()
        }

        val placeholders = translationKeys.joinToString(",") { "?" }
        val sqlStr = """
            SELECT * FROM tb_translation 
            WHERE projectId = ? AND translationKey IN ($placeholders)
            ORDER BY translationKey, languageId
        """.trimIndent()

        println("sqlStr -> $sqlStr")

        val params = mutableListOf<Any>().apply {
            add(projectId)
            addAll(translationKeys)
        }

        return try {
            val allTranslations = mJdbcTemplate.query(
                sqlStr,
                PreparedStatementSetter { ps ->
                    params.forEachIndexed { index, param ->
                        ps.setObject(index + 1, param)
                    }
                },
                BeanPropertyRowMapper(Translation::class.java)
            )

            // 按translationKey分组
            val translationMap = allTranslations.groupBy { it.translationKey ?: "" }

            // 构建TranslationRow列表，保持原始顺序
            translationKeys.mapNotNull { key ->
                translationMap[key]?.let { translations ->
                    TranslationRow(key, translations)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**-------ProjectIp CRUD Operations---------*/
    override fun upsertProjectIp(ip: String, projectId: String): ProjectIp? {
        // First check if record exists
        val existingSqlStr = "SELECT * FROM project_ips WHERE ip = ? AND projectId = ?"
        println("existingSqlStr -> $existingSqlStr")
        
        return try {
            val existingRecords = mJdbcTemplate.query(existingSqlStr, PreparedStatementSetter {
                it.setString(1, ip)
                it.setString(2, projectId)
            }, BeanPropertyRowMapper(ProjectIp::class.java))
            
            val currentTimestamp = (System.currentTimeMillis() / 1000).toInt()
            
            if (existingRecords.isNotEmpty()) {
                // Record exists, update timestamp
                val existingRecord = existingRecords.first()
                val updateSqlStr = "UPDATE project_ips SET update_time = ? WHERE id = ?"
                println("updateSqlStr -> $updateSqlStr")
                
                val updateResult = mJdbcTemplate.update(updateSqlStr) {
                    it.setInt(1, currentTimestamp)
                    it.setInt(2, existingRecord.id!!)
                }
                
                if (updateResult > 0) {
                    println("更新ProjectIp时间戳: ${existingRecord.id}")
                    queryProjectIpById(existingRecord.id!!)
                } else {
                    null
                }
            } else {
                // Record doesn't exist, insert new one
                val insertSqlStr = "INSERT INTO project_ips(ip, projectId, update_time) VALUES(?, ?, ?)"
                println("insertSqlStr -> $insertSqlStr")
                
                val keyHolder = GeneratedKeyHolder()
                val affectedRows = mJdbcTemplate.update({ connection ->
                    val ps = connection.prepareStatement(insertSqlStr, arrayOf("id"))
                    ps.setString(1, ip)
                    ps.setString(2, projectId)
                    ps.setInt(3, currentTimestamp)
                    ps
                }, keyHolder)
                
                if (affectedRows > 0) {
                    keyHolder.key?.let { generatedId ->
                        println("新增ProjectIp: $generatedId")
                        queryProjectIpById(generatedId.toInt())
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    override fun deleteProjectIp(id: Int): Boolean {
        val sqlStr = "DELETE FROM project_ips WHERE id = ?"
        println("sqlStr -> $sqlStr")
        
        return try {
            mJdbcTemplate.update(sqlStr) {
                it.setInt(1, id)
            } > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    override fun queryProjectIpById(id: Int): ProjectIp? {
        val sqlStr = "SELECT * FROM project_ips WHERE id = ?"
        println("sqlStr -> $sqlStr")
        
        return try {
            val results = mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
                it.setInt(1, id)
            }, BeanPropertyRowMapper(ProjectIp::class.java))
            
            results.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    override fun queryProjectIpsByProjectId(projectId: String): List<ProjectIp> {
        val sqlStr = "SELECT * FROM project_ips WHERE projectId = ? ORDER BY update_time DESC"
        println("sqlStr -> $sqlStr")
        
        return try {
            mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
                it.setString(1, projectId)
            }, BeanPropertyRowMapper(ProjectIp::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    override fun queryProjectIpsByIp(ip: String): List<ProjectIp> {
        val sqlStr = "SELECT * FROM project_ips WHERE ip = ? ORDER BY update_time DESC"
        println("sqlStr -> $sqlStr")
        
        return try {
            mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
                it.setString(1, ip)
            }, BeanPropertyRowMapper(ProjectIp::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    override fun queryAllProjectIps(): List<ProjectIp> {
        val sqlStr = "SELECT * FROM project_ips ORDER BY update_time DESC"
        println("sqlStr -> $sqlStr")
        
        return try {
            mJdbcTemplate.query(sqlStr, BeanPropertyRowMapper(ProjectIp::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

   
    override fun upsertTranslationEngine(ip: String, engine: String): TranslationEnginePreference? {
        val currentTimestamp = (System.currentTimeMillis() / 1000).toInt()
        
        try {
            mJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS translation_engine_preference (ip VARCHAR(255) PRIMARY KEY, engine VARCHAR(50), update_time INT)")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return try {
            val existingRecord = queryTranslationEngineByIp(ip)
            if (existingRecord != null) {
                val updateSql = "UPDATE translation_engine_preference SET engine = ?, update_time = ? WHERE ip = ?"
                mJdbcTemplate.update(updateSql, engine, currentTimestamp, ip)
                queryTranslationEngineByIp(ip)
            } else {
                val insertSql = "INSERT INTO translation_engine_preference(ip, engine, update_time) VALUES(?, ?, ?)"
                mJdbcTemplate.update(insertSql, ip, engine, currentTimestamp)
                queryTranslationEngineByIp(ip)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun queryTranslationEngineByIp(ip: String): TranslationEnginePreference? {
        try {
            mJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS translation_engine_preference (ip VARCHAR(255) PRIMARY KEY, engine VARCHAR(50), update_time INT)")
        } catch (e: Exception) {
            // e.printStackTrace()
        }
        
        val sqlStr = "SELECT * FROM translation_engine_preference WHERE ip = ?"
        return try {
            val results = mJdbcTemplate.query(sqlStr, PreparedStatementSetter {
                it.setString(1, ip)
            }, BeanPropertyRowMapper(TranslationEnginePreference::class.java))
            results.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}