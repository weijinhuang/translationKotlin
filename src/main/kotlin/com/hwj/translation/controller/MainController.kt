package com.hwj.translation.controller

import com.google.gson.Gson
import com.hwj.translation.baidu.MD5
import com.hwj.translation.baidu.TransApi
import com.hwj.translation.bean.*
import com.hwj.translation.bean.param.BaiduTranslationParam
import com.hwj.translation.bean.param.BaiduTranslationResult
import com.hwj.translation.bean.param.DeleteTranslationParam
import com.hwj.translation.bean.param.GetTranslationParam
import com.hwj.translation.dao.TranslationDaoImpl
import com.hwj.translation.print
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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

    @CrossOrigin
    @RequestMapping("/translateByBaidu")
    fun translateByBaidu(@RequestBody param: BaiduTranslationParam): CommonResponse<BaiduTranslationResult> {
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
        return CommonResponse(
            baiduTranslationResultResponse.code(),
            baiduTranslationResultResponse.message(),
            baiduTranslationResultResponse.body()
        )
    }

    @CrossOrigin
    @RequestMapping("/translateByBaidu2")
    fun translateByBaidu2(@RequestBody param: BaiduTranslationParam): CommonResponse<BaiduTranslationResult> {
        val api = TransApi(BAIDU_APP_ID, BAIDU_SCRECT)
        val to = param.to
        param.to = when (to) {
            "es" -> "spa"
            "fr" -> "fra"
            "ja" -> "jp"
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
            CommonResponse(
                200,
                "",
                baiduTranslationResultResponse
            )
        }
    }


    @CrossOrigin
    @RequestMapping("/sayhello")
    fun helloWorld(): ResponseEntity<ByteArray> {
        val currentDir = System.getProperty("user.dir")
        println("当前目录：$currentDir")
        val fileDir = File("$currentDir/files")
        fileDir.mkdirs()
        val translationFile = File("$currentDir/files", "translation.txt")
        val createNewFile = translationFile.createNewFile()
        translationFile.outputStream().use {
            it.write("test".toByteArray())
        }
        print("createNewFile:$createNewFile")
        val zipFilePath = "$currentDir/files" + File.separator + "strings.zip"
        val zipFile = File(zipFilePath)
        zipFile.createNewFile()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFilePath))).use { zosp ->
            FileInputStream(translationFile).use { fis ->
                BufferedInputStream(fis).use {
                    val entry = ZipEntry("translation.txt")
                    zosp.putNextEntry(entry)
                    it.copyTo(zosp, 1024)
                }
            }
        }
        val readBytes = zipFile.readBytes()
        val headers = HttpHeaders()
        headers.setContentDispositionFormData("attachment", "strings.zip")
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM

        return ResponseEntity.ok().headers(headers).contentLength(readBytes.size.toLong()).body(readBytes)
    }


    @CrossOrigin
    @RequestMapping("/exportTranslation/{projectId}/{platform}")
    fun exportTranslation(@PathVariable projectId: String, @PathVariable platform: String): ResponseEntity<ByteArray> {
        val languageList = mTranslationDao.getLanguageList(projectId)
        if (languageList.isNotEmpty()) {

            //创建zip目录
            val currentDir = System.getProperty("user.dir")
            println("当前目录：$currentDir")
            val cacheDir = File("$currentDir/cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach {
                    println("删除缓存${it.absolutePath}")
                    it.deleteOnExit()
                }
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
            val zipFile = File(cacheDir.absolutePath + File.separator + "strings.zip")
            zipFile.outputStream().use { output ->
                ZipOutputStream(BufferedOutputStream(output)).use { zipOut ->
                    addFilesToZip(cacheDir.absolutePath, "", zipOut)
                }
            }

            val readBytes = zipFile.readBytes()
            val headers = HttpHeaders()
            headers.setContentDispositionFormData("attachment", "strings.zip")
            headers.contentType = MediaType.APPLICATION_OCTET_STREAM
            return ResponseEntity.ok().headers(headers).contentLength(readBytes.size.toLong()).body(readBytes)
        }
        return ResponseEntity.ok().body(ByteArray(0))

    }

    private fun addFilesToZip(directory: String, parentDirectoryName: String, zipOut: ZipOutputStream) {
        val folder = File(directory)
        if (folder.exists()) {
            for (file in folder.listFiles()) {
                if (file.isDirectory) {
                    addFilesToZip(file.absolutePath, "$parentDirectoryName${file.name}/", zipOut)
                } else {
                    val entryName = "$parentDirectoryName${file.name}"

                    val fileInputStream = BufferedInputStream(FileInputStream(file))
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

    @CrossOrigin
    @PostMapping("/getAllTranslation")
    fun getTranslationList(@RequestBody getTranslationParam: GetTranslationParam): CommonResponse<List<Translation>> {
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
        print("deleteProject:$project")
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
        println("新增翻译:${translationList.size}")
        val failedList = mutableListOf<Translation>()

        val moduleCaches = HashMap<Int, com.hwj.translation.bean.Module>()

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
        println("更新翻译:${translationList?.size}")
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

        var moduleList = mTranslationDao.getAllModules(projectId)
        println("getAllTranslation/$projectId -> ${moduleList.size}")
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