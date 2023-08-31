package com.hwj.translation.controller

import com.hwj.translation.bean.CommonResponse
import com.hwj.translation.bean.Language
import com.hwj.translation.bean.Project
import com.hwj.translation.bean.Translation
import com.hwj.translation.dao.TranslationDaoImpl
import com.hwj.translation.print
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.io.File
import java.nio.file.Paths

@RestController
class MainController {

    @Autowired
    private lateinit var mTranslationDao: TranslationDaoImpl

    @CrossOrigin
    @RequestMapping("/sayhello")
    fun helloWorld(): String {
        val currentDir = System.getProperty("user.dir")
        println("当前目录：$currentDir")
        val fileDir = File("$currentDir/files")
        fileDir.mkdirs()
        val translationFile = File("$currentDir/files", "translation.zip")
        val createNewFile = translationFile.createNewFile()
        print("createNewFile:$createNewFile")
        return translationFile.absolutePath
    }


    @CrossOrigin
    @GetMapping("/getAllTranslation/{projectId}")
    fun getAllTranslation(@PathVariable("projectId") projectId: String): CommonResponse<List<Translation>> {
        println("getAllTranslation/$projectId")
        val translationList = mTranslationDao.getAllTranslationByProjectId(projectId)
        return CommonResponse(200, null, translationList)
    }

    @CrossOrigin
    @RequestMapping("/getLanguageList/{projectId}")
    fun getLanguageList(@PathVariable projectId: String): CommonResponse<List<Language>> {
        val languageList = mTranslationDao.getLanguageList(projectId)
        println("getLanguageList/$projectId -> ")
        languageList.print()
        return CommonResponse(200, null, languageList)
    }

    @CrossOrigin
    @RequestMapping("getAllProjects")
    fun getProjects(): CommonResponse<List<Project>> {
        val projectList = mTranslationDao.getAllProject()
        return CommonResponse(200, null, projectList)
    }


    @CrossOrigin
    @PostMapping("/deleteLanguage")
    fun deleteLanguage(@RequestBody language: Language): CommonResponse<Void> {
        println("deleteLanguage:$language")
        if (language.languageId.isNullOrBlank() || language.projectId.isNullOrBlank()) {
            return CommonResponse(-1, "参数错误", null)
        }
        return try {
            var deleteTranslationSuccess =
                mTranslationDao.deleteTranslationByLanguageId(language.projectId!!, language.languageId!!)
            if (!deleteTranslationSuccess) {
                CommonResponse(-1, "删除翻译错误", null)
            } else {
                var deleteLanguageSuccess = mTranslationDao.deleteLanguage(language)
                if (!deleteLanguageSuccess) {
                    CommonResponse(-1, "删除语言错误", null)
                } else {
                    CommonResponse(200, "删除语言成功", null)
                }
            }
        } catch (e: Exception) {
            CommonResponse(-1, e.message, null)
        }
    }

    @CrossOrigin
    @PostMapping("/deleteTranslationByKey")
    fun deleteTranslationByTranslationKey(@RequestBody translation: Translation): CommonResponse<Void> {
        println("deleteTranslationByTranslationKey:$translation")
        if (translation.translationKey.isNullOrBlank() || translation.projectId.isNullOrBlank()) {
            return CommonResponse(-1, "参数错误", null)
        }
        return try {
            var deleteTranslationSuccess =
                mTranslationDao.deleteTranslationByTranslationKey(translation.projectId!!, translation.translationKey!!)
            if (!deleteTranslationSuccess) {
                CommonResponse(-1, "删除翻译错误", null)
            } else {
                CommonResponse(200, "删除翻译成功", null)
            }
        } catch (e: Exception) {
            CommonResponse(-1, e.message, null)
        }
    }


