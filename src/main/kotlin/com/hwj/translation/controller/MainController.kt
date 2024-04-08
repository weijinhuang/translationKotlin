package com.hwj.translation.controller

import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hwj.translation.baidu.TransApi
import com.hwj.translation.bean.*
import com.hwj.translation.bean.param.*
import com.hwj.translation.dao.TranslationDaoImpl
import com.hwj.translation.print
import com.hwj.translation.util.log
import io.github.evanrupert.excelkt.workbook
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.collections.HashMap


@RestController
class MainController {

    val gson = Gson()

    @Autowired
    private lateinit var mTranslationDao: TranslationDaoImpl

    @Autowired
    private var mRequest: HttpServletRequest? = null

    fun loadGoogleCredentials() {
//        val googleCredentialsFile = ""
//        FileInputStream(googleCredentialsFile).use { fileInputStream ->
//            val googleCredentials = GoogleCredentials.fromStream(fileInputStream)
//            googleCredentials.refreshIfExpired()
//            googleCredentials.accessToken
//
//        }
        val translate: Translate = TranslateOptions.getDefaultInstance().service

    }

    @CrossOrigin
    @RequestMapping("getGoogleSupportLanguage")
    fun getGoogleSupportLanguage() {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");
        println("TranslationByGoogle")
        val translateService = TranslateOptions.getDefaultInstance().service
        val listSupportedLanguages = translateService.listSupportedLanguages()
        listSupportedLanguages.forEach {
            println("${it.name}:${it.code}")
        }
    }


    @CrossOrigin
    @RequestMapping("/translationSystem")
    fun <PARAM, RESPONSE> mainEntrance(@RequestBody param: CommonParam<PARAM>): CommonResponse<RESPONSE?> {
        println("====V2 Request ${param.cmd} ${Date()}")
        val commonResponse = when (param.cmd) {
            GET_ALL_PROJECTS -> getProjectsV2()
            GET_ALL_TRANSLATION -> getTranslationListV2(param)
            TRANSLATE_BY_GOOGLE -> translateByGoogleV2(param)
            GET_LANGUAGE_LIST -> getLanguageListV2(param)
            DELETE_LANGUAGE -> deleteLanguageV2(param)
            DELETE_TRANSLATION_BY_KEY -> deleteTranslationByTranslationKeyV2(param)
            ADD_LANGUAGE -> addLanguagesV2(param)
            DELETE_PROJECT -> deleteProjectV2(param)
            ADD_PROJECT -> addProjectV2(param)
            ADD_TRANSLATION -> addTranslationsV2(param)
            UPDATE_TRANSLATION -> updateTranslationsV2(param)
            TRANSLATE_BY_BAIDU -> translateByBaiduV2(param)
            GET_ALL_MODULES -> getAllModulesV2(param)
            ADD_MODULE -> addModuleV2(param)
            DELETE_MODULE -> deleteModuleV2(param)
            null -> CommonResponse(code = -1, msg = "接口名为空", null)
            else -> CommonResponse(code = 400, msg = "未知接口${param.cmd}", null)
        }
        println("----V2 Request${param.cmd} ${Date()}\n\n\n")
        return commonResponse as CommonResponse<RESPONSE?>
    }


    /***************************************V2 Request**********************************************/


    fun translateByGoogleV2(param: CommonParam<*>): CommonResponse<TranslationResult?> {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");

        return param.data?.let {
            gson.fromJson(gson.toJson(param.data), GoogleTranslationParam::class.java)?.let { realParam ->
                val translateService = TranslateOptions.getDefaultInstance().service
                var sourceLanguage = realParam.sourceLanguage
                if (sourceLanguage.isNullOrEmpty()) {
                    val detection = translateService.detect(realParam.content)
                    sourceLanguage = detection.language
                    println("检测到语言:$sourceLanguage")
                }
                return try {
                    val translateResult =
                        translateService.translate(realParam.content, Translate.TranslateOption.sourceLanguage(sourceLanguage), Translate.TranslateOption.targetLanguage(realParam.targetLanguage))
                    println("翻译结果：${translateResult.translatedText} model:${translateResult.model}")
                    CommonResponse(200, "", TranslationResult().apply {
                        this.sourceLanguage = realParam.sourceLanguage
                        targetLanguage = realParam.targetLanguage
                        transResult = translateResult.translatedText
                        errorCode = 0
                    })
                } catch (e: Exception) {
                    CommonResponse(-1, e.message, null);
                }
            } ?: CommonResponse(-1, "参数解析出错", null)

        } ?: CommonResponse(-1, "参数为空", null)
    }

    fun translateByBaiduV2(commonParam: CommonParam<*>): CommonResponse<TranslationResult> {
        return parseRealParam(commonParam, BaiduTranslationParam::class.java)?.let { param ->
            val api = TransApi(BAIDU_APP_ID, BAIDU_SCRECT)
            val to = param.to
            param.to = when (to) {
                "es" -> "spa"
                "fr" -> "fra"
                "ja" -> "jp"
                "zh-CN" -> "zh"
                else -> to
            }

            val transResult = api.getTransResult(param.q, param.from, param.to)
            println("$param->$transResult")
            val baiduTranslationResultResponse = Gson().fromJson(transResult, BaiduTranslationResult::class.java)

            return if (baiduTranslationResultResponse.error_code != null) {
                CommonResponse(
                    -1, baiduTranslationResultResponse.error_code, null
                )
            } else {
                println("baiduTranslationResultResponse:$baiduTranslationResultResponse")
                val result = TranslationResult().apply {
                    sourceLanguage = param.from
                    targetLanguage = param.to
                    this.transResult = baiduTranslationResultResponse.trans_result?.get(0)?.dst ?: ""
                    errorCode = 0
                }
                CommonResponse(
                    200, "", result
                )
            }
        } ?: CommonResponse(-1, "参数解析错误", null)

    }

