package com.hwj.translation.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hwj.translation.baidu.TransApi
import com.hwj.translation.bean.*
import com.hwj.translation.bean.param.*
import com.hwj.translation.busniness.*
import com.hwj.translation.dao.TranslationDaoImpl
import com.hwj.translation.net.RetrofitUtil.mOkHttpClient
import com.hwj.translation.println2
import com.hwj.translation.util.*
import io.github.evanrupert.excelkt.workbook
import jakarta.servlet.http.HttpServletRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.lang.reflect.Type
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


const val proxyHost = "127.0.0.1"
const val proxyPort = "7897"

@RestController
class MainController {


    @Autowired
    private lateinit var mTranslationDao: TranslationDaoImpl

    @Autowired
    private var mRequest: HttpServletRequest? = null

    val mObjectMapper = ObjectMapper()

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
    @RequestMapping("testm3u8")
    fun testM3U8(): ResponseEntity<String> {
        return ResponseEntity.ok("http://172.16.21.156/test.m3u8")
    }


    private fun initSystemProxy() {
        System.setProperty("http.proxyHost", proxyHost)
        System.setProperty("http.proxyPort", proxyPort)
        System.setProperty("https.proxyHost", proxyHost)
        System.setProperty("https.proxyPort", proxyPort)
    }

    @CrossOrigin
    @RequestMapping("getGoogleSupportLanguage")
    fun getGoogleSupportLanguage() {
        initSystemProxy()
        println2("TranslationByGoogle")
        val translateService = TranslateOptions.getDefaultInstance().service
        val listSupportedLanguages = translateService.listSupportedLanguages()
        listSupportedLanguages.forEach {
            println2("${it.name}:${it.code}")
        }
    }

    private val mProjectRepository by lazy { ProjectRepository(mTranslationDao) }

    private val mLanguageRepository by lazy { LanguageRepository(mTranslationDao) }

    private val mTranslationRepository by lazy { TranslationRepository(mTranslationDao) }

    private val mModuleRepository by lazy { ModuleRepository(mTranslationDao) }

    private val mProjectIpRepository by lazy { ProjectIpRepository(mTranslationDao) }

