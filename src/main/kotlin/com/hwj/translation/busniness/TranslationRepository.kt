package com.hwj.translation.busniness

import com.hwj.translation.bean.CommonResponse
import com.hwj.translation.bean.Module
import com.hwj.translation.bean.Translation
import com.hwj.translation.bean.param.*
import com.hwj.translation.dao.TranslationDao
import com.hwj.translation.util.handleSingleQuotes

class TranslationRepository(translationDao: TranslationDao) : BaseRepository(translationDao) {


    fun checkTranslationByKeyInProject(param: CommonParam<*>): CommonResponse<Int> {
        return parseRealParam(param, CheckTranslationCountOfKeyInProjectParam::class.java)?.let { realParam ->
            val count = mTranslationDao.getTranslationCountOfKeyInProject(realParam.projectId ?: "", realParam.translationKey ?: "")
            CommonResponse(200, "", count)
        } ?: CommonResponse(-1, "", null)
    }

    fun getTranslationListV2(param: CommonParam<*>): CommonResponse<List<Translation>> {
        return parseRealParam(param, GetTranslationParam::class.java)?.let { realParam ->
            val translationList = if (null == realParam.moduleId) {
                mTranslationDao.getAllTranslationByProjectId(realParam.projectId)
            } else {
                mTranslationDao.queryTranslationByModule(realParam.moduleId!!, realParam.projectId)
            }
            println("查詢到翻譯：${translationList.size}")
            CommonResponse(200, "", translationList.filter { it.hide == 0 })
        } ?: CommonResponse(-1, "参数错误", emptyList())
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


    val addTranslationSB = java.lang.StringBuilder()
    fun addTranslationsV2(commonParam: CommonParam<*>): CommonResponse<List<Translation>> {

        return parseRealListPram(commonParam, Translation::class.java)?.let { translationList ->
            val failedList = mutableListOf<Translation>()
            return try {
                translationList.forEach { translation ->
                    translation.projectId?.let { projectId ->
                        translation.languageId?.let { languageId ->
                            translation.translationKey?.let { translationKey ->
                                translation.translationContent?.let { translationContent ->
                                    translation.translationContent = handleSingleQuotes(addTranslationSB, translationContent)
                                    val module = getModule(translation, projectId) ?: return CommonResponse(-1, "添加模块失败", null)
                                    if (translation.translationId == null || translation.translationId == -1) {//新增翻译
                                        translation.moduleId = module.moduleId
                                        //先查询是否有冲突的key
                                        val translationDB = mTranslationDao.queryTranslationByKeyInLanguage(translationKey, projectId, languageId)
                                        if (translationDB.isNotEmpty()) {
                                            if (translation.forceAdd) {
                                                print(" ${translation.translationKey} 已存在 更新内容")
                                                translation.translationId = translationDB[0].translationId
                                                var newTranslationSuccess = mTranslationDao.updateTranslation(translation)
                                                if (!newTranslationSuccess) {
                                                    print("更新失败 $translation")
                                                }
                                            } else {
                                                if (translation.translationContent != translationDB[0].translationContent) {
                                                    translation.oldTranslationContent = translationDB[0].translationContent
                                                    failedList.add(translation)
                                                }
                                            }
                                        } else {//本地没有冲突直接添加
                                            val resultTranslation = mTranslationDao.addTranslation(translation)
                                            if (resultTranslation != null) {
                                                println(" ${translation.translationKey} 添加成功, content:${translation.translationContent}")
                                            } else {
                                                print(" ${translation.translationKey} 添加失败, content:${translation.translationContent}")
                                            }
                                        }


                                    } else {//translation Id不为空 更新翻译
                                        val translationDB = mTranslationDao.queryTranslationByKeyInLanguage(translationKey, projectId, languageId)
                                        //先查询一边本地是否有key冲突（防止改key冲突）
                                        if (translationDB.isNotEmpty()) {
                                            //有冲突
                                            val localTranslation = translationDB[0]
                                            if (translation.translationId == localTranslation.translationId) {//冲突的翻译是同一个，更新即可
                                                var updateSuccess = mTranslationDao.updateTranslation(translation)
                                                print("更新 $updateSuccess")
                                            } else {////冲突的翻译不是同一个
                                                print(" ${translation.translationKey} 已存在")
                                                return CommonResponse(-1, "Key冲突", emptyList())
                                            }
                                        } else {//key没有冲突，直接更新
                                            translation.moduleId = module.moduleId
                                            val success = mTranslationDao.updateTranslation(translation)
                                            if (success) {
                                                print(" ${translation.translationKey} 添加失败, content:${translation.translationContent}")
                                            } else {
                                                print(" ${translation.translationKey} 添加成功, content:${translation.translationContent}")
                                            }
                                        }
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

    fun updateTranslationsV2(commonParam: CommonParam<*>): CommonResponse<List<Translation>> {

        return parseRealListPram(commonParam, Translation::class.java)?.let { translationList ->
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

    val moduleCaches = HashMap<String, Module>()//暂时没有用，为以后增加模块扩展预留

    fun getModule(translation: Translation, projectId: String): Module? {
        var module = moduleCaches[projectId]
        if (module == null) {
            println("未找到内存缓存，查找数据库")
            var moduleDB = mTranslationDao.queryModuleById(projectId)
            if (moduleDB.isEmpty()) {
                println("未找到数据库缓存，创建新module")
                module = Module()
                module.moduleName = ""
                module.projectId = projectId
                var addModuleResult = mTranslationDao.addModule(module.moduleName, module.projectId!!)

                if (addModuleResult != null) {
                    println("已创建module：${module.moduleId}")
                    module = addModuleResult
                    moduleCaches[projectId] = module

                }
            } else {
                module = moduleDB[0]
                println("数据库缓存module：${module.moduleId}")
                moduleCaches[projectId] = module
            }
        } else {
            println("内存缓存module：${module.moduleId}")
        }
        return module
    }

    fun mergeTranslationV2(commonParam: CommonParam<*>): CommonResponse<Void> {
        return parseRealParam(commonParam, MergeTranslationParam::class.java)?.let { realParam ->
            realParam.projectId?.let { projectId ->
                realParam.mainTranslationKey?.let { mainTranslationKey ->
                    val mainTranslationList = mTranslationDao.queryTranslationByKey(mainTranslationKey, projectId)
                    if (mainTranslationList.isNotEmpty()) {
                        realParam.translationToBeHideKeyList?.let { deleteTranslationKeyList ->
                            var commentBuilder = java.lang.StringBuilder()
                            deleteTranslationKeyList.forEach { deleteTranslationKey ->
                                val translationToBeHideList = mTranslationDao.queryTranslationByKey(deleteTranslationKey, projectId)
                                if (translationToBeHideList.isNotEmpty()) {
                                    translationToBeHideList.forEach { translationToBeHide ->
                                        translationToBeHide.hide = 1
                                        translationToBeHide.referto = mainTranslationKey
                                        val updateTranslationResult = mTranslationDao.updateTranslation(translationToBeHide)
                                        println("${translationToBeHide.translationKey} updateTranslationResult:$updateTranslationResult")
                                    }
                                }
                                val deleteSuccess = mTranslationDao.deleteTranslationByKey(deleteTranslationKey, projectId)
                                if (deleteSuccess) {
                                    commentBuilder.append(deleteTranslationKey).append(",")
                                }
                            }
                            mainTranslationList.forEach { mainTranslation ->
                                mainTranslation.comment = commentBuilder.toString()
                                mTranslationDao.updateTranslation(mainTranslation)
                            }
                            CommonResponse(200, "", null)
                        } ?: CommonResponse(-1, "没有要删除的翻译", null)
                    } else {
                        CommonResponse(-1, "未查询到主翻译", null)
                    }
                } ?: CommonResponse(-1, "没有主翻译", null)
            } ?: CommonResponse(-1, "项目id为空", null)
        } ?: CommonResponse(-1, "参数解析出错", null)
    }
}