    fun getTranslationListV2(param: CommonParam<*>): CommonResponse<List<Translation>> {
        return parseRealParam(param, GetTranslationParam::class.java)?.let { realParam ->
            val translationList = if (null == realParam.moduleId) {
                mTranslationDao.getAllTranslationByProjectId(realParam.projectId)
            } else {
                mTranslationDao.queryTranslationByModule(realParam.moduleId!!, realParam.projectId)
            }
            println("查詢到翻譯：${translationList.size}")
            CommonResponse(200, "", translationList)
        } ?: CommonResponse(-1, "参数错误", emptyList())
    }

    fun getLanguageListV2(param: CommonParam<*>): CommonResponse<List<Language>> {
        return parseRealParam<QueryLanguageListParam>(param, QueryLanguageListParam::class.java)?.let { realParam ->
            realParam.projectId?.let { projectId ->
                val languageList = mTranslationDao.getLanguageList(projectId)
                return CommonResponse(200, null, languageList)
            } ?: CommonResponse(-1, "ProjectId为空", emptyList())
        } ?: CommonResponse(-1, "ProjectId为空", emptyList())
    }

    fun getProjectsV2(): CommonResponse<List<Project>> {
        val projectList = mTranslationDao.getAllProject()
        log(mRequest?.remoteAddr, "projectList -> ${projectList.size}")
        return CommonResponse(200, null, projectList)
    }


    fun deleteLanguageV2(param: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(param, Language::class.java)?.let { language ->
            if (language.languageId == null || language.projectId.isNullOrBlank()) {
                return CommonResponse(-1, "参数错误", null)
            }
            return try {
                var deleteTranslationSuccess = mTranslationDao.deleteTranslationByLanguageId(language.projectId!!, language.languageId!!)
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
                e.printStackTrace()
                CommonResponse(-1, e.message, null)
            }
        } ?: CommonResponse(-1, "参数错误", null)
    }

    fun deleteTranslationByTranslationKeyV2(param: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(param, DeleteTranslationParam::class.java)?.let { deleteTranslationParam ->
            if (deleteTranslationParam.translationKey.isNullOrBlank() || deleteTranslationParam.projectId.isNullOrBlank()) {
                return CommonResponse(-1, "参数错误", null)
            }
            return try {
                var deleteTranslationSuccess = mTranslationDao.deleteTranslationByTranslationKey(
                    deleteTranslationParam.projectId!!, deleteTranslationParam.translationKey!!
                )
                if (!deleteTranslationSuccess) {
                    CommonResponse(-1, "删除翻译错误", null)
                } else {
                    CommonResponse(200, "删除翻译成功", null)
                }
            } catch (e: Exception) {
                CommonResponse(-1, e.message, null)
            }
        } ?: CommonResponse(-1, "参数错误", null)

    }

    fun addLanguagesV2(param: CommonParam<*>): CommonResponse<List<Language>> {
        val typeToken = object : TypeToken<List<Language>>() {}.type
        val toJson = gson.toJson(param.data)
        return gson.fromJson<List<Language>>(toJson, typeToken)?.let { languageList ->
            val resultList = mutableListOf<Language>()
            if (languageList.isNotEmpty()) {
                languageList.forEach { language ->
                    try {
                        val newLanguage = language.projectId?.let { projectId ->
                            language.languageName?.let { languageName ->
                                val queryLanguageList = mTranslationDao.queryLanguageByLanguageName(language.languageName!!, language.projectId!!)
                                if (!queryLanguageList.isNullOrEmpty()) {
                                    println("语言已存在")
                                    queryLanguageList[0]
                                } else {
                                    val success = mTranslationDao.addLanguage(
                                        language.languageDes!!, language.languageName!!, language.projectId!!
                                    )
                                    if (success) {
                                        println("添加成功")
                                        mTranslationDao.queryLanguageByLanguageName(languageName, projectId)?.get(0)
                                    } else {
                                        println("添加语言失败")
                                        return CommonResponse(-1, "添加语言失败", emptyList())
                                    }
                                }
                            }
                        }
                        newLanguage?.let { resultList.add(it) }
                    } catch (e: Exception) {
                        return CommonResponse(-1, e.message, emptyList())
                    }
                }
                if (resultList.size == languageList.size) {
                    CommonResponse(200, "", resultList)
                } else {
                    CommonResponse(-1, "添加语言失败", resultList)
                }
            }
            CommonResponse(200, "", resultList)
        } ?: CommonResponse(-1, "", emptyList())
    }


