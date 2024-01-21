package com.hwj.translation.controller

import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.gson.Gson
import com.hwj.translation.baidu.MD5
import com.hwj.translation.baidu.TransApi
import com.hwj.translation.bean.*
import com.hwj.translation.bean.param.*
import com.hwj.translation.dao.TranslationDaoImpl
import com.hwj.translation.print
import com.hwj.translation.util.log
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


@RestController
class MainController {


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
    @RequestMapping("/translateByBaidu")
    fun translateByBaidu(@RequestBody param: BaiduTranslationParam): CommonResponse<TranslationResult> {
        val salt = "123456789"
        // 签名
        val src: String = BAIDU_APP_ID + param.q + salt + BAIDU_SCRECT
        val md5Str = MD5.md5(src)
        println("$src->$md5Str")
        param.sign = md5Str
        param.salt = salt
        param.appid = BAIDU_APP_ID

        param.encode()

        println("translateByBaidu:${param.toString()}")

        val baiduTranslationResultResponse =
            RetrofitUtil.mTranslationApi.translateByBaidu(
                param.q!!,
                param.salt!!,
                param.appid!!,
                param.sign!!,
                param.from!!,
                param.to!!
            ).execute()



        println("baiduTranslationResultResponse:${baiduTranslationResultResponse.body().toString()}")
        val result = TranslationResult().apply {
            sourceLanguage = param.from
            targetLanguage = param.to
            transResult = baiduTranslationResultResponse.body()?.trans_result?.get(0)?.dst ?: ""
            errorCode = baiduTranslationResultResponse.code()
        }
        return CommonResponse(
            baiduTranslationResultResponse.code(),
            baiduTranslationResultResponse.message(),
            result
        )
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
        val baiduTranslationResultResponse =
            Gson().fromJson(transResult, BaiduTranslationResult::class.java)

        return if (baiduTranslationResultResponse.error_code != null) {
            CommonResponse(
                -1,
                baiduTranslationResultResponse.error_code,
                null
            )
        } else {
            println("baiduTranslationResultResponse:${baiduTranslationResultResponse.toString()}")
            val result = TranslationResult().apply {
                sourceLanguage = param.from
                targetLanguage = param.to
                this.transResult = baiduTranslationResultResponse.trans_result?.get(0)?.dst ?: ""
                errorCode = 0
            }
            CommonResponse(
                200,
                "",
                result
            )


        }
    }


    @CrossOrigin
    @RequestMapping("/sayhello")
    fun helloWorld() {
        log(mRequest?.remoteAddr, "sayHello")
        val currentDir = System.getProperty("user.dir")
        println("当前目录：$currentDir")
        val fileDir = File("$currentDir/files")
        deleteCache(fileDir)

        deleteCache(File("$currentDir/cache"))

    }


    @CrossOrigin
    @RequestMapping("/exportTranslation/{projectId}/{platform}")
    fun exportTranslation(@PathVariable projectId: String, @PathVariable platform: String): ResponseEntity<ByteArray> {
        log(mRequest?.remoteAddr, "/exportTranslation/$projectId/$platform")
        if (platform == "android") {
            return exportAndroid(projectId)
        } else {
            return exportIOS(projectId)
        }

    }

    fun exportIOS(projectId: String): ResponseEntity<ByteArray> {
        log(mRequest?.remoteAddr, "exportIOS")
        val languageList = mTranslationDao.getLanguageList(projectId)
        if (languageList.isNotEmpty()) {

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
            for (language in languageList) {
                language.languageId?.let { languageId ->
                    val translationInLanguage: List<Translation?> = mTranslationDao.queryTranslationByLanguage(languageId, projectId)
                    println("查询到翻译数量：${translationInLanguage.size}")
                    translationInLanguage.let { translationList ->
                        //创建目录
                        val dirName = when (language.languageName) {
                            "spa" -> "es.lproj"
                            "fra" -> "fr.lproj"
                            "jp" -> "ja.lproj"
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
                                        if (translationContent.contains("|")) {
                                            val contentArray = translationContent.split("|")
                                            var i = 0
                                            contentArray.forEach { contentItem ->
                                                fos.write("\"$translationKey\"${i++}=\"$contentItem\"\n".toByteArray())
                                            }

                                        } else {
                                            fos.write("\"$translationKey\"=\"$translationContent\"\n".toByteArray())
                                        }
                                    }
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

    fun exportAndroid(projectId: String): ResponseEntity<ByteArray> {
        log(mRequest?.remoteAddr, "exportAndroid")
        val languageList = mTranslationDao.getLanguageList(projectId)
        if (languageList.isNotEmpty()) {

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
            for (language in languageList) {
                language.languageId?.let { languageId ->
                    val translationInLanguage: List<Translation?> = mTranslationDao.queryTranslationByLanguage(languageId, projectId)
                    println("查询到翻译数量：${translationInLanguage.size}")
                    translationInLanguage.let { translationList ->
                        //创建目录
                        val dirName = when (language.languageName) {
                            "en" -> "values"
                            "spa" -> "values-es"
                            "fra" -> "values-fr"
                            "jp" -> "values-ja"
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
        log(mRequest?.remoteAddr, "deleteTranslationByTranslationKey:$deleteTranslationParam")
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
                        val queryLanguageList =
                            mTranslationDao.queryLanguageByLanguageName(language.languageName!!, language.projectId!!)
                        if (!queryLanguageList.isNullOrEmpty()) {
                            println("语言已存在")
                        } else {
                            val success = mTranslationDao.addLanguage(
                                language.languageDes!!,
                                language.languageName!!,
                                language.projectId!!
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

    @CrossOrigin
    @RequestMapping("addProject")
    fun addProject(
        @RequestBody project: Project
    ): CommonResponse<Void> {
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

                                mTranslationDao.addLanguage2(oldLanguage.languageDes!!,oldLanguage.languageName!!, project.projectId!!)?.let { addLanguage ->
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
                                var module = moduleCaches.get(tranlation.moduleId)
                                if (module == null) {
                                    var moduleDB = mTranslationDao.queryModuleById(tranlation.moduleId ?: 0, projectId)
                                    if (moduleDB.isEmpty()) {
                                        module = Module()
                                        module.moduleName = ""
                                        module.projectId = projectId
                                        var addModuleResult =
                                            mTranslationDao.addModule(module.moduleName, module.projectId!!)
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


}