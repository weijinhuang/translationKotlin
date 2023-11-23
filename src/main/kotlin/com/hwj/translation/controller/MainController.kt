package com.hwj.translation.controller

import com.hwj.translation.bean.CommonResponse
import com.hwj.translation.bean.Language
import com.hwj.translation.bean.Project
import com.hwj.translation.bean.Translation
import com.hwj.translation.bean.param.DeleteTranslationParam
import com.hwj.translation.bean.param.GetTranslationParam
import com.hwj.translation.dao.TranslationDaoImpl
import com.hwj.translation.print
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.io.File

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
    @PostMapping("/getAllTranslation")
    fun getTranslationList(@RequestBody getTranslationParam: GetTranslationParam): CommonResponse<List<Translation>> {
        val translationList = if (null == getTranslationParam.moduleId) {
            mTranslationDao.getAllTranslationByProjectId(getTranslationParam.projectId)
        } else {
            mTranslationDao.queryTranslationByModule(getTranslationParam.moduleId!!, getTranslationParam.projectId)
        }
        println("getTranslationList -> ${translationList.size}")
        return CommonResponse(200, null, translationList)
    }


    @CrossOrigin
    @RequestMapping("/getLanguageList/{projectId}")
    fun getLanguageList(@PathVariable projectId: String): CommonResponse<List<Language>> {
        val languageList = mTranslationDao.getLanguageList(projectId)
        println("getLanguageList/$projectId -> ${languageList.size}")
        languageList.print()
        return CommonResponse(200, null, languageList)
    }

    @CrossOrigin
    @RequestMapping("getAllProjects")
    fun getProjects(): CommonResponse<List<Project>> {
        val projectList = mTranslationDao.getAllProject()
        println("projectList -> ${projectList.size}")
        return CommonResponse(200, null, projectList)
    }


    @CrossOrigin
    @PostMapping("/deleteLanguage")
    fun deleteLanguage(@RequestBody language: Language): CommonResponse<Void> {
        println("deleteLanguage:$language")
        if (language.languageId == null || language.projectId.isNullOrBlank()) {
            return CommonResponse(-1, "参数错误", null)
        }
        return try {
            var deleteTranslationSuccess =
                mTranslationDao.deleteTranslationByLanguageId(language.projectId!!, language.languageId!!)
            if (!deleteTranslationSuccess) {
                CommonResponse(-1, "删除翻译错误", null)
            } else {
                var deleteLanguageSuccess = mTranslationDao.deleteLanguage(language.languageId!!)
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
    fun deleteTranslationByTranslationKey(@RequestBody deleteTranslationParam: DeleteTranslationParam): CommonResponse<Void> {
        println("deleteTranslationByTranslationKey:$deleteTranslationParam")
        if (deleteTranslationParam.translationKey.isNullOrBlank() || deleteTranslationParam.projectId.isNullOrBlank()) {
            return CommonResponse(-1, "参数错误", null)
        }
        return try {
            var deleteTranslationSuccess =
                mTranslationDao.deleteTranslationByTranslationKey(
                    deleteTranslationParam.projectId!!,
                    deleteTranslationParam.translationKey!!
                )
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
            } else if (language.languageName.isNullOrBlank()) {
                println("参数错误:languageName为空")
                CommonResponse(-1, "参数错误：languageName为空", null)
            } else {
                val queryLanguageList =
                    mTranslationDao.queryLanguageByLanguageName(language.languageName!!, language.projectId!!)
                if (!queryLanguageList.isNullOrEmpty()) {
                    println("语言已存在")
                    CommonResponse(-1, "语言已存在", null)
                } else {
                    val success = mTranslationDao.addLanguage(
                        language.languageDes!!,
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
    @RequestMapping("/deleteProject")
    fun deleteProject(@RequestBody project: Project): CommonResponse<Void> {
        return try {
            if (project.projectId.isNullOrEmpty()) {
                println("参数错误:projectId为空")
                CommonResponse(-1, "参数错误：projectId为空", null)
            } else {
                val result = mTranslationDao.deleteProject(project)
                if (result) {
                    CommonResponse(200, "删除成功", null)
                } else {
                    CommonResponse(-1, "删除失败", null)
                }
            }
        } catch (e: Exception) {
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
                CommonResponse(-1, "参数错误：projectId为空", null)
            } else {
                val queryProjectByPackageName = mTranslationDao.queryProjectsByProjectId(project.projectId!!)
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
        println("addTranslations:${translationList.size}")
        val failedList = mutableListOf<Translation>()
        return try {
            translationList.forEach { tranlation ->
                tranlation.projectId?.let { projectId ->
                    tranlation.languageId?.let { languageId ->
                        tranlation.translationKey?.let { translationKey ->
                            var add = tranlation.forceAdd
                            if (!add) {
                                val translationDB =
                                    mTranslationDao.queryTranslationByKeyInLanguage(
                                        translationKey,
                                        projectId,
                                        languageId
                                    )
                                if (translationDB.isNotEmpty()) {
                                    add = false
                                    if (tranlation.translationContent != translationDB[0].translationContent) {
                                        tranlation.oldTranslationContent = translationDB[0].translationContent
                                    }
                                } else {
                                    add = true
                                }
                            }
                            if (add) {
                                var module = com.hwj.translation.bean.Module()
                                var moduleDB = mTranslationDao.queryModuleById(tranlation.moduleId ?: 0, projectId)
                                if (moduleDB.isEmpty()) {
                                    module.moduleName = ""
                                    module.projectId = projectId
                                    var addModuleResult =
                                        mTranslationDao.addModule(module.moduleName, module.projectId!!)
                                    if (!addModuleResult) {
                                        return CommonResponse(-1, "add mudle failed", emptyList())
                                    }
                                } else {
                                    module = moduleDB[0]
                                }
                                tranlation.moduleId = module.moduleId
                                val success = mTranslationDao.addTranslation(tranlation)
                                print("添加结果：$success $tranlation")
                                if (!success) {
                                    failedList.add(tranlation)
                                }
                            } else {
                                if (!tranlation.oldTranslationContent.isNullOrEmpty()) {
                                    failedList.add(tranlation)
                                }
                            }
                        }
                    }
                }

            }
            if (failedList.isNotEmpty()) {
                print("")
            }
            CommonResponse(200, "success", failedList)
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


    @CrossOrigin
    @GetMapping("/getAllModules/{projectId}")
    fun getAllModules(@PathVariable("projectId") projectId: String): CommonResponse<List<com.hwj.translation.bean.Module>> {

        val moduleList = mTranslationDao.getAllModules(projectId)
        println("getAllTranslation/$projectId -> ${moduleList.size}")
        return CommonResponse(200, null, moduleList)
    }


    @CrossOrigin
    @PostMapping("/addModule")
    fun addModule(@RequestBody module: com.hwj.translation.bean.Module): CommonResponse<Void> {
        if (module.moduleName.isNullOrEmpty() || module.projectId.isNullOrEmpty()) {
            return CommonResponse(-1, "參數錯誤", null)
        }
        val queryModuleByName = mTranslationDao.queryModuleByName(module.moduleName!!, module.projectId!!)
        if (queryModuleByName.isNotEmpty()) {
            return CommonResponse(-1, "已有相同模块", null)
        }
        mTranslationDao.addModule(module.moduleName!!, module.projectId!!)
        return CommonResponse(200, "", null)
    }

    @CrossOrigin
    @PostMapping("/deleteModule")
    fun deleteModule(@RequestBody module: com.hwj.translation.bean.Module): CommonResponse<Void> {
        if (module.moduleName.isNullOrEmpty() || module.projectId.isNullOrEmpty()) {
            return CommonResponse(-1, "參數錯誤", null)
        }
        return try {
            mTranslationDao.deleteModule(module.moduleId!!, module.projectId!!)
            return CommonResponse(200, "", null)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return CommonResponse(-1, e.message, null)
        }
    }


}