    fun addTranslationsV2(commonParam: CommonParam<*>): CommonResponse<List<Translation>> {
        val typeToken = object : TypeToken<List<Translation>>() {}.type
        val realParamJson = gson.toJson(commonParam.data)
        return gson.fromJson<List<Translation>>(realParamJson, typeToken)?.let { translationList ->
            val failedList = mutableListOf<Translation>()
            return try {
                translationList.forEach { translation ->
                    translation.projectId?.let { projectId ->
                        translation.languageId?.let { languageId ->
                            translation.translationKey?.let { translationKey ->
                                val translationDB = mTranslationDao.queryTranslationByKeyInLanguage(translationKey, projectId, languageId)
                                if (translationDB.isNotEmpty()) {
                                    if (translation.forceAdd) {
                                        translation.translationId = translationDB[0].translationId
                                        var updateSuccess = mTranslationDao.updateTranslation(translation)
                                        if (!updateSuccess) {
                                            failedList.add(translation)
                                        }
                                    } else {
                                        print(" ${translation.translationKey} 已存在")
                                        if (translation.translationContent != translationDB[0].translationContent) {
                                            translation.oldTranslationContent = translationDB[0].translationContent
                                            failedList.add(translation)
                                        }
                                    }
                                } else {
                                    val module = getModule(translation, projectId) ?: return CommonResponse(-1, "添加模块失败", null)
                                    translation.moduleId = module.moduleId
                                    val success = mTranslationDao.addTranslation(translation)
                                    if (!success) {
                                        print(" ${translation.translationKey} 添加失败, content:${translation.translationContent}")
                                        failedList.add(translation)
                                    } else {
                                        print(" ${translation.translationKey} 添加成功, content:${translation.translationContent}")
                                    }
                                }
                            }
                        }
                    }
                }
                CommonResponse(200, "success", failedList)
            } catch (e: Exception) {
                CommonResponse(-1, e.message, emptyList())
            }
        } ?: CommonResponse(-1, "参数解析错误", emptyList())
    }

    val moduleCaches = HashMap<Int, Module>()

    fun getModule(translation: Translation, projectId: String): Module? {
        var module = moduleCaches[translation.moduleId]
        if (module == null) {
            var moduleDB = mTranslationDao.queryModuleById(translation.moduleId ?: 0, projectId)
            if (moduleDB.isEmpty()) {
                module = Module()
                module.moduleName = ""
                module.projectId = projectId
                var addModuleResult = mTranslationDao.addModule(module.moduleName, module.projectId!!)
                if (!addModuleResult) {
                    return null
                }
            } else {
                module = moduleDB[0]
                moduleCaches.put(translation.moduleId ?: 0, module)
            }
        }
        return module
    }


    fun updateTranslationsV2(commonParam: CommonParam<*>): CommonResponse<List<Translation>> {
        val typeToken = object : TypeToken<List<Translation>>() {}.type
        return gson.fromJson<List<Translation>>(gson.toJson(commonParam.data), typeToken)?.let { translationList ->
            if (translationList.isNullOrEmpty()) {
                CommonResponse(200, "", emptyList())
            } else {
                val failedList = mutableListOf<Translation>()
                try {
                    translationList.forEach {
                        val success = mTranslationDao.updateTranslation(it)
                        print("更新结果：$success $it")
                        if (!success) {
                            failedList.add(it)
                        }
                    }
                    CommonResponse(200, "", failedList)
                } catch (e: Exception) {
                    e.printStackTrace()
                    CommonResponse(200, e.message, failedList)
                }
            }

        } ?: CommonResponse(-1, "解析參數錯誤", emptyList())

    }

    fun getAllModulesV2(commonParam: CommonParam<*>): CommonResponse<List<Module>> {
        return parseRealParam(commonParam, String::class.java)?.let { projectId ->
            var moduleList = mTranslationDao.getAllModules(projectId)
            log(mRequest?.remoteAddr, "/getAllModules/$projectId -> ${moduleList.size}")
            if (moduleList.size == 0) {
                mTranslationDao.addModule("default", projectId)
            }
            moduleList = mTranslationDao.getAllModules(projectId)
            CommonResponse(200, null, moduleList)
        } ?: CommonResponse(-1, "解析参数出错", emptyList())

    }


    fun addModuleV2(commonParam: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(commonParam, Module::class.java)?.let { module ->
            if (module.moduleName.isNullOrEmpty() || module.projectId.isNullOrEmpty()) {
                return CommonResponse(-1, "參數錯誤", null)
            }
            val queryModuleByName = mTranslationDao.queryModuleByName(module.moduleName!!, module.projectId!!)
            if (queryModuleByName.isNotEmpty()) {
                return CommonResponse(-1, "已有相同模块", null)
            }
            mTranslationDao.addModule(module.moduleName!!, module.projectId!!)
            CommonResponse(200, "", null)
        } ?: CommonResponse(-1, "参数解析出错", null)

    }

    fun deleteModuleV2(commonParam: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(commonParam, Module::class.java)?.let { module ->
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
        } ?: CommonResponse(-1, "参数解析出错", null)

    }

    /***************************************V2 Request**********************************************/

    @CrossOrigin
    @RequestMapping("/translateByGoogle")
    fun translateByGoogle(@RequestBody param: GoogleTranslationParam): CommonResponse<TranslationResult> {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");
        println("TranslationByGoogle")
        val translateService = TranslateOptions.getDefaultInstance().service
        var sourceLanguage = param.sourceLanguage
        if (sourceLanguage.isNullOrEmpty()) {
            val detection = translateService.detect(param.content)
            sourceLanguage = detection.language
            println("检测到语言:$sourceLanguage")
        }
        try {
            val translateResult = translateService.translate(param.content, Translate.TranslateOption.sourceLanguage(sourceLanguage), Translate.TranslateOption.targetLanguage(param.targetLanguage))
            println("翻译结果：${translateResult.translatedText} model:${translateResult.model}")
            return CommonResponse(200, "", TranslationResult().apply {
                this.sourceLanguage = param.sourceLanguage
                targetLanguage = param.targetLanguage
                transResult = translateResult.translatedText
                errorCode = 0
            })
        } catch (e: Exception) {
            return CommonResponse(-1, "", TranslationResult().apply { });
        }

    }