    @CrossOrigin
    @PostMapping("/addLanguage")
    fun addLanguage(@RequestBody language: Language): CommonResponse<Void> {
        println("addLanguage:$language")
        return try {
            if (language.projectId.isNullOrBlank()) {
                println("参数错误:projectId")
                CommonResponse(-1, "参数错误：projectId为空", null)
            } else if (language.languageId.isNullOrBlank()) {
                println("参数错误:languageId")
                CommonResponse(-1, "参数错误：languageId为空", null)
            } else if (language.languageName.isNullOrBlank()) {
                println("参数错误:languageName为空")
                CommonResponse(-1, "参数错误：languageName为空", null)
            } else {
                val queryLanguageList =
                    mTranslationDao.queryLanguageByLanguageId(language.languageId!!, language.projectId!!)
                if (!queryLanguageList.isNullOrEmpty()) {
                    println("语言已存在")
                    CommonResponse(-1, "语言已存在", null)
                } else {
                    val success = mTranslationDao.addLanguage(
                        language.languageId!!,
                        language.languageName!!,
                        language.projectId!!
                    )
                    if (success) {
                        println("添加成功")
                        CommonResponse(200, "添加成功", null)
                    } else {
                        println("添加项目失败")
                        CommonResponse(-1, "添加项目失败", null)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            CommonResponse(-1, e.message, null)
        }
    }

    @CrossOrigin
    @RequestMapping("addProject")
    fun addProject(
        @RequestBody project: Project
    ): CommonResponse<Void> {
        println("addProject:$project")
        return try {
            if (project.projectId.isNullOrBlank()) {
                println("参数错误:packageName为空")
                CommonResponse(-1, "参数错误：packageName为空", null)
            } else if (project.projectName.isNullOrBlank()) {
                println("参数错误:projectName为空")
                CommonResponse(-1, "参数错误：projectName为空", null)
            } else {
                val queryProjectByPackageName = mTranslationDao.queryProjectsByPackageName(project.projectId!!)
                if (null != queryProjectByPackageName && queryProjectByPackageName.isNotEmpty()) {
                    println("项目id已存在")
                    CommonResponse(-1, "项目id已存在", null)
                } else {
                    val success = mTranslationDao.addProject(Project().apply {
                        this.projectName = project.projectName
                        this.projectId = project.projectId
                    })
                    if (success) {
                        println("添加成功")
                        CommonResponse(200, "添加成功", null)
                    } else {
                        println("添加项目失败")
                        CommonResponse(-1, "添加项目失败", null)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            CommonResponse(-1, e.message, null)
        }
    }


    @CrossOrigin
    @PostMapping("/addTranslations")
    fun addTranslations(@RequestBody translationList: List<Translation>): CommonResponse<List<Translation>> {
        println("updateTranslations:${translationList.size}")
        val failedList = mutableListOf<Translation>()

        return try {

            translationList.forEach { tranlation ->
                tranlation.projectId?.let { projectId ->
                    tranlation.languageId?.let { languageId ->
                        tranlation.translationKey?.let { translationKey ->


                            val languageList = mTranslationDao.queryLanguageByLanguageId(languageId, projectId)
                            if (languageList.isNullOrEmpty()) {
                                val success = mTranslationDao.addLanguage(
                                    languageId,
                                    "",
                                    projectId
                                )
                                print("创建新语言 $success")
                            }

                            val translationDB =
                                mTranslationDao.queryTranslationByKey(translationKey, projectId, languageId)
                            if (translationDB.isEmpty()) {
                                val success = mTranslationDao.addTranslation(tranlation)
                                print("添加结果：$success $tranlation")
                                if (!success) {
                                    failedList.add(tranlation)
                                }
                            } else {
                                failedList.add(tranlation)
                            }
                        }
                    }
                }

            }
            if (failedList.isNotEmpty()) {
                print("")
            }
            CommonResponse(if (failedList.isEmpty()) 200 else -1, "success", failedList)
        } catch (e: Exception) {
            CommonResponse(-1, e.message, emptyList())
        }
    }


    @CrossOrigin
    @PostMapping("/updateTranslations")
    fun updateTranslations(@RequestBody translationList: List<Translation>?): CommonResponse<List<Translation>> {
        println("updateTranslations:${translationList?.size}")
        if (translationList.isNullOrEmpty()) {
            return CommonResponse(200, "", emptyList())
        }
        val failedList = mutableListOf<Translation>()
        try {
            translationList.forEach {
                val success = mTranslationDao.updateTranslation(it)
                print("更新结果：$success $it")
                if (!success) {
                    failedList.add(it)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return CommonResponse(200, "", failedList)
    }


}