    @CrossOrigin
    @RequestMapping("/translationSystem")
    fun <PARAM, RESPONSE> mainEntrance(@RequestBody param: CommonParam<PARAM>): CommonResponse<RESPONSE?> {
        println2("IP: ${mRequest?.remoteAddr} START => ${param.cmd} ${Date().toLocaleString()} ")
        val commonResponse = when (param.cmd) {
            GET_ALL_PROJECTS -> mProjectRepository.getProjectsV2()
            DELETE_PROJECT -> mProjectRepository.deleteProjectV2(param)
            ADD_PROJECT -> mProjectRepository.addProjectV2(param)

            GET_LANGUAGE_LIST -> mLanguageRepository.getLanguageListV2(param)
            DELETE_LANGUAGE -> mLanguageRepository.deleteLanguageV2(param)
            ADD_LANGUAGE -> mLanguageRepository.addLanguagesV2(param)
            UPDATE_LANGUAGE -> mLanguageRepository.updateLanguageV2(param)

            COPY_TRANSLATION -> mTranslationRepository.copyTranslation(param)
            ADD_TRANSLATION_V3 -> mTranslationRepository.addTranslationV3(param)
            BATCH_ADD_TRANSLATION -> mTranslationRepository.batchImportTranslation(param)
            CHECK_TRANSLATION_kEY -> mTranslationRepository.checkTranslationByKeyInProject(param)
            GET_ALL_TRANSLATION -> mTranslationRepository.getTranslationListV2(param)
            GET_ALL_TRANSLATION_V3 -> mTranslationRepository.getTranslationListV3(param)
            GET_TRANSLATION_ROWS_PAGINATED -> mTranslationRepository.getTranslationRowsPaginated(param)
            SEARCH_TRANSLATIONS_BY_CONTENT -> mTranslationRepository.searchTranslationsByContent(param)
            DELETE_TRANSLATION_BY_KEY -> mTranslationRepository.deleteTranslationByTranslationKeyV2(param)
            ADD_TRANSLATION -> mTranslationRepository.addTranslationsV2(param)
            UPDATE_TRANSLATION -> mTranslationRepository.updateTranslationsV2(param)
            MERGE_TRANSLATION -> mTranslationRepository.mergeTranslationV2(param)

            TRANSLATE_BY_BAIDU -> translateByBaiduV2(param)
            TRANSLATE_BY_GOOGLE -> translateByGoogleV2(param)
//            TRANSLATE_BY_GOOGLE -> translateByDeepSeek(param)
            TRANSLATE_BY_DEEPSEEK -> translateByDeepSeek(param)

            GET_ALL_MODULES -> mModuleRepository.getAllModulesV2(param)
            ADD_MODULE -> mModuleRepository.addModuleV2(param)
            DELETE_MODULE -> mModuleRepository.deleteModuleV2(param)

            UPSERT_TRANSLATION_ENGINE -> mProjectIpRepository.upsertTranslationEngine(mRequest?.remoteAddr ?: "", param)
            QUERY_TRANSLATION_ENGINE -> mProjectIpRepository.queryTranslationEngine(mRequest?.remoteAddr ?: "", param)

            UPSERT_PROJECT_IP -> mProjectIpRepository.upsertProjectIp(mRequest?.remoteAddr ?: "", param)
            DELETE_PROJECT_IP -> mProjectIpRepository.deleteProjectIpV2(param)
            QUERY_PROJECT_IPS -> mProjectIpRepository.queryProjectIpsV2(mRequest?.remoteAddr ?: "", param)

            QUERY_WORLD_LANGUAGES -> CommonResponse(200, "", CommonLanguageList())

            null -> CommonResponse(code = -1, msg = "接口名为空", null)
            else -> CommonResponse(code = 400, msg = "未知接口${param.cmd}", null)
        }
        println2("↑↑↑↑↑↑↑↑↑↑↑↑↑ ${param.cmd} ${Date()}\n\n\n")
        return commonResponse as CommonResponse<RESPONSE?>
    }


    /***************************************V2 Request**********************************************/