    @CrossOrigin
    @RequestMapping("/translateByBaidu2")
    fun translateByBaidu2(@RequestBody param: BaiduTranslationParam): CommonResponse<TranslationResult> {
        val api = TransApi(BAIDU_APP_ID, BAIDU_SCRECT)
        val to = param.to
        param.to = when (to) {
            "es" -> "spa"
            "fr" -> "fra"
            "ja" -> "jp"
            "zh-CN" -> "zh"
            else -> to
        }

        val transResult = api.getTransResult(param.q, param.from, param.to)
        println("$param->$transResult")
        val baiduTranslationResultResponse = Gson().fromJson(transResult, BaiduTranslationResult::class.java)

        return if (baiduTranslationResultResponse.error_code != null) {
            CommonResponse(
                -1, baiduTranslationResultResponse.error_code, null
            )
        } else {
            println("baiduTranslationResultResponse:$baiduTranslationResultResponse")
            val result = TranslationResult().apply {
                sourceLanguage = param.from
                targetLanguage = param.to
                this.transResult = baiduTranslationResultResponse.trans_result?.get(0)?.dst ?: ""
                errorCode = 0
            }
            CommonResponse(
                200, "", result
            )


        }
    }


    @CrossOrigin
    @RequestMapping("/sayhello")
    fun helloWorld(): String {
        log(mRequest?.remoteAddr, "sayHello")
        Thread.sleep(5000)
        val currentDir = System.getProperty("user.dir")
        println("当前目录：$currentDir")
        val fileDir = File("$currentDir/files")
        deleteCache(fileDir)

        deleteCache(File("$currentDir/cache"))
        return ""
    }

    @CrossOrigin
    @PostMapping("/exportTranslation2")
    fun exportTranslation2(@RequestBody exportTranslationParam: ExportTranslationParam): ResponseEntity<ByteArray> {
        log(mRequest?.remoteAddr, "/exportTranslation2")
        exportTranslationParam.projectIdList.forEach {
            print("$it  ")
        }
        if (exportTranslationParam.platform == "android") {
            return exportAndroid(exportTranslationParam)
        } else if (exportTranslationParam.platform == "ios") {
            return exportIOS(exportTranslationParam)
        } else {
            return exportExcel(exportTranslationParam)
        }
    }

    fun exportExcel(param: ExportTranslationParam): ResponseEntity<ByteArray> {
        log(mRequest?.remoteAddr, "exportExcel")
        val mainProjectId = param.projectIdList.first()

        val mainLanguageList = mTranslationDao.getLanguageList(mainProjectId)
        val subLanguageList = mutableListOf<Language>()
        if (param.projectIdList.size > 1) {
            for (i in 1 until param.projectIdList.size) {
                subLanguageList.addAll(mTranslationDao.getLanguageList(param.projectIdList[i]))
            }
        }
        if (mainLanguageList.isNotEmpty()) {
            //创建zip目录
            val currentDir = System.getProperty("user.dir")
            println("当前目录：$currentDir")
            val cacheDir = File("$currentDir/cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach {
                    deleteCache(it)
                }
            } else {
                cacheDir.mkdirs()
            }


            val keyLanguageContentMap: HashMap<String, HashMap<Int, String>> = HashMap()

            //分语言导出
            for (language in mainLanguageList) {
                val mainTranslationList: List<Translation> = mTranslationDao.queryTranslationByLanguage(language.languageId ?: 0, language.projectId ?: "")
                mainTranslationList.forEach { translation ->
                    parseTranslationToMap(translation.languageId ?: 0, translation, keyLanguageContentMap)
                }

                subLanguageList.forEach { subLanguage ->
                    if (subLanguage.languageName == language.languageName) {
                        val subTranslationList = mTranslationDao.queryTranslationByLanguage(subLanguage.languageId ?: 0, subLanguage.projectId ?: "")
                        subTranslationList.forEach { translation ->
                            parseTranslationToMap(language.languageId ?: 0, translation, keyLanguageContentMap)
                        }
                    }
                }
            }

            val fileDir = "$currentDir/files"
            File(fileDir).let { files ->
                if (files.exists()) {
                    files.listFiles()?.forEach {
                        deleteCache(it)
                    }
                } else {
                    files.mkdirs()
                }
            }

            workbook {
                sheet {
                    row {
                        cell("Key")
                        mainLanguageList.forEach { language ->
                            cell("${language.languageName}(${language.languageDes})")
                        }
                    }
                    keyLanguageContentMap.keys.forEach { translationKey ->
                        row {
                            cell(translationKey)
                            mainLanguageList.forEach { language ->
                                cell(keyLanguageContentMap[translationKey]?.get(language.languageId ?: 0) ?: "")
                            }
                        }
                    }
                }
            }.write("$fileDir/$mainProjectId.xlsx")

            println("合并excel完毕：$fileDir/$mainProjectId.xlsx")
            val readBytes = File("$fileDir/$mainProjectId.xlsx").readBytes()
            val headers = HttpHeaders()
            headers.setContentDispositionFormData("attachment", "$mainProjectId.xlsx")
            headers.contentType = MediaType.APPLICATION_OCTET_STREAM
            return ResponseEntity.ok().headers(headers).contentLength(readBytes.size.toLong()).body(readBytes)
        }
        return ResponseEntity.ok().body(ByteArray(0))

    }

    private fun parseTranslationToMap(languageId: Int, translation: Translation, keyLanguageContentMap: HashMap<String, HashMap<Int, String>>) {

        translation.translationKey?.let { translationKey ->
            translation.translationContent?.let { translationContent ->
                var languageContentMap = keyLanguageContentMap[translationKey]
                if (null == languageContentMap) {
                    languageContentMap = HashMap()
                    keyLanguageContentMap[translationKey] = languageContentMap
                }
                languageContentMap[languageId] = translationContent
            }

        }

    }

    fun exportIOS(param: ExportTranslationParam): ResponseEntity<ByteArray> {
        log(mRequest?.remoteAddr, "exportIOS")
        val mainProjectId = param.projectIdList.first()

        val mainLanguageList = mTranslationDao.getLanguageList(mainProjectId)
        val subLanguageList = mutableListOf<Language>()
        if (param.projectIdList.size > 1) {
            for (i in 1 until param.projectIdList.size) {
                subLanguageList.addAll(mTranslationDao.getLanguageList(param.projectIdList[i]))
            }
        }


        if (mainLanguageList.isNotEmpty()) {
            //创建zip目录
            val currentDir = System.getProperty("user.dir")
            println("当前目录：$currentDir")
            val cacheDir = File("$currentDir/cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach {
                    deleteCache(it)
                }
            } else {
                cacheDir.mkdirs()
            }

            val languageDirList: MutableList<File> = mutableListOf()

            //分语言导出
            for (language in mainLanguageList) {
                val translationInLanguage = mutableListOf<Translation>()
                val mainTranslationList: List<Translation> = mTranslationDao.queryTranslationByLanguage(language.languageId ?: 0, language.projectId ?: "")
                mainTranslationList.forEach { translationInLanguage.add(it) }

                subLanguageList.forEach { subLanguage ->
                    if (subLanguage.languageName == language.languageName) {
                        val subTranslationList = mTranslationDao.queryTranslationByLanguage(subLanguage.languageId ?: 0, subLanguage.projectId ?: "")
                        subTranslationList.forEach { translationInLanguage.add(it) }
                    }
                }

                println("查询到翻译数量：${translationInLanguage.size}")
                translationInLanguage.let { translationList ->
                    //创建目录
                    val dirName = when (language.languageName) {
                        "spa" -> "es.lproj"
                        "fra" -> "fr.lproj"
                        "jp" -> "ja.lproj"
                        "zh-CN" -> "zh-Hans.lproj"
                        "zh-TW" -> "zh-Hant.lproj"
                        else -> "${language.languageName}.lproj"
                    }

                    val languageDir = File("$cacheDir/$dirName")
                    if (!languageDir.exists()) {
                        val success = languageDir.mkdirs()
                        println("创建目录$languageDir $success")
                    }
                    languageDirList.add(languageDir)

                    val file = File(languageDir, "Localizable.strings")
                    file.createNewFile()
                    println("创建${file.absolutePath}")
                    FileOutputStream(file).use { fos ->
                        translationList.forEach { translation ->
                            translation?.translationKey?.let { translationKey ->
                                translation.translationContent?.let { translationContent ->
//                                    if (translationContent.contains("|")) {
//                                        val contentArray = translationContent.split("|")
//                                        var i = 0
//                                        contentArray.forEach { contentItem ->
//                                            fos.write("\"$translationKey\"${i++}=\"$contentItem\"\n;".toByteArray())
//                                        }
//
//                                    } else {
                                    fos.write("\"$translationKey\"=\"$translationContent\";\n".toByteArray())
//                                    }
                                }
                            }
                        }
                    }
                }

            }

            val fileDir = "$currentDir/files"
            File(fileDir).let { files ->
                if (files.exists()) {
                    files.listFiles()?.forEach {
                        deleteCache(it)
                    }
                } else {
                    files.mkdirs()
                }
            }

            val zipFile = File(fileDir + File.separator + "strings.zip")
            FileOutputStream(zipFile, false).use { output ->
                ZipOutputStream(BufferedOutputStream(output)).use { zipOut ->
                    addFilesToZip(cacheDir.absolutePath, "", zipOut)
                }
            }

            println("压缩完毕：${zipFile.absolutePath}")
            val readBytes = zipFile.readBytes()
            val headers = HttpHeaders()
            headers.setContentDispositionFormData("attachment", "strings.zip")
            headers.contentType = MediaType.APPLICATION_OCTET_STREAM
            return ResponseEntity.ok().headers(headers).contentLength(readBytes.size.toLong()).body(readBytes)
        }
        return ResponseEntity.ok().body(ByteArray(0))
    }

    fun exportAndroid(param: ExportTranslationParam): ResponseEntity<ByteArray> {
        log(mRequest?.remoteAddr, "exportAndroid")
        val mainProjectId = param.projectIdList.first()
        val manLanguageList = mTranslationDao.getLanguageList(mainProjectId)

        val subLanguageList = mutableListOf<Language>()
        if (param.projectIdList.size > 1) {
            for (i in 1 until param.projectIdList.size) {
                subLanguageList.addAll(mTranslationDao.getLanguageList(param.projectIdList[i]))
            }
        }


        if (manLanguageList.isNotEmpty()) {
            //创建zip目录
            val currentDir = System.getProperty("user.dir")
            println("当前目录：$currentDir")
            val cacheDir = File("$currentDir/cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach {
                    deleteCache(it)
                }
            } else {
                cacheDir.mkdirs()
            }

            val languageDirList: MutableList<File> = mutableListOf()

            //分语言导出
            for (language in manLanguageList) {
                val translationInLanguage = mutableListOf<Translation>()
                val mainProjectTranslationList: List<Translation> = mTranslationDao.queryTranslationByLanguage(language.languageId ?: 0, language.projectId ?: "")
                mainProjectTranslationList.forEach { translationInLanguage.add(it) }
                subLanguageList.forEach { subLanguage ->
                    if (subLanguage.languageName == language.languageName) {
                        val subTranslationList = mTranslationDao.queryTranslationByLanguage(subLanguage.languageId ?: 0, subLanguage.projectId ?: "")
                        subTranslationList.forEach { translationInLanguage.add(it) }
                    }
                }
                println("查询到翻译数量：${translationInLanguage.size}")
                translationInLanguage.let { translationList ->
                    //创建目录
                    val dirName = when (language.languageName) {
                        "en" -> "values"
                        "spa" -> "values-es"
                        "fra" -> "values-fr"
                        "jp" -> "values-ja"
                        "zh-CN" -> "values-zh"
                        else -> "values-${language.languageName}"
                    }

                    val languageDir = File("$cacheDir/$dirName")
                    if (!languageDir.exists()) {
                        val success = languageDir.mkdirs()
                        println("创建目录$languageDir $success")
                    }
                    languageDirList.add(languageDir)

                    //创建xml
                    val xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
                    val resources = xmlDoc.createElement("resources")
                    translationList.forEach { translation ->
                        translation?.translationKey?.let { translationKey ->
                            translation.translationContent?.let { translationContent ->
                                if (translationContent.contains("|")) {
                                    val `string-array` = xmlDoc.createElement("string-array")
                                    `string-array`.setAttribute("name", translationKey)
                                    val contentArray = translationContent.split("|")
                                    contentArray.forEach { contentItem ->
                                        val item = xmlDoc.createElement("item")
                                        item.textContent = contentItem
                                        `string-array`.appendChild(item)
                                    }
                                    resources.appendChild(`string-array`)
                                } else {
                                    val string = xmlDoc.createElement("string")
                                    string.setAttribute("name", translationKey)
                                    string.textContent = translationContent
                                    resources.appendChild(string)
                                }
                            }
                        }
                    }
                    xmlDoc.appendChild(resources)
                    val xmlFile = File(languageDir, "strings.xml")
                    println("创建${xmlFile.absolutePath}")
                    val success = xmlFile.createNewFile()
                    print("$success")
                    val transformFactory = TransformerFactory.newInstance()
                    val transformer = transformFactory.newTransformer()
                    val source = DOMSource(xmlDoc)
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                    val result = StreamResult(xmlFile)
                    transformer.transform(source, result)
                    print("生成${xmlFile.absolutePath}")
                }

            }
            val fileDir = "$currentDir/files"
            File(fileDir).let { files ->
                if (files.exists()) {
                    files.listFiles()?.forEach {
                        deleteCache(it)
                    }
                } else {
                    files.mkdirs()
                }

            }

            val zipFile = File(fileDir + File.separator + "strings.zip")
            FileOutputStream(zipFile, false).use { output ->
                ZipOutputStream(BufferedOutputStream(output)).use { zipOut ->
                    addFilesToZip(cacheDir.absolutePath, "", zipOut)
                }
            }

            println("压缩完毕：${zipFile.absolutePath}")
            val readBytes = zipFile.readBytes()
            val headers = HttpHeaders()
            headers.setContentDispositionFormData("attachment", "strings.zip")
            headers.contentType = MediaType.APPLICATION_OCTET_STREAM
            return ResponseEntity.ok().headers(headers).contentLength(readBytes.size.toLong()).body(readBytes)
        }
        return ResponseEntity.ok().body(ByteArray(0))
    }

    private fun deleteCache(cacheDirFile: File) {
        if (cacheDirFile.isFile) {
            println("删除：${cacheDirFile.absolutePath}")
            cacheDirFile.delete()
        } else {
            println("遍历目录：${cacheDirFile.absolutePath}")
            val listFiles = cacheDirFile.listFiles()
            listFiles?.forEach {
                if (it.isFile) {
                    println("删除：${it.absolutePath}")
                    it.delete()
                } else {
                    deleteCache(it)
                }
            }
            println("删除目录：${cacheDirFile.absolutePath}")
            cacheDirFile.delete()
        }
    }

    private fun addFilesToZip(directory: String, parentDirectoryName: String, zipOut: ZipOutputStream) {
        println(" addFilesToZip($directory: String, $parentDirectoryName: String, zipOut: ZipOutputStream)")
        val folder = File(directory)
        if (folder.exists()) {
            folder.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    addFilesToZip(file.absolutePath, "$parentDirectoryName${file.name}/", zipOut)
                } else {
                    val entryName = "$parentDirectoryName${file.name}"
                    BufferedInputStream(FileInputStream(file)).use { fileInputStream ->
                        val zipEntry = ZipEntry(entryName)
                        zipOut.putNextEntry(zipEntry)

                        var bytesRead: Int
                        val buffer = ByteArray(1024)
                        while (true) {
                            bytesRead = fileInputStream.read(buffer, 0, buffer.size)
                            if (bytesRead == -1) break
                            zipOut.write(buffer, 0, bytesRead)
                        }

                        fileInputStream.close()
                        zipOut.closeEntry()
                    }

                }
            }
        }
    }


    @CrossOrigin
    @PostMapping("/getAllTranslation")
    fun getTranslationList(@RequestBody getTranslationParam: GetTranslationParam): CommonResponse<List<Translation>> {
        log(mRequest?.remoteAddr, "getAllTranslation")
        val translationList = if (null == getTranslationParam.moduleId) {
            mTranslationDao.getAllTranslationByProjectId(getTranslationParam.projectId)
        } else {
            mTranslationDao.queryTranslationByModule(getTranslationParam.moduleId!!, getTranslationParam.projectId)
        }
        println("getTranslationList -> ${translationList.size}")

        return CommonResponse(200, "", translationList)
    }


    @CrossOrigin
    @RequestMapping("/getLanguageList/{projectId}")
    fun getLanguageList(@PathVariable projectId: String): CommonResponse<List<Language>> {
        val languageList = mTranslationDao.getLanguageList(projectId)
        log(mRequest?.remoteAddr, "getLanguageList/$projectId -> ${languageList.size}")
        languageList.print()
        return CommonResponse(200, null, languageList)
    }

    private var mCount = 0

    @CrossOrigin
    @RequestMapping("getAllProjects")
    fun getProjects(): CommonResponse<List<Project>> {

        val projectList = mTranslationDao.getAllProject()
        log(mRequest?.remoteAddr, "projectList -> ${projectList.size}")
        return CommonResponse(200, null, projectList)
    }

    @CrossOrigin
    @PostMapping("/deleteLanguage")
    fun deleteLanguage(@RequestBody language: Language): CommonResponse<Void> {
        log(mRequest?.remoteAddr, "deleteLanguage:$language")
        if (language.languageId == null || language.projectId.isNullOrBlank()) {
            return CommonResponse(-1, "参数错误", null)
        }
        return try {
            var deleteTranslationSuccess = mTranslationDao.deleteTranslationByLanguageId(language.projectId!!, language.languageId!!)
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
        log(mRequest?.remoteAddr, "deleteTranslationByTranslationKey:$deleteTranslationParam")
        if (deleteTranslationParam.translationKey.isNullOrBlank() || deleteTranslationParam.projectId.isNullOrBlank()) {
            return CommonResponse(-1, "参数错误", null)
        }
        return try {
            var deleteTranslationSuccess = mTranslationDao.deleteTranslationByTranslationKey(
                deleteTranslationParam.projectId!!, deleteTranslationParam.translationKey!!
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
    @PostMapping("/addLanguages")
    fun addLanguages(@RequestBody languageList: List<Language>): CommonResponse<List<Language>> {
        log(mRequest?.remoteAddr, "addLanguages:${languageList.size}")
        val failedList = mutableListOf<Language>()
        if (languageList.isNotEmpty()) {
            languageList.forEach { language ->
                try {
                    if (language.projectId.isNullOrBlank()) {
                        println("参数错误:projectId")
                        failedList.add(language)
                    } else if (language.languageName.isNullOrBlank()) {
                        println("参数错误:languageName为空")
                        failedList.add(language)
                    } else {
                        val queryLanguageList = mTranslationDao.queryLanguageByLanguageName(language.languageName!!, language.projectId!!)
                        if (!queryLanguageList.isNullOrEmpty()) {
                            println("语言已存在")
                        } else {
                            val success = mTranslationDao.addLanguage(
                                language.languageDes!!, language.languageName!!, language.projectId!!
                            )
                            if (success) {
                                println("添加成功")
                            } else {
                                println("添加项目失败")
                                failedList.add(language)
                            }
                        }
                    }
                } catch (e: java.lang.Exception) {
                    failedList.add(language)
                }
            }
        }
        return CommonResponse(200, "", failedList)
    }

    @CrossOrigin
    @PostMapping("/addLanguage")
    fun addLanguage(@RequestBody language: Language): CommonResponse<Void> {
        log(mRequest?.remoteAddr, "addLanguage:$language")
        return try {
            if (language.projectId.isNullOrBlank()) {
                println("参数错误:projectId")
                CommonResponse(-1, "参数错误：projectId为空", null)
            } else if (language.languageName.isNullOrBlank()) {
                println("参数错误:languageName为空")
                CommonResponse(-1, "参数错误：languageName为空", null)
            } else {
                val queryLanguageList = mTranslationDao.queryLanguageByLanguageName(language.languageName!!, language.projectId!!)
                if (!queryLanguageList.isNullOrEmpty()) {
                    println("语言已存在")
                    CommonResponse(-1, "语言已存在", null)
                } else {
                    val success = mTranslationDao.addLanguage(
                        language.languageDes!!, language.languageName!!, language.projectId!!
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
        log(mRequest?.remoteAddr, "deleteProject:$project")
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

    fun deleteProjectV2(commonParam: CommonParam<*>): CommonResponse<Void> {
        return try {
            parseRealParam(commonParam, Project::class.java)?.let { project ->
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
            } ?: CommonResponse(-1, "参数解析错误", null)

        } catch (e: Exception) {
            CommonResponse(-1, e.message, null)
        }
    }

    fun addProjectV2(commonParam: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(commonParam, Project::class.java)?.let { project ->
            log(mRequest?.remoteAddr, "addProject:$project")
            return try {
                if (project.projectId.isNullOrBlank()) {
                    println("参数错误:projectId为空")
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
                            project.copyFromProject?.let { targetProjectId ->
                                println("复制目标项目：${project.copyFromProject}")
                                val oldModules = mTranslationDao.getAllModules(targetProjectId)
                                var moduleId = 0
                                oldModules.forEach { module ->
                                    module.projectId = project.projectId
                                    mTranslationDao.addModule(module.moduleName, projectId = project.projectId!!)
                                }
                                moduleId = mTranslationDao.getAllModules(project.projectId!!).first().moduleId
                                val oldLanguages = mTranslationDao.getLanguageList(targetProjectId)
                                oldLanguages.forEach { oldLanguage ->

                                    mTranslationDao.addLanguage2(oldLanguage.languageDes!!, oldLanguage.languageName!!, project.projectId!!)?.let { addLanguage ->
                                        val oldTranslations = mTranslationDao.queryTranslationByLanguage(oldLanguage.languageId ?: 0, targetProjectId)
                                        println("复制语言：${addLanguage},该语言下翻译数:${oldTranslations.size}")
                                        oldTranslations.forEach { translation ->
                                            translation.translationId = null
                                            translation.moduleId = moduleId
                                            translation.projectId = project.projectId
                                            translation.languageId = addLanguage.languageId ?: 0

                                            val addTranslation = mTranslationDao.addTranslation(translation)
                                            if (addTranslation) {
                                                println("复制翻译:$translation 成功")
                                            } else {
                                                println("复制翻译:$translation 失败")
                                            }
                                        }
                                    }


                                }

                            }

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
        } ?: CommonResponse(-1, "解析参数错误", null)
    }


    @CrossOrigin
    @RequestMapping("addProject")
    fun addProject(@RequestBody project: Project): CommonResponse<Void> {
        log(mRequest?.remoteAddr, "addProject:$project")
        return try {
            if (project.projectId.isNullOrBlank()) {
                println("参数错误:projectId为空")
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
                        project.copyFromProject?.let { targetProjectId ->
                            println("复制目标项目：${project.copyFromProject}")
                            val oldModules = mTranslationDao.getAllModules(targetProjectId)
                            var moduleId = 0
                            oldModules.forEach { module ->
                                module.projectId = project.projectId
                                mTranslationDao.addModule(module.moduleName, projectId = project.projectId!!)
                            }
                            moduleId = mTranslationDao.getAllModules(project.projectId!!).first().moduleId
                            val oldLanguages = mTranslationDao.getLanguageList(targetProjectId)
                            oldLanguages.forEach { oldLanguage ->

                                mTranslationDao.addLanguage2(oldLanguage.languageDes!!, oldLanguage.languageName!!, project.projectId!!)?.let { addLanguage ->
                                    val oldTranslations = mTranslationDao.queryTranslationByLanguage(oldLanguage.languageId ?: 0, targetProjectId)
                                    println("复制语言：${addLanguage},该语言下翻译数:${oldTranslations.size}")
                                    oldTranslations.forEach { translation ->
                                        translation.translationId = null
                                        translation.moduleId = moduleId
                                        translation.projectId = project.projectId
                                        translation.languageId = addLanguage.languageId ?: 0

                                        val addTranslation = mTranslationDao.addTranslation(translation)
                                        if (addTranslation) {
                                            println("复制翻译:$translation 成功")
                                        } else {
                                            println("复制翻译:$translation 失败")
                                        }
                                    }
                                }


                            }

                        }

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
        log(mRequest?.remoteAddr, "新增翻译:${translationList.size}")
        val failedList = mutableListOf<Translation>()

        val moduleCaches = HashMap<Int, Module>()

        return try {
            translationList.forEach { tranlation ->
                tranlation.projectId?.let { projectId ->
                    tranlation.languageId?.let { languageId ->
                        tranlation.translationKey?.let { translationKey ->
                            var add = tranlation.forceAdd
                            if (!add) {
                                val translationDB = mTranslationDao.queryTranslationByKeyInLanguage(
                                    translationKey, projectId, languageId
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
                                var module = moduleCaches.get(tranlation.moduleId)
                                if (module == null) {
                                    var moduleDB = mTranslationDao.queryModuleById(tranlation.moduleId ?: 0, projectId)
                                    if (moduleDB.isEmpty()) {
                                        module = Module()
                                        module.moduleName = ""
                                        module.projectId = projectId
                                        var addModuleResult = mTranslationDao.addModule(module.moduleName, module.projectId!!)
                                        if (!addModuleResult) {
                                            return CommonResponse(-1, "add mudle failed", emptyList())
                                        }
                                    } else {
                                        module = moduleDB[0]
                                        moduleCaches.put(tranlation.moduleId ?: 0, module)
                                    }
                                }
                                tranlation.moduleId = module.moduleId
                                val success = mTranslationDao.addTranslation(tranlation)

                                if (!success) {
                                    print(" ${tranlation.translationKey} 添加失败, content:${tranlation.translationContent}")
                                    failedList.add(tranlation)
                                }
                            } else {
                                if (!tranlation.oldTranslationContent.isNullOrEmpty()) {
                                    print(" ${tranlation.translationKey} 已存在")
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
        log(mRequest?.remoteAddr, "更新翻译:${translationList?.size}")
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
    fun getAllModules(@PathVariable("projectId") projectId: String): CommonResponse<List<Module>> {
        var moduleList = mTranslationDao.getAllModules(projectId)
        log(mRequest?.remoteAddr, "/getAllModules/$projectId -> ${moduleList.size}")
        if (moduleList.size == 0) {
            mTranslationDao.addModule("default", projectId)
        }
        moduleList = mTranslationDao.getAllModules(projectId)
        return CommonResponse(200, null, moduleList)
    }

    @CrossOrigin
    @PostMapping("/addModule")
    fun addModule(@RequestBody module: Module): CommonResponse<Void> {
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
    fun deleteModule(@RequestBody module: Module): CommonResponse<Void> {
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

    fun <PARAM> parseRealParam(param: CommonParam<*>, clazz: Class<PARAM>): PARAM? {
        return gson.fromJson(gson.toJson(param.data), clazz)
    }

}