    fun translateByGoogleV2(param: CommonParam<*>): CommonResponse<TranslationResult?> {
        initSystemProxy()
        return param.data?.let {
            parseRealParam(param, GoogleTranslationParam::class.java)?.let { realParam ->
                val translateService = TranslateOptions.getDefaultInstance().service
                var sourceLanguage = realParam.sourceLanguage
                if (sourceLanguage.isNullOrEmpty()) {
                    val detection = translateService.detect(realParam.content)
                    sourceLanguage = detection.language
                    println2("检测到语言:$sourceLanguage")
                }
                if (realParam.targetLanguageList.isNullOrEmpty()) {
                    return try {
                        println2("开始翻译：sourceLanguage:$sourceLanguage targetLanguage:${realParam.targetLanguage} content:${realParam.content}")
                        val translateResult = translateService.translate(
                            realParam.content,
                            Translate.TranslateOption.sourceLanguage(sourceLanguage),
                            Translate.TranslateOption.targetLanguage(realParam.targetLanguage),
                            Translate.TranslateOption.format("text")
                        )
                        println2("翻译结果：${translateResult.translatedText} model:${translateResult.model}")
                        CommonResponse(200, "", TranslationResult().apply {
                            this.sourceLanguage = realParam.sourceLanguage
                            targetLanguage = realParam.targetLanguage
                            transResult = translateResult.translatedText
                            errorCode = 0
                        })
                    } catch (e: Exception) {
                        CommonResponse(-1, e.message, null);
                    }
                } else {
                    return try {
                        var translationResultList: MutableList<TranslatedResultKV> = mutableListOf()
                        realParam.targetLanguageList?.forEach { targetLanguage ->
                            println2("开始翻译：sourceLanguage:$sourceLanguage targetLanguage:${targetLanguage} content:${realParam.content}")
                            val translateResult = translateService.translate(
                                realParam.content,
                                Translate.TranslateOption.sourceLanguage(sourceLanguage),
                                Translate.TranslateOption.targetLanguage(targetLanguage),
                                Translate.TranslateOption.format("text")
                            )
                            println2("翻译结果：${translateResult.translatedText} model:${translateResult.model}")
                            val translatedResultKV = TranslatedResultKV().apply {
                                this.languageName = targetLanguage
                                this.translatedResult = translateResult.translatedText
                            }
                            translationResultList.add(translatedResultKV)
                        }
                        CommonResponse(200, "", TranslationResult().apply {
                            this.sourceLanguage = realParam.sourceLanguage
                            this.translationResultList = translationResultList
                            errorCode = 0
                        })
                    } catch (e: Exception) {
                        CommonResponse(-1, e.message, null);
                    }
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
            println2("$param->$transResult")
            val baiduTranslationResultResponse = Gson().fromJson(transResult, BaiduTranslationResult::class.java)

            return if (baiduTranslationResultResponse.error_code != null) {
                CommonResponse(
                    -1, baiduTranslationResultResponse.error_code, null
                )
            } else {
                println2("baiduTranslationResultResponse:$baiduTranslationResultResponse")
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

    fun calculateCost(
        cacheHitTokens: Int,
        cacheMissTokens: Int,
        outputTokens: Int
    ): Double {
        val cacheHitPricePerMillion = 0.2  // 元/百万tokens
        val cacheMissPricePerMillion = 2.0  // 元/百万tokens
        val outputPricePerMillion = 3.0     // 元/百万tokens

        val cacheHitCost = cacheHitTokens * cacheHitPricePerMillion / 1_000_000.0
        val cacheMissCost = cacheMissTokens * cacheMissPricePerMillion / 1_000_000.0
        val outputCost = outputTokens * outputPricePerMillion / 1_000_000.0

        return cacheHitCost + cacheMissCost + outputCost
    }

    fun translateByDeepSeek(commonParam: CommonParam<*>): CommonResponse<TranslationResult> {
        return parseRealParam(commonParam, DeepSeekTranslationParam::class.java)?.let { param ->

            val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: System.getProperty("DEEPSEEK_API_KEY") ?: "sk-abfa10f449d0404ebdce2568753cd234"
            if (apiKey.isNullOrBlank()) {
                return CommonResponse(-1, "DeepSeek API key未配置", null)
            }

            val url = "https://api.deepseek.com/chat/completions"

            val client = mOkHttpClient

            val targetLanguageList = param.targetLanguageList?.joinToString(separator = ",")

            if (targetLanguageList.isNullOrEmpty()) {
                return CommonResponse(-1, "没有目标语言", null)
            }
            val systemContent = "你是个翻译专家，执行以下步骤：\n1. 识别用户消息中的【待翻译文本】\n2. 按指定格式返回翻译结果\n3.翻译尽可能的简短，因为文案会使用在手机app上"
            val roleContent = """
                            |参数：
                            |- 原文语言：${param.sourceLanguage}
                            |- 目标语言列表：[${targetLanguageList}]
                            |- 文本使用场景：智能家居App ${param.translateContext ?: ""}
                            |
                            |请务必输出一个有效的JSON对象，不要包含任何其他解释文字。
                            |期望的JSON输出格式示例：
                            |[
                            |  {"languageName": "en", "translatedResult": "Hello"},
                            |  {"languageName": "ja", "translatedResult": "こんにちは"}
                            | ]
                            |
                            |待翻译文本：${param.content}
                            |""".trimMargin()
            println2("DeepSeek翻译，content：${param.content} ${param.sourceLanguage} -> $targetLanguageList roleContent:$roleContent")
            val messages = listOf(
                mapOf("role" to "system", "content" to systemContent), mapOf(
                    "role" to "user", "content" to roleContent
                )
            )

            val requestBodyMap = mapOf(
                "model" to "deepseek-chat",
                "messages" to messages,
                "stream" to false,
                "response_format" to mapOf("type" to "json_object"), //
                "max_tokens" to 1000, // 防止长JSON被截断
            )

            val jsonBody = Gson().toJson(requestBodyMap)

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = okhttp3.RequestBody.create(mediaType, jsonBody)

            val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json").post(requestBody).build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBodyStr = response.body?.string()
                    val deepSeekTranslationResult = Gson().fromJson(responseBodyStr, DeepSeekTranslationResult::class.java)
                    val content = deepSeekTranslationResult.choices.first().message.content
                    val translatedResultKVListType = object : TypeToken<List<TranslatedResultKV?>?>() {}.type
                    val translatedResultKVList: List<TranslatedResultKV> = try {
                        Gson().fromJson(content, translatedResultKVListType)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    if (translatedResultKVList.isEmpty()) {
                        CommonResponse(-1, "翻译出现问题", null)
                    } else {
                        val result = TranslationResult().apply {
                            sourceLanguage = param.sourceLanguage
                            targetLanguage = ""
                            this.translationResultList = translatedResultKVList
                            errorCode = 0
                        }
                        // 你的数据
                        val cacheHit = deepSeekTranslationResult.usage.prompt_tokens_details?.cached_tokens ?: 0
                        val cacheMiss = deepSeekTranslationResult.usage.prompt_cache_miss_tokens ?: 0
                        val output = deepSeekTranslationResult.usage.completion_tokens ?: 0
                        val totalCost = calculateCost(cacheHit, cacheMiss, output)
                        CommonResponse(200, "本次消耗token：${deepSeekTranslationResult.usage.total_tokens},花费：￥$totalCost", result)
                    }
                } else {
                    val errorBody = response.body?.string()
                    println2("DeepSeek API Error: ${response.code} - $errorBody")
                    CommonResponse(-1, "DeepSeek API Error: ${response.code}", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CommonResponse(-1, e.message, null)
            }
        } ?: CommonResponse(-1, "参数解析错误", null)
    }


    val moduleCaches = HashMap<String, Module>()

    fun getModule(translation: Translation, projectId: String): Module? {
        var module = moduleCaches[translation.projectId]
        if (module == null) {
            var moduleDB = mTranslationDao.queryModuleById(projectId)
            if (moduleDB.isEmpty()) {
                module = mTranslationDao.addModule("", projectId)
            } else {
                module = moduleDB[0]
            }
            if (null != module) {
                moduleCaches.put(translation.projectId ?: "", module)
            }

        }
        return module
    }

    /***************************************V2 Request**********************************************/

    @CrossOrigin
    @RequestMapping("/translateByGoogle")
    fun translateByGoogle(@RequestBody param: GoogleTranslationParam): CommonResponse<TranslationResult> {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7890");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7890");
        println2("TranslationByGoogle")
        val translateService = TranslateOptions.getDefaultInstance().service
        var sourceLanguage = param.sourceLanguage
        if (sourceLanguage.isNullOrEmpty()) {
            val detection = translateService.detect(param.content)
            sourceLanguage = detection.language
            println2("检测到语言:$sourceLanguage")
        }
        try {
            val translateResult = translateService.translate(param.content, Translate.TranslateOption.sourceLanguage(sourceLanguage), Translate.TranslateOption.targetLanguage(param.targetLanguage))
            println2("翻译结果：${translateResult.translatedText} model:${translateResult.model}")
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

    fun exportExcelOfKey(param: ExportTranslationParam): ResponseEntity<ByteArray> {
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
            println2("当前目录：$currentDir")
            val cacheDir = File("$currentDir/cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach {
                    deleteCache(it)
                }
            } else {
                cacheDir.mkdirs()
            }


            val keyLanguageContentMap: HashMap<String, HashMap<Int, String>> = HashMap()

            var sortTranslationList: MutableList<Translation>? = null

            //分语言导出
            for (language in mainLanguageList) {
                val mainTranslationList: List<Translation> = mTranslationDao.queryTranslationByLanguage(language.languageId ?: 0, language.projectId ?: "")
                if (sortTranslationList == null) {
                    sortTranslationList = mainTranslationList.toMutableList()
                }
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

            sortTranslationList?.sortByDescending {
                it.translationId
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
                    sortTranslationList?.let { sortTranslationList ->
                        sortTranslationList.forEach { sortTranslation ->
                            sortTranslation.translationKey?.let { translationKey ->
                                row {
                                    cell(translationKey)
                                    mainLanguageList.forEach { language ->
                                        cell(keyLanguageContentMap[translationKey]?.get(language.languageId ?: 0) ?: "")
                                    }
                                }
                            }

                        }

                    }
//                    keyLanguageContentMap.keys.forEach { translationKey ->
//                        row {
//                            cell(translationKey)
//                            mainLanguageList.forEach { language ->
//                                cell(keyLanguageContentMap[translationKey]?.get(language.languageId ?: 0) ?: "")
//                            }
//                        }
//                    }
                }
            }.write("$fileDir/$mainProjectId.xlsx")

            println2("合并excel完毕：$fileDir/$mainProjectId.xlsx")
            val readBytes = File("$fileDir/$mainProjectId.xlsx").readBytes()
            val headers = HttpHeaders()
            headers.setContentDispositionFormData("attachment", "$mainProjectId.xlsx")
            headers.contentType = MediaType.APPLICATION_OCTET_STREAM
            return ResponseEntity.ok().headers(headers).contentLength(readBytes.size.toLong()).body(readBytes)
        }
        return ResponseEntity.ok().body(ByteArray(0))
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
            println2("当前目录：$currentDir")
            val cacheDir = File("$currentDir/cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach {
                    deleteCache(it)
                }
            } else {
                cacheDir.mkdirs()
            }


            val keyLanguageContentMap: HashMap<String, HashMap<Int, String>> = HashMap()

            var sortTranslationList: MutableList<Translation>? = null

            //分语言导出
            for (language in mainLanguageList) {
                val mainTranslationList: List<Translation> = mTranslationDao.queryTranslationByLanguage(language.languageId ?: 0, language.projectId ?: "")
                if (sortTranslationList == null) {
                    sortTranslationList = mainTranslationList.toMutableList()
                }
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

            sortTranslationList?.sortByDescending {
                it.translationId
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
                    sortTranslationList?.let { sortTranslationList ->
                        sortTranslationList.forEach { sortTranslation ->
                            sortTranslation.translationKey?.let { translationKey ->
                                row {
                                    cell(translationKey)
                                    mainLanguageList.forEach { language ->
                                        cell(keyLanguageContentMap[translationKey]?.get(language.languageId ?: 0) ?: "")
                                    }
                                }
                            }

                        }

                    }
//                    keyLanguageContentMap.keys.forEach { translationKey ->
//                        row {
//                            cell(translationKey)
//                            mainLanguageList.forEach { language ->
//                                cell(keyLanguageContentMap[translationKey]?.get(language.languageId ?: 0) ?: "")
//                            }
//                        }
//                    }
                }
            }.write("$fileDir/$mainProjectId.xlsx")

            println2("合并excel完毕：$fileDir/$mainProjectId.xlsx")
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

    val mStringBuilder = java.lang.StringBuilder()
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
            println2("当前目录：$currentDir")
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
                val hideTranslationList = mutableListOf<Translation>()
                val mainTranslationList: List<Translation> = mTranslationDao.queryTranslationByLanguage(language.languageId ?: 0, language.projectId ?: "")
                mainTranslationList.forEach {
                    if (it.hide == 1) {
                        hideTranslationList.add(it)
                    } else {
                        translationInLanguage.add(it)
                    }
                }

                subLanguageList.forEach { subLanguage ->
                    if (subLanguage.languageName == language.languageName) {
                        val subTranslationList = mTranslationDao.queryTranslationByLanguage(subLanguage.languageId ?: 0, subLanguage.projectId ?: "")
                        subTranslationList.forEach {
                            if (it.hide == 1) {
                                hideTranslationList.add(it)
                            } else {
                                translationInLanguage.add(it)
                            }
                        }
                    }
                }

                println2("查询到翻译数量：${translationInLanguage.size}")

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
                    println2("创建目录$languageDir $success")
                }
                languageDirList.add(languageDir)

                val file = File(languageDir, "Localizable.strings")
                file.createNewFile()
                println2("创建${file.absolutePath}")
                FileOutputStream(file).use { fos ->
                    translationInLanguage.let { translationList ->
                        translationList.forEach { translation ->
                            translation.translationKey?.let { translationKey ->
                                translation.translationContent?.let { translationContent ->
//                                    if (translationContent.contains("|")) {
//                                        val contentArray = translationContent.split("|")
//                                        var i = 0
//                                        contentArray.forEach { contentItem ->
//                                            fos.write("\"$translationKey\"${i++}=\"$contentItem\"\n;".toByteArray())
//                                        }
//
//                                    } else {
                                    val charArray = translationContent.toCharArray()
                                    mStringBuilder.clear()

                                    for (i in charArray.indices) {
                                        var c = charArray[i]
                                        if (c == '"') {
                                            if (i == 0) {
                                                mStringBuilder.append('\\')
                                                mStringBuilder.append(c)
                                            } else {
                                                val preChar = charArray[i - 1]
                                                if (preChar == '\\') {
                                                    mStringBuilder.append(c)
                                                } else {
                                                    mStringBuilder.append('\\')
                                                    mStringBuilder.append(c)
                                                }
                                            }
                                        } else {
                                            mStringBuilder.append(c)
                                        }
                                    }
                                    fos.write("\"$translationKey\"=\"$mStringBuilder\";".toByteArray())
                                    if (!translation.comment.isNullOrEmpty()) {
                                        fos.write("//${translation.translationKey}=${translation.comment}\n".toByteArray())
                                    } else {
                                        fos.write("\n".toByteArray())
                                    }
//                                    }
                                }
                            }
                        }
                    }
                    println2("已合并翻译：${hideTranslationList.size}")
                    hideTranslationList.forEach { hideTranslation ->
                        val mergeTranslation = "//【${hideTranslation.referto}】 ${hideTranslation.translationKey} = ${hideTranslation.translationContent}\n"
                        fos.write(mergeTranslation.toByteArray())
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

            println2("压缩完毕：${zipFile.absolutePath}")
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
            println2("当前目录：$currentDir")
            val cacheDir = File("$currentDir/cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach {
                    deleteCache(it)
                }
            } else {
                cacheDir.mkdirs()
            }

            val languageDirList: MutableList<File> = mutableListOf()

            val addTranslationSB = java.lang.StringBuilder()
            //分语言导出
            for (language in manLanguageList) {
                val translationInLanguage = mutableListOf<Translation>()
                val hideTranslationList = mutableListOf<Translation>()
                val mainProjectTranslationList: List<Translation> = mTranslationDao.queryTranslationByLanguage(language.languageId ?: 0, language.projectId ?: "")
                mainProjectTranslationList.forEach {
                    if (it.hide == 1) {
                        hideTranslationList.add(it)
                    } else {
                        translationInLanguage.add(it)
                    }
                }
                subLanguageList.forEach { subLanguage ->
                    if (subLanguage.languageName == language.languageName) {
                        val subTranslationList = mTranslationDao.queryTranslationByLanguage(subLanguage.languageId ?: 0, subLanguage.projectId ?: "")
                        subTranslationList.forEach {
                            if (it.hide == 1) {
                                hideTranslationList.add(it)
                            } else {
                                translationInLanguage.add(it)
                            }
                        }
                    }
                }
                println2("查询到翻译数量：${translationInLanguage.size}")
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
                    println2("创建目录$languageDir $success")
                }
                languageDirList.add(languageDir)

                //创建xml
                val xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
                val resources = xmlDoc.createElement("resources")
                translationInLanguage.forEach { translation ->
                    translation.translationKey?.let { translationKey ->
                        translation.translationContent?.let { translationContent ->
                            if (translationContent.contains("|")) {
                                val `string-array` = xmlDoc.createElement("string-array")
                                `string-array`.setAttribute("name", translationKey)
                                val contentArray = translationContent.split("|")
                                contentArray.forEach { contentItem ->
                                    val item = xmlDoc.createElement("item")
                                    item.textContent = handleSingleQuotes(addTranslationSB, contentItem)
                                    `string-array`.appendChild(item)
                                }
                                resources.appendChild(`string-array`)
                            } else {
                                val stringElement = xmlDoc.createElement("string")
                                stringElement.setAttribute("name", translationKey)
                                stringElement.textContent = handleSingleQuotes(addTranslationSB, translationContent)

                                resources.appendChild(stringElement)

                            }
                            if (!translation.comment.isNullOrEmpty()) {
                                val createComment = xmlDoc.createComment("${translation.translationKey}=${translation.comment}")
                                resources.appendChild(createComment)
                            }
                        }
                    }
                }
                hideTranslationList.forEach { hideTranslation ->
                    val createComment = xmlDoc.createComment("【${hideTranslation.referto}】 ${hideTranslation.translationKey} = ${hideTranslation.translationContent}")
                    resources.appendChild(createComment)
                }
                xmlDoc.appendChild(resources)
                val xmlFile = File(languageDir, "strings.xml")
                println2("创建${xmlFile.absolutePath}")
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

            println2("压缩完毕：${zipFile.absolutePath}")
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
            println2("删除：${cacheDirFile.absolutePath}")
            cacheDirFile.delete()
        } else {
            println2("遍历目录：${cacheDirFile.absolutePath}")
            val listFiles = cacheDirFile.listFiles()
            listFiles?.forEach {
                if (it.isFile) {
                    println2("删除：${it.absolutePath}")
                    it.delete()
                } else {
                    deleteCache(it)
                }
            }
            println2("删除目录：${cacheDirFile.absolutePath}")
            cacheDirFile.delete()
        }
    }

    private fun addFilesToZip(directory: String, parentDirectoryName: String, zipOut: ZipOutputStream) {
        println2(" addFilesToZip($directory: String, $parentDirectoryName: String, zipOut: ZipOutputStream)")
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
    @RequestMapping("/sayhello")
    fun helloWorld() {
        log(mRequest?.remoteAddr, "sayHello")
        val currentDir = System.getProperty("user.dir")
        println2("当前目录：$currentDir")
        val fileDir = File("$currentDir/files")
        deleteCache(fileDir)

        deleteCache(File("$currentDir/cache"))

    }

    @CrossOrigin
    @PostMapping("/upload-translations")
    fun uploadTranslations(
        @RequestParam("file") file: MultipartFile
    ): CommonResponse<out ParseTranslationResult> {
        val startTime = System.currentTimeMillis()
        val filename = file.originalFilename ?: "unknown"
        val response = if (file.isEmpty) {
            CommonResponse(-1, "文件为空", null)
        } else {
            try {
                println2("接收到文件：$filename")
                getParserForFile(filename).let { parser ->
                    val content = if (parser is ExcelParser) "" else String(file.bytes, Charsets.UTF_8)
                    val translations = parser.parse(content, file.inputStream)
                    CommonResponse(200, "", translations)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CommonResponse(-1, e.message, null)
            }
        }
        println2("解析$filename 翻译数量：${response.data?.translationRowList?.size} 花费时间：ms${System.currentTimeMillis() - startTime}")
        return response
    }


    fun <PARAM> parseRealParam(param: CommonParam<*>, clazz: Class<PARAM>): PARAM? {
        return mObjectMapper.convertValue(param.data, clazz)
    }

    fun <PARAM> parseRealListPram(param: CommonParam<*>, clazz: Class<PARAM>): List<PARAM>? {
        val type = object : TypeReference<List<PARAM>>() {
            override fun getType(): Type {
                val typeToken = TypeToken.getParameterized(List::class.java, clazz)
                return typeToken.type
            }
        }

        val listResult = mObjectMapper.convertValue(param.data, type)
        return listResult
